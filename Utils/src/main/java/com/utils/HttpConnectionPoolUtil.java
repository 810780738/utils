package com.utils;


import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.Synchronized;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description: httpclient连接池
 *
 * @Create: 2018/12/25 10:07
 **/

public class HttpConnectionPoolUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpConnectionPoolUtil.class);

    private static int CONNECT_TIMEOUT = HttpConnectionConfig.HTTP_CONNECTION_TIMEOUT;// 设置连接建立的超时时间为10s
    private static int SOCKET_TIME_OUT = HttpConnectionConfig.HTTP_SOCKET_TIMEOUT;
    private static int MAX_CONNECTION = HttpConnectionConfig.HTTP_MAX_POOL_SIZE;
    private static int MAX_ROUTE = HttpConnectionConfig.HTTP_MAX_ROUTE;
    private static int MAX_PER_ROUTE = HttpConnectionConfig.HTTP_MAX_PER_ROUTE;
    private static CloseableHttpClient httpClient;//请求客户端（单例）
    private static PoolingHttpClientConnectionManager manager;//连接池管理
    private static ScheduledExecutorService executorService;//定时任务线程池


    private static  String url = "http://218.2.78.174:9001/srvc/services/risreport?wsdl";

    private static void setRequestConfig(HttpRequestBase httpRequestBase){
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIME_OUT)
                .build();
        httpRequestBase.setConfig(requestConfig);
    }

    /**
     * @Author zhusm
     * @Description 创建httpclient实例
     * @Date 10:59 2018/12/25
     * @Param [url]
     * @return org.apache.http.impl.client.CloseableHttpClient
     **/
    @Synchronized //多线程下调用防止重复创建httpclient
    public static CloseableHttpClient getHttpClient(){
        if (httpClient == null){
            if (httpClient == null){
                httpClient = createHttpClient();
                executorService = Executors.newScheduledThreadPool(1);
                executorService.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        manager.closeExpiredConnections();
                        manager.closeIdleConnections(HttpConnectionConfig.HTTP_IDEL_TIMEOUT, TimeUnit.MICROSECONDS);
                    }
                }, HttpConnectionConfig.HTTP_MONITOR_INTERVAL, HttpConnectionConfig.HTTP_MONITOR_INTERVAL,TimeUnit.MILLISECONDS);
            }
        }
        return httpClient;
    }

    private static CloseableHttpClient createHttpClient() {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
                .getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory
                .getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("http", plainsf)
                .register("https", sslsf).build();
        manager = new PoolingHttpClientConnectionManager(registry);
        manager.setMaxTotal(MAX_CONNECTION);
        manager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
//        String urlend = url.split(":")[2];
//        String[] split = urlend.split("/");
//        Integer port = Integer.valueOf(split[0]);
//        String urls = url.split(":")[1];
        HttpHost httpHost = new HttpHost("218.2.78.174", 9001);
        manager.setMaxPerRoute(new HttpRoute(httpHost),MAX_ROUTE);

        //失败重试
        HttpRequestRetryHandler handler = new HttpRequestRetryHandler() {

            @Override
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                if (i > 3) {
                    //重试超过3次,放弃请求
                    logger.error("retry has more than 3 time, give up request");
                    return false;
                }
                if (e instanceof NoHttpResponseException) {
                    //服务器没有响应,可能是服务器断开了连接,应该重试
                    logger.error("receive no response from server, retry");
                    return true;
                }
                if (e instanceof SSLHandshakeException) {
                    // SSL握手异常
                    logger.error("SSL hand shake exception");
                    return false;
                }
                if (e instanceof InterruptedIOException) {
                    //超时
                    logger.error("InterruptedIOException");
                    return false;
                }
                if (e instanceof UnknownHostException) {
                    // 服务器不可达
                    logger.error("server host unknown");
                    return false;
                }
                if (e instanceof ConnectTimeoutException) {
                    // 连接超时
                    logger.error("Connection Time out");
                    return false;
                }
                if (e instanceof SSLException) {
                    logger.error("SSLException");
                    return false;
                }

                HttpClientContext context = HttpClientContext.adapt(httpContext);
                HttpRequest request = context.getRequest();
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    //如果请求是幂等请求，则再次尝试
                    return true;
                }
                return false;
            }
        };
        return HttpClients
                .custom()
                .setConnectionManager(manager)
                .setRetryHandler(handler)
                .build();
    }

    /**
     * @Author zhusm
     * @Description Post参数设置
     * @Date 11:04 2018/12/25
     * @Param [httpPost, params]
     * @return void
     **/
    private static void setPostParams(HttpPost httpPost, Map<String,String> params){
        List<NameValuePair> pairs = new ArrayList<>();
        Set<String> keys = params.keySet();
        for (String key : keys) {
            pairs.add(new BasicNameValuePair(key,params.get(key)));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(pairs,"utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Author zhusm
     * @Description post请求返回对象
     * @Date 13:14 2018/12/25
     * @Param [url, params]
     * @return T
     **/
    public static <T> T doPost(Map<String,String> params,Class<T> clazz){
        HttpPost post = new HttpPost(url);
        setRequestConfig(post);
        post.addHeader("Content-Type", "application/json;charset=utf-8");
        setPostParams(post,params);
        CloseableHttpResponse response = null;
        InputStream is = null;
        T res = null;
        try {
            response = getHttpClient().execute(post,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                String result = IOUtils.toString(is, "utf-8");
                res = JSON.parseObject(result, clazz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    /**
     * @Author: zhusm@bsoft.com.cn
     * @Description: post请求
     * @CreateTime: 15:08 2018/12/26
     * @Params: [url, params]
     * @return: java.lang.String
     **/
    public static String doPost(Map<String,String> params){
        HttpPost post = new HttpPost(url);
        setRequestConfig(post);
        post.addHeader("Content-Type", "application/json;charset=utf-8");
        setPostParams(post,params);
        CloseableHttpResponse response = null;
        InputStream is = null;
        String result = null;
        try {
            response = getHttpClient().execute(post,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                result = IOUtils.toString(is, "utf-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * @Author: zhusm@bsoft.com.cn
     * @Description: post请求
     * @CreateTime: 14:18 2018/12/27
     * @Params: [json]
     * @return: java.lang.String
     **/
    public static <T> T doPost(String requesrtUri,Object json,Class<T> clazz){
        HttpPost post = null;
        String requestJson = JSONUtils.toString(json);
        logger.info(requestJson);
        if (StringUtils.isEmpty(requesrtUri)){
            post = new HttpPost(url);
        }else {
            post = new HttpPost(url+requesrtUri);
        }
        setRequestConfig(post);
        post.addHeader("Content-Type", "application/json;charset=utf-8");
        post.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = null;
        InputStream is = null;
        T result = null;
        try {
            logger.info("request -->"+post.getURI());
            response = getHttpClient().execute(post,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                String responseString = IOUtils.toString(is, "utf-8");
                logger.info("responseString -->"+responseString);
                result = JSONUtils.parse(responseString, clazz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    public static <T> T doPost(String requesrtUri,String orgId,Object json,Class<T> clazz){
        HttpPost post = null;
        String requestJson = JSONUtils.toString(json);
        logger.info(requestJson);
//        url = OrgIdResource.getUrl(orgId);
        if (StringUtils.isEmpty(requesrtUri)){
            post = new HttpPost(HttpConnectionPoolUtil.url);
        }else {
            post = new HttpPost(url+requesrtUri);
        }
        setRequestConfig(post);
        post.addHeader("Content-Type", "application/json;charset=utf-8");
        post.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = null;
        InputStream is = null;
        T result = null;
        try {
            logger.info("request -->"+post.getURI());
            response = getHttpClient().execute(post,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                String responseString = IOUtils.toString(is, "utf-8");
                logger.info("responseString -->"+responseString);
                result = JSONUtils.parse(responseString, clazz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * @Author: zhusm@bsoft.com.cn
     * @Description: post请求
     * @CreateTime: 14:18 2018/12/27
     * @Params: [json]
     * @return: java.lang.String
     **/
    public static <T> T doPostWithUrl(String requesrtUri,Object json,Class<T> clazz){
        String requestJson = JSONUtils.toString(json);
        logger.info(requestJson);
        HttpPost post  = new HttpPost(requesrtUri);
        setRequestConfig(post);
        post.addHeader("Content-Type", "application/json;charset=utf-8");
        post.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = null;
        InputStream is = null;
        T result = null;
        try {
            logger.info("request -->"+post.getURI());
            response = getHttpClient().execute(post,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                String responseString = IOUtils.toString(is, "utf-8");
                logger.info("responseString -->"+responseString);
                result = JSONUtils.parse(responseString, clazz);
            }
        } catch (IOException e) {
            logger.error("请求出错 -->"+e.getMessage());
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: wsdl请求
     * @CreateTime: 15:04 2019/5/22
     * @Params: [requesrtUri, request, clazz]
     * @return: T
     **/
    public static <T> T doWsdl(String request,Class<T> clazz) throws UnsupportedEncodingException {
        logger.info(url+"--->:{}",request);
        if (StringUtil.isEmpty(request) || StringUtil.isEmpty(url)){
            throw new IllegalArgumentException("请求地址或请求参数错误");
        }
        HttpPost post  = new HttpPost(url);
        setRequestConfig(post);
        post.addHeader("content-type", "text/xml;charset=utf-8");
        post.setEntity(new StringEntity(request));
        CloseableHttpResponse response = null;
        InputStream is = null;
        T result = null;
        try {
            logger.info("request -->"+post.getURI());
            response = getHttpClient().execute(post,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                String responseString = IOUtils.toString(is, "utf-8");
                logger.info("responseString -->"+responseString);
//                result = JSONUtils.parse(responseString, clazz);
            }
        } catch (IOException e) {
            logger.error("请求出错 -->"+e.getMessage());
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    /**
     * @Author zhusm
     * @Description get请求
     * @Date 13:33 2018/12/25
     * @Param [url, params, clazz]
     * @return T
     **/
    public static <T> T doGet(Class<T> clazz){
        HttpGet get = new HttpGet(url);
        setRequestConfig(get);
        CloseableHttpResponse response = null;
        T res = null;
        InputStream is = null;
        try {
            response = getHttpClient().execute(get,HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (null != entity){
                is = entity.getContent();
                String result = IOUtils.toString(is, "utf-8");
                res = JSON.parseObject(result, clazz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;

    }

    /**
     * @Author zhusm
     * @Description 关闭连接池  单次使用httpclient后调用
     * @Date 13:16 2018/12/25
     * @Param []
     * @return void
     **/
    public static void closeConnectionPool(){
        try {
            httpClient.close();
            manager.close();
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Data
    public static class HttpConnectionConfig {
        public static final int HTTP_MAX_PER_ROUTE = 40;
        private static final Integer HTTP_IDEL_TIMEOUT = 60;
        private static final Integer HTTP_MONITOR_INTERVAL = 60;
        private static final Integer HTTP_CONNECTION_TIMEOUT = 10000;
        private static final Integer HTTP_SOCKET_TIMEOUT = 30000;
        private static final Integer HTTP_MAX_POOL_SIZE = 200;//最大连接数
        private static final Integer HTTP_MAX_ROUTE = 100;//最大连接数
    }
}
