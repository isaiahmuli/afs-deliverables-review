package com.example.invoices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI invoicesOpenApi() {
        return new OpenAPI().info(new Info().title("Invoices API").version("v1")
                .description("Create, view, send, cancel and delete invoices. Refusals are application/problem+json."));
    }
}
