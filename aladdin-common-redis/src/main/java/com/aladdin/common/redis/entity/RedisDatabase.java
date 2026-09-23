package com.aladdin.common.redis.entity;

/**
 * Redis数据库索引枚举
 *
 * @author cles
 * @date 2026/05/07
 */
public enum RedisDatabase {

    API(0),
    DATA(1);

    private final int index;

    public int getIndex() {
        return index;
    }

    RedisDatabase(int index) {
        this.index = index;
    }
}
