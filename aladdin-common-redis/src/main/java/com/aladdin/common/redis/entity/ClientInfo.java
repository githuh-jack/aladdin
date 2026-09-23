package com.aladdin.common.redis.entity;

import lombok.Data;

/**
 * Redis客户端信息
 *
 * @author cles
 * @date 2026/05/07
 */
@Data
public class ClientInfo {

    private String id;

    private String addr;

    private String age;

    private String db;
}
