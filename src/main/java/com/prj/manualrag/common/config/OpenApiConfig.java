package com.prj.manualrag.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Manual RAG API")
                                .description("사용자 설명서 기반 RAG 서비스")
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("Manual RAG")
                                )
                );
    }
}
