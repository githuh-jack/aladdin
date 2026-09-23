package com.aladdin.common.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis配置属性
 *
 * @author cles
 * @date 2026/05/08
 */
@Component
@ConfigurationProperties(prefix = "spring.redis")
public class JedisConfig {

    private static boolean enableRedis = true;

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;

    public static boolean getEnableRedis() {
        return enableRedis;
    }

    public static void setEnableRedis(boolean enable) {
        enableRedis = enable;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }
}
