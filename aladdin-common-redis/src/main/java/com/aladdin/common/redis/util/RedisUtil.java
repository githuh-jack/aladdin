package com.aladdin.common.redis.util;

import com.aladdin.common.redis.RedisService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类（静态方法，兼容旧代码）
 *
 * @author cles
 * @date 2026/05/08
 */
@Component
public class RedisUtil {

    private static RedisService redisServiceInstance;

    private final RedisService redisService;

    public RedisUtil(RedisService redisService) {
        this.redisService = redisService;
    }

    @PostConstruct
    public void init() {
        redisServiceInstance = this.redisService;
    }

    public static void setString(String key, long timeout, String value) {
        redisServiceInstance.set(key, value, timeout, TimeUnit.SECONDS);
    }

    public static void setString(String key, long timeout, Object value) {
        redisServiceInstance.set(key, value, timeout, TimeUnit.SECONDS);
    }

    public static String getString(String key) {
        Object value = redisServiceInstance.get(key);
        return value != null ? value.toString() : null;
    }

    public static void setObj(String key, Object value) {
        redisServiceInstance.set(key, value);
    }

    public static void setObj(String key, Object value, long timeout) {
        redisServiceInstance.set(key, value, timeout, TimeUnit.SECONDS);
    }

    public static <T> T getObj(String key) {
        return redisServiceInstance.get(key);
    }

    public static Boolean delete(String key) {
        return redisServiceInstance.delete(key);
    }

    public static Boolean expire(String key, long timeout) {
        return redisServiceInstance.expire(key, timeout, TimeUnit.SECONDS);
    }

    public static Boolean hasKey(String key) {
        return redisServiceInstance.hasKey(key);
    }

    public static Long increment(String key) {
        return redisServiceInstance.increment(key);
    }

    public static Long increment(String key, long delta) {
        return redisServiceInstance.increment(key, delta);
    }

    public static void hSet(String key, String hashKey, Object value) {
        redisServiceInstance.hSet(key, hashKey, value);
    }

    public static Object hGet(String key, String hashKey) {
        return redisServiceInstance.hGet(key, hashKey);
    }

    public static Map<Object, Object> hGetAll(String key) {
        return redisServiceInstance.hGetAll(key);
    }

    public static Long sAdd(String key, Object... values) {
        return redisServiceInstance.sAdd(key, values);
    }

    public static Set<Object> sMembers(String key) {
        return redisServiceInstance.sMembers(key);
    }

    public static Long lPush(String key, Object value) {
        return redisServiceInstance.lPush(key, value);
    }

    public static java.util.List<Object> lRange(String key, long start, long end) {
        return redisServiceInstance.lRange(key, start, end);
    }
}
