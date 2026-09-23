package com.aladdin.system.controller;

import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.redis.RedisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * IP黑白名单管理控制器
 *
 * @author cles
 * @date 2026/06/12
 */
@RestController
@RequestMapping("/ipList")
public class IpListController {

    private final RedisService redisService;

    public IpListController(RedisService redisService) {
        this.redisService = redisService;
    }

    /** 获取黑名单列表 */
    @GetMapping("/blacklist")
    @PreAuthorize("hasAuthority('system:ip:list')")
    public R<Set<Object>> getBlacklist() {
        return R.ok(redisService.sMembers(RedisKeyConstant.IP_BLACKLIST));
    }

    /** 添加黑名单IP */
    @PostMapping("/blacklist")
    @PreAuthorize("hasAuthority('system:ip:edit')")
    public R<Void> addBlacklist(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isEmpty()) {
            return R.fail("IP不能为空");
        }
        redisService.sAdd(RedisKeyConstant.IP_BLACKLIST, ip);
        return R.ok();
    }

    /** 移除黑名单IP */
    @DeleteMapping("/blacklist/{ip}")
    @PreAuthorize("hasAuthority('system:ip:edit')")
    public R<Void> removeBlacklist(@PathVariable String ip) {
        redisService.sRemove(RedisKeyConstant.IP_BLACKLIST, ip);
        return R.ok();
    }

    /** 获取白名单列表 */
    @GetMapping("/whitelist")
    @PreAuthorize("hasAuthority('system:ip:list')")
    public R<Set<Object>> getWhitelist() {
        return R.ok(redisService.sMembers(RedisKeyConstant.IP_WHITELIST));
    }

    /** 添加白名单IP */
    @PostMapping("/whitelist")
    @PreAuthorize("hasAuthority('system:ip:edit')")
    public R<Void> addWhitelist(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isEmpty()) {
            return R.fail("IP不能为空");
        }
        redisService.sAdd(RedisKeyConstant.IP_WHITELIST, ip);
        return R.ok();
    }

    /** 移除白名单IP */
    @DeleteMapping("/whitelist/{ip}")
    @PreAuthorize("hasAuthority('system:ip:edit')")
    public R<Void> removeWhitelist(@PathVariable String ip) {
        redisService.sRemove(RedisKeyConstant.IP_WHITELIST, ip);
        return R.ok();
    }
}
