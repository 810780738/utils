package com.utils;

import com.google.common.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @Author: zhusm@bsoft.com.cn
 *
 * @Description: 缓存
 *
 * @Create: 2019-04-24 16:10
 **/
public abstract class SimpleCacha<K,T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCacha.class);

    //过期时间默认永不过期
    protected long timeOuts = 60 * 60 * 24 * 365;
    //缓存并发数
    protected int concurrencyLevel=3;
    //最大容量
    protected int maximumSize = 100;
    //初始化容量
    protected int initialCapacity = 10;

    private volatile LoadingCache<K,T> loadingCache;
    private volatile Cache<K,T> cache;

    AtomicReference<RemovalCause> removenotification = new AtomicReference<>();
    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description:初始化设置
     * @CreateTime: 17:23 2019/4/24
     * @Params: []
     * @return: com.google.common.cache.CacheBuilder<java.lang.Object,java.lang.Object>
     **/
    public CacheBuilder<Object,Object> cachaInstance(){
        return CacheBuilder.newBuilder()
                //并发数
                .concurrencyLevel(concurrencyLevel)
                //初始化容量
                .initialCapacity(initialCapacity)
                //最大容量 使用量最少删除
                .maximumSize(maximumSize);
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description:缓存不存在时触发
     * @CreateTime: 14:39 2019/5/22
     * @Params: []
     * @return: com.google.common.cache.CacheLoader<K,T>
     **/
    protected CacheLoader<K, T> overrideLoad(){
        CacheLoader<K, T> cacheLoader = new CacheLoader<K, T>() {
            @Override
            public T load(K k) throws Exception {
                return null;
            }
        };
        return cacheLoader;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 缓存移除通知
     * @CreateTime: 17:31 2019/4/24
     * @Params: [b] 是否返回移除通知信息
     * @return: java.lang.String
     **/
    protected CacheBuilder<Object, Object> setListener(){
        return cachaInstance()
                .removalListener(
                notification -> {
                    LOGGER.info(notification.getKey() + " " + notification.getValue() + " 被移除,原因:" + notification.getCause());
                    removenotification.set(notification.getCause());
                }
        );
    }

    protected LoadingCache<K,T> newLoadingCache(T t){
        if(loadingCache == null){
            loadingCache = cachaInstance().expireAfterWrite(timeOuts,TimeUnit.SECONDS).build(overrideLoad());
            if(loadingCache == null){
                synchronized (SimpleCacha.class){
                    loadingCache = cachaInstance().expireAfterWrite(timeOuts,TimeUnit.SECONDS).build(overrideLoad());
                }
            }
        }
        return loadingCache;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description:
     * @CreateTime: 12:28 2019/5/17
     * @Params: [t]
     * @return: com.google.common.cache.Cache<K,T>
     **/
    protected Cache<K,T> newNoLoadCache(T t) {
        if (cache == null) {
            cache = cachaInstance().newBuilder().expireAfterWrite(timeOuts, TimeUnit.SECONDS).build();
            if (cache == null) {
                synchronized (SimpleCacha.class) {
                    cache = cachaInstance().newBuilder().expireAfterWrite(timeOuts, TimeUnit.SECONDS).build();
                }
            }
        }
        return cache;
    }

    protected Cache<K,T> newNoLoadCache(T t, long timeOuts) {
        if (cache == null) {
            cache = cachaInstance().newBuilder().expireAfterWrite(timeOuts, TimeUnit.SECONDS).build();
            if (cache == null) {
                synchronized (SimpleCacha.class) {
                    cache = cachaInstance().newBuilder().expireAfterWrite(timeOuts, TimeUnit.SECONDS).build();
                }
            }
        }
        return cache;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 设置超时超时时间的缓存
     * @CreateTime: 19:19 2019/4/24
     * @Params: [t, timeout]
     * @return: com.google.common.cache.LoadingCache<java.lang.String,T>
     **/
    protected LoadingCache<K,T> newLoadingCache(T t, long timeout){
        if(loadingCache == null){
            loadingCache = cachaInstance()
                    .expireAfterWrite(timeout,TimeUnit.SECONDS)
                    .build(overrideLoad());
            synchronized (SimpleCacha.class){
                if(loadingCache == null) {
                    loadingCache = cachaInstance()
                            .expireAfterWrite(timeout, TimeUnit.SECONDS)
                            .build(overrideLoad());
                }
            }
        }
        return loadingCache;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 单例创建LoadingCache
     * @CreateTime: 22:51 2019/5/16
     * @Params: [t]
     * @return: com.google.common.cache.LoadingCache<java.lang.String,T>
     **/
    public LoadingCache<K,T> newListenerLoadingCache(T t) {
        if(loadingCache == null){
            loadingCache = setListener().expireAfterWrite(timeOuts,TimeUnit.SECONDS).build(overrideLoad());
            synchronized (SimpleCacha.class) {
                if (loadingCache == null) {
                    loadingCache = setListener().expireAfterWrite(timeOuts,TimeUnit.SECONDS).build(overrideLoad());
                }
            }
        }
        return loadingCache;
    }

    /***
     * @Author: zhusm@bsoft.com.cn
     * @Description: 设置超时时间的缓存移除通知  当缓存不存在时触发load方法
     * @CreateTime: 19:22 2019/4/24
     * @Params: [t, timeoout]
     * @return: com.google.common.cache.LoadingCache<java.lang.String,T>
     **/
    protected LoadingCache<K,T> newListenerLoadingCache(T t, long timeout){
        if(loadingCache == null){
            loadingCache = setListener()
                    .expireAfterWrite(timeout,TimeUnit.SECONDS)
                    .build(overrideLoad());
            synchronized (SimpleCacha.class){
                if (loadingCache == null) {
                    loadingCache = setListener()
                            .expireAfterWrite(timeout,TimeUnit.SECONDS)
                            .build(overrideLoad());
                }
            }
        }

        return loadingCache;
    }


    protected void setCache(K key,T value){
        loadingCache = newListenerLoadingCache(value);
        loadingCache.put(key,value);
    }

    protected void setNoLoadCache(K key,T value){
        cache = newNoLoadCache(value);
        cache.put(key,value);
    }


    protected void remove(K key){
        loadingCache.invalidate(key);
    }

    protected void removeNoLoadCache(K key){
        cache.invalidate(key);
    }

    protected T getCache(K key) throws ExecutionException {
        return loadingCache.get(key);
    }
    protected T getNoLoadCache(K key) throws ExecutionException {
        return cache.getIfPresent(key);
    }

    protected T getIfPresent(K key){
        return loadingCache.getIfPresent(key);
    }
}
