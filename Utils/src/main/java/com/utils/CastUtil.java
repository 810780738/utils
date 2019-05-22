package com.utils;/**
 * @Auther: Administrator
 * @Date: 2019/5/22 11:06
 * @Description:
 */

/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description: 转换类型工具
 *
 * @Create: 2019-05-22 11:06
 **/
public class CastUtil {
    public static int castInt(Object o) {
        return castInt(o,0);
    }

    public static int castInt(Object o, int defaultValue) {
        int value = defaultValue;
        if (o != null){
            String strValue = castString(o);
            if (StringUtil.isNotEmpty(strValue)){
                try{
                    value = Integer.parseInt(strValue);
                }catch (NumberFormatException e){
                    value = defaultValue;
                }
            }
        }
        return value;
    }

    public static String castString(Object o) {
        return castString(o,"");
    }

    public static String castString(Object o, String defaultVaue) {
        return o != null ? String.valueOf(o):defaultVaue;
    }

    public static boolean castBoolean(Object o) {
        return castBoolean(o,false);
    }

    private static boolean castBoolean(Object o, boolean defaluet) {
        boolean value = defaluet;
        if (o != null){
            value = Boolean.parseBoolean(castString(o));
        }
        return value;
    }
}
