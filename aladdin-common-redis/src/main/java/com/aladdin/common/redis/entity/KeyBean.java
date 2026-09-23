package com.aladdin.common.redis.entity;

import lombok.Data;

/**
 * Redis键信息
 *
 * @author cles
 * @date 2026/05/07
 */
@Data
public class KeyBean {

    private long ttl;

    private long size;

    private String key;

    private String type;

    private String text;

    private String json;

    private String raws;

    private String hexs;

    private Object data;
}
