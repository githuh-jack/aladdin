package com.aladdin.system.service.impl;

import com.aladdin.common.core.constant.RedisKeyConstant;
import com.aladdin.common.db.base.BaseServiceImpl;
import com.aladdin.common.redis.RedisService;
import com.aladdin.system.dao.SysTenantDao;
import com.aladdin.system.entity.SysTenant;
import com.aladdin.system.service.SysTenantService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 租户服务实现
 *
 * @author cles
 * @date 2026/06/12
 */
@Service
public class SysTenantServiceImpl extends BaseServiceImpl<SysTenantDao, SysTenant> implements SysTenantService {

    private final RedisService redisService;

    public SysTenantServiceImpl(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public SysTenant getByTenantCode(String tenantCode) {
        return getMapper().selectByTenantCode(tenantCode);
    }

    @Override
    public boolean isValid(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        // 先从缓存取
        String key = RedisKeyConstant.TENANT + tenantId;
        Object cached = redisService.get(key);
        if (cached != null) {
            return "1".equals(cached.toString());
        }
        SysTenant tenant = getById(tenantId);
        boolean valid = tenant != null
                && (tenant.getStatus() == null || tenant.getStatus() == 1)
                && (tenant.getExpireTime() == null || tenant.getExpireTime().isAfter(LocalDateTime.now()));
        redisService.set(key, valid ? "1" : "0", 30, TimeUnit.MINUTES);
        return valid;
    }
}
