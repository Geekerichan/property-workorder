package com.prop.wo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档：/doc.html，可直接在线调试
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI propertyWorkOrderOpenApi() {
        return new OpenAPI().info(new Info()
                .title("物业公司运维工单管理系统 API")
                .description("物业端 /api/admin、作业端 /api/app、业主端 /api/h5")
                .version("1.0.0"));
    }
}
