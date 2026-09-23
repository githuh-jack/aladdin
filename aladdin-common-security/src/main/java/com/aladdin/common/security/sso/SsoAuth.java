package com.aladdin.common.security.sso;

import com.aladdin.common.security.entity.OmUser;
import com.aladdin.common.redis.RedisService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SSO认证工具
 *
 * @author cles
 * @date 2026/05/08
 */
public class SsoAuth {

    private static final ConcurrentHashMap<String, OmUser> TOKEN_MAP = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRE_HOURS = 2;

    private static RedisService redisService;

    public static void setRedisService(RedisService redisService) {
        SsoAuth.redisService = redisService;
    }

    public static String generateToken(OmUser user) {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        TOKEN_MAP.put(token, user);
        if (redisService != null) {
            redisService.set("sso:token:" + token, user, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        return token;
    }

    public static OmUser getUserByToken(String token) {
        if (token == null) {
            return null;
        }
        OmUser user = TOKEN_MAP.get(token);
        if (user == null && redisService != null) {
            user = redisService.get("sso:token:" + token);
            if (user != null) {
                TOKEN_MAP.put(token, user);
            }
        }
        return user;
    }

    public static void removeToken(String token) {
        if (token != null) {
            TOKEN_MAP.remove(token);
            if (redisService != null) {
                redisService.delete("sso:token:" + token);
            }
        }
    }
}
