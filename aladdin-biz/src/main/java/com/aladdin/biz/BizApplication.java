package com.aladdin.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 业务模块启动类
 * 承载：信件/好友/日记/感想/笔记/邮票/信封/铜钱/订单 等C端业务
 *
 * @author cles
 * @date 2026/09/15
 */
@SpringBootApplication(
        scanBasePackages = {"com.aladdin.biz", "com.aladdin.common"},
        exclude = {
                org.redisson.spring.starter.RedissonAutoConfiguration.class
        }
)
@EnableDiscoveryClient
@MapperScan({"com.aladdin.biz.dao"})
public class BizApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizApplication.class, args);
    }
}
