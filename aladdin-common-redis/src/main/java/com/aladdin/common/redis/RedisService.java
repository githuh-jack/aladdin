package com.aladdin.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisAvailable;

    public RedisService(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider,
                        ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.redisAvailable = this.redisTemplate != null && this.stringRedisTemplate != null;
        if (!redisAvailable) {
            log.warn("Redis不可用，RedisService将以空操作模式运行");
        }
    }

    public void set(String key, Object value) {
        if (redisAvailable) {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (redisAvailable) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        }
    }

    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        if (redisAvailable) {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
            return result != null && result;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (redisAvailable) {
            return (T) redisTemplate.opsForValue().get(key);
        }
        return null;
    }

    public String getString(String key) {
        if (redisAvailable) {
            return stringRedisTemplate.opsForValue().get(key);
        }
        return null;
    }

    public Boolean delete(String key) {
        if (redisAvailable) {
            return redisTemplate.delete(key);
        }
        return false;
    }

    public Long delete(Collection<String> keys) {
        if (redisAvailable) {
            return redisTemplate.delete(keys);
        }
        return 0L;
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (redisAvailable) {
            return redisTemplate.expire(key, timeout, unit);
        }
        return false;
    }

    public Long getExpire(String key) {
        if (redisAvailable) {
            return redisTemplate.getExpire(key);
        }
        return -1L;
    }

    public Boolean hasKey(String key) {
        if (redisAvailable) {
            return redisTemplate.hasKey(key);
        }
        return false;
    }

    public Long increment(String key) {
        if (redisAvailable) {
            return redisTemplate.opsForValue().increment(key);
        }
        return null;
    }

    public Long increment(String key, long delta) {
        if (redisAvailable) {
            return redisTemplate.opsForValue().increment(key, delta);
        }
        return null;
    }

    public Long decrement(String key) {
        if (redisAvailable) {
            return redisTemplate.opsForValue().decrement(key);
        }
        return null;
    }

    public void hSet(String key, String hashKey, Object value) {
        if (redisAvailable) {
            redisTemplate.opsForHash().put(key, hashKey, value);
        }
    }

    public Object hGet(String key, String hashKey) {
        if (redisAvailable) {
            return redisTemplate.opsForHash().get(key, hashKey);
        }
        return null;
    }

    public Map<Object, Object> hGetAll(String key) {
        if (redisAvailable) {
            return redisTemplate.opsForHash().entries(key);
        }
        return Collections.emptyMap();
    }

    public void hPutAll(String key, Map<String, Object> map) {
        if (redisAvailable) {
            redisTemplate.opsForHash().putAll(key, map);
        }
    }

    public Long hDelete(String key, Object... hashKeys) {
        if (redisAvailable) {
            return redisTemplate.opsForHash().delete(key, hashKeys);
        }
        return 0L;
    }

    public Boolean hHasKey(String key, String hashKey) {
        if (redisAvailable) {
            return redisTemplate.opsForHash().hasKey(key, hashKey);
        }
        return false;
    }

    public Long sAdd(String key, Object... values) {
        if (redisAvailable) {
            return redisTemplate.opsForSet().add(key, values);
        }
        return 0L;
    }

    public Set<Object> sMembers(String key) {
        if (redisAvailable) {
            return redisTemplate.opsForSet().members(key);
        }
        return Collections.emptySet();
    }

    public Boolean sIsMember(String key, Object value) {
        if (redisAvailable) {
            return redisTemplate.opsForSet().isMember(key, value);
        }
        return false;
    }

    public Long sRemove(String key, Object... values) {
        if (redisAvailable) {
            return redisTemplate.opsForSet().remove(key, values);
        }
        return 0L;
    }

    public Boolean zAdd(String key, Object value, double score) {
        if (redisAvailable) {
            return redisTemplate.opsForZSet().add(key, value, score);
        }
        return false;
    }

    public Set<Object> zRange(String key, long start, long end) {
        if (redisAvailable) {
            return redisTemplate.opsForZSet().range(key, start, end);
        }
        return Collections.emptySet();
    }

    public Long zRemove(String key, Object... values) {
        if (redisAvailable) {
            return redisTemplate.opsForZSet().remove(key, values);
        }
        return 0L;
    }

    public Long lPush(String key, Object value) {
        if (redisAvailable) {
            return redisTemplate.opsForList().leftPush(key, value);
        }
        return 0L;
    }

    public List<Object> lRange(String key, long start, long end) {
        if (redisAvailable) {
            return redisTemplate.opsForList().range(key, start, end);
        }
        return Collections.emptyList();
    }

    public Long lSize(String key) {
        if (redisAvailable) {
            return redisTemplate.opsForList().size(key);
        }
        return 0L;
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}
