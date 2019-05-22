package com.utils;/**
 * @Auther: Administrator
 * @Date: 2019/5/22 14:32
 * @Description:
 */

import com.google.common.cache.CacheLoader;

import java.util.concurrent.ExecutionException;

/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description: HttpClient缓存
 *
 * @Create: 2019-05-22 14:32
 **/
public class HttpClientCache extends SimpleCacha<String,Object>{

    @Override
    protected void setCache(String key, Object value) {
        super.newListenerLoadingCache(value,5L);
        super.setCache(key, value);
    }

    @Override
    protected CacheLoader<String, Object> overrideLoad() {
        CacheLoader<String, Object> cacheLoader = new CacheLoader<String, Object>() {
            @Override
            public Object load(String key) throws Exception {
                String response = HttpConnectionPoolUtil.doWsdl("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.tb_ris_report.srvc.xuyiqy.wondersgroup.com\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <ser:xmlfind>\n" +
                        "         <ser:req><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><request>\n" +
                        "    <pmap>\n" +
                        "        <item>\n" +
                        "           <key>kh</key>\n" +
                        "            <value>320830040602006501</value>\n" +
                        "        </item>\n" +
                        "    </pmap>\n" +
                        "    <header>\n" +
                        "        <password>3wrsfsdfasdf</password>\n" +
                        "        <pubkey>43233</pubkey>\n" +
                        "        <token>123456</token>\n" +
                        "        <username>mininin</username>\n" +
                        "    </header>\n" +
                        "</request>]]></ser:req>\n" +
                        "      </ser:xmlfind>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>", String.class);
                if (StringUtil.isEmpty(response)){
                    return "no response";
                }
                System.out.println(response);
                return response;
            }
        };
        return cacheLoader;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        HttpClientCache httpClientCache = new HttpClientCache();
        httpClientCache.setCache("1111","123");
        System.out.println(httpClientCache.getCache("11"));
    }
}
