package com.aladdin.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 平台启动层
 * 单进程合并承载 auth(认证) + system(系统管理) + biz(业务)，日常只需启动本类
 * auth/biz 保持独立启动能力(AuthApplication=9200、BizApplication=9202，与本类同端口互斥)
 *
 * @author cles
 * @date 2026/09/23
 */
@SpringBootApplication(
        scanBasePackages = "com.aladdin",
        exclude = {
                org.redisson.spring.starter.RedissonAutoConfiguration.class
        }
)
@EnableDiscoveryClient
@MapperScan({"com.aladdin.system.dao", "com.aladdin.biz.dao"})
public class AladdinApplication {

    public static void main(String[] args) {
        SpringApplication.run(AladdinApplication.class, args);
    }
}
