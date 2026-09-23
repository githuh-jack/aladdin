package com.aladdin.common.security.service;

import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.security.config.SecurityProperties;
import com.aladdin.common.redis.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token服务
 *
 * @author cles
 * @date 2026/05/06
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final SecurityProperties securityProperties;
    private final RedisService redisService;
    private SecretKey secretKey;

    public TokenService(SecurityProperties securityProperties, RedisService redisService) {
        this.securityProperties = securityProperties;
        this.redisService = redisService;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = securityProperties.getToken().getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 创建Token
     */
    public String createToken(Long userId, String username, Map<String, Object> claims) {
        Date now = new Date();
        long expireMs = securityProperties.getToken().getExpireMinutes() * 60 * 1000L;
        Date expireDate = new Date(now.getTime() + expireMs);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        redisService.set(RedisKeyConstant.LOGIN_TOKEN + userId, token, securityProperties.getToken().getExpireMinutes(), TimeUnit.MINUTES);
        redisService.set(RedisKeyConstant.LOGIN_USER + userId, username, securityProperties.getToken().getExpireMinutes(), TimeUnit.MINUTES);

        return token;
    }

    /**
     * 解析Token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String cachedToken = redisService.get(RedisKeyConstant.LOGIN_TOKEN + userId);
            if (cachedToken == null || !cachedToken.equals(token)) {
                log.warn("Token已失效或已被踢出: userId={}", userId);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从Token获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 刷新Token（续期）
     */
    public String refreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);

            long expireMs = securityProperties.getToken().getExpireMinutes() * 60 * 1000L;
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();

            if (remaining < expireMs / 2) {
                return createToken(userId, username, null);
            }

            redisService.expire(RedisKeyConstant.LOGIN_TOKEN + userId, securityProperties.getToken().getExpireMinutes(), TimeUnit.MINUTES);
            redisService.expire(RedisKeyConstant.LOGIN_USER + userId, securityProperties.getToken().getExpireMinutes(), TimeUnit.MINUTES);
            return token;
        } catch (Exception e) {
            log.warn("Token刷新失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 移除Token（踢出用户）
     */
    public void removeToken(Long userId) {
        redisService.delete(RedisKeyConstant.LOGIN_TOKEN + userId);
        redisService.delete(RedisKeyConstant.LOGIN_USER + userId);
    }

    public long getExpireMinutes() {
        return securityProperties.getToken().getExpireMinutes();
    }

    /**
     * Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
