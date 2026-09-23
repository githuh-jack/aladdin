package com.aladdin.common.redis.entity;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Redis服务器信息
 *
 * @author cles
 * @date 2026/05/07
 */
@Data
public class RedisInfo {

    private String server = "";
    private String client = "";
    private String memory = "";
    private String persistence = "";
    private String stats = "";
    private String replication = "";
    private String cpu = "";
    private String cluster = "";
    private String keyspace = "";
    private List<ClientInfo> users = Collections.emptyList();
}
