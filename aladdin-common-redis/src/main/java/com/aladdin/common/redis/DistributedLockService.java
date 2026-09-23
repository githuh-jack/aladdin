package com.aladdin.common.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务
 *
 * @author cles
 * @date 2026/05/06
 */
@Component
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁中断: key={}", lockKey);
            return false;
        }
    }

    public void lock(String lockKey, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        lock.lock(leaseTime, unit);
    }

    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        return lock.isLocked();
    }

    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, LockTask<T> task) {
        if (!tryLock(lockKey, waitTime, leaseTime, unit)) {
            throw new RuntimeException("获取分布式锁失败: " + lockKey);
        }
        try {
            return task.execute();
        } finally {
            unlock(lockKey);
        }
    }

    @FunctionalInterface
    public interface LockTask<T> {
        T execute();
    }
}
