package com.utils;/**
 * @Auther: Administrator
 * @Date: 2019/5/22 14:48
 * @Description:
 */

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description:
 *
 * @Create: 2019-05-22 14:48
 **/
public class JSONUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    public JSONUtils() {
    }

    public static <T> T parse(String value, Class<T> clz) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            try {
                return mapper.readValue(value, clz);
            } catch (Exception var3) {
                throw new IllegalStateException(var3);
            }
        }
    }

    public static <T> T parse(byte[] bytes, Class<T> clz) {
        try {
            return mapper.readValue(bytes, clz);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static <T> T parse(InputStream ins, Class<T> clz) {
        try {
            return mapper.readValue(ins, clz);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static <T> T parse(Reader reader, Class<T> clz) {
        try {
            return mapper.readValue(reader, clz);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static <T> T update(String value, T object) {
        try {
            return mapper.readerForUpdating(object).readValue(value);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static String writeValueAsString(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception var2) {
            throw new IllegalStateException(var2);
        }
    }

    public static void write(OutputStream outs, Object o) {
        try {
            mapper.writeValue(outs, o);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static void write(Writer writer, Object o) {
        try {
            mapper.writeValue(writer, o);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static String toString(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception var2) {
            throw new IllegalStateException(var2);
        }
    }

    public static String toString(Object o, Class<?> clz) {
        try {
            return mapper.writerWithType(clz).writeValueAsString(o);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    public static byte[] toBytes(Object o) {
        try {
            return mapper.writeValueAsBytes(o);
        } catch (Exception var2) {
            throw new IllegalStateException(var2);
        }
    }

    static {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(Include.NON_NULL);
    }
}
