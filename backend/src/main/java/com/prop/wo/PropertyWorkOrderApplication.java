package com.prop.wo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 物业公司运维工单管理系统 启动类
 */
@EnableScheduling
@MapperScan("com.prop.wo.modules.**.mapper")
@SpringBootApplication
public class PropertyWorkOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(PropertyWorkOrderApplication.class, args);
    }
}
