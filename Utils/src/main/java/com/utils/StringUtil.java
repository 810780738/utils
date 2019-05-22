package com.utils;/**
 * @Auther: Administrator
 * @Date: 2019/5/22 11:11
 * @Description:
 */

import org.apache.commons.lang.StringUtils;

/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description: 字符串工具类
 *
 * @Create: 2019-05-22 11:11
 **/
public class StringUtil {

    public static boolean isEmpty(String value){
        if (value != null){
            value.trim();
        }
        return StringUtils.isEmpty(value);
    }
    public static boolean isNotEmpty(String value){
        return !isEmpty(value);
    }
}
