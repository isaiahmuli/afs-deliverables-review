package com.example.payments.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI paymentsOpenApi() {
        return new OpenAPI().info(new Info().title("Payments API").version("v1")
                .description("Payment links for sent invoices, and the provider's payment reports. Refusals are application/problem+json."));
    }
}
