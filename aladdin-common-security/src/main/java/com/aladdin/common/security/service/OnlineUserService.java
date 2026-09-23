package com.aladdin.common.security.service;

import com.aladdin.common.redis.RedisService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 在线用户管理服务
 *
 * @author cles
 * @date 2026/05/06
 */
@Component
public class OnlineUserService {

    private static final String ONLINE_PREFIX = "online:user:";
    private static final String ONLINE_SET = "online:users";

    private final RedisService redisService;
    private final TokenService tokenService;

    public OnlineUserService(RedisService redisService, TokenService tokenService) {
        this.redisService = redisService;
        this.tokenService = tokenService;
    }

    /**
     * 用户上线
     */
    public void userOnline(Long userId, String username) {
        redisService.set(ONLINE_PREFIX + userId, username, tokenService.getExpireMinutes(), TimeUnit.MINUTES);
        redisService.sAdd(ONLINE_SET, userId);
    }

    /**
     * 用户下线
     */
    public void userOffline(Long userId) {
        redisService.delete(ONLINE_PREFIX + userId);
        redisService.sRemove(ONLINE_SET, userId);
    }

    /**
     * 获取在线用户数
     */
    public long getOnlineCount() {
        Set<Object> members = redisService.sMembers(ONLINE_SET);
        return members != null ? members.size() : 0;
    }

    /**
     * 获取在线用户列表
     */
    public List<Map<String, Object>> getOnlineUsers() {
        Set<Object> members = redisService.sMembers(ONLINE_SET);
        List<Map<String, Object>> result = new ArrayList<>();
        if (members == null) {
            return result;
        }
        for (Object memberId : members) {
            Long userId = Long.parseLong(memberId.toString());
            String username = redisService.get(ONLINE_PREFIX + userId);
            if (username != null) {
                Map<String, Object> user = new java.util.HashMap<>();
                user.put("userId", userId);
                user.put("username", username);
                result.add(user);
            } else {
                redisService.sRemove(ONLINE_SET, userId);
            }
        }
        return result;
    }

    /**
     * 强制下线
     */
    public void forceLogout(Long userId) {
        tokenService.removeToken(userId);
        userOffline(userId);
    }

    /**
     * 判断用户是否在线
     */
    public boolean isOnline(Long userId) {
        return redisService.sIsMember(ONLINE_SET, userId);
    }
}
