package com.dongjian.erp.manufacturingerpsystem.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI erpOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("制造企业ERP系统 API")
                        .description("毕业设计后端接口文档")
                        .version("v1.0.0")
                        .license(new License().name("For Graduation Project")))
                .externalDocs(new ExternalDocumentation()
                        .description("Knife4j 文档页")
                        .url("/doc.html"));
    }
}
