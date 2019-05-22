package com.utils;/**
 * @Auther: Administrator
 * @Date: 2019/5/22 10:51
 * @Description:
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description: properties文件读取
 *
 * @Create: 2019-05-22 10:51
 **/
public class PropsUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropsUtil.class);

    public static Properties loadProps(String fileName) throws FileNotFoundException {
        InputStream is = null;
        Properties properties = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                throw new FileNotFoundException(fileName + "file is not found");
            }
            properties = new Properties();
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 获取字符类型属性（默认为空）
     * @CreateTime: 10:59 2019/5/22
     * @Params: [props, key]
     * @return: java.lang.String
     **/
    public static String getString(Properties props,String key){
        return getString(props,key,"");
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 获取字符型属性，指定默认类型
     * @CreateTime: 10:59 2019/5/22
     * @Params: [props, key, s]
     * @return: java.lang.String
     **/
    public static String getString(Properties props, String key, String defaultValue) {
        String value = defaultValue;
        if (props.containsKey(key)){
            value = props.getProperty(key);
        }
        return value;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 获取int型值
     * @CreateTime: 11:22 2019/5/22
     * @Params: [props, key]
     * @return: int
     **/
    public static int getInt(Properties props,String key){
        return getInt(props,key,0);
    }

    public static int getInt(Properties props, String key, int defaultInt) {
        int value = defaultInt;
        if (props.containsKey(key)){
            value = CastUtil.castInt(props.getProperty(key));
        }
        return value;
    }

    public static boolean getBoolean(Properties props,String key){
        return getBoolean(props,key,false);
    }

    private static boolean getBoolean(Properties props, String key, boolean defalutValue) {
        boolean value = defalutValue;
        if (props.containsKey(key)){
            value = CastUtil.castBoolean(props.getProperty(key));
        }
        return value;
    }

}
