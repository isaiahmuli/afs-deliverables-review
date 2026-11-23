package com.example.invoices.config;

import com.example.invoices.error.Problems;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Browsers may call the API only from the listed origins (per environment, in configuration).
 * A rejected CORS request gets the same problem+json shape as every other refusal.
 */
@Configuration
public class CorsConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(@Value("${billing.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        cors.setAllowedHeaders(List.of("Content-Type", "Idempotency-Key"));
        cors.setExposedHeaders(List.of("Location", "Idempotent-Replayed"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        CorsFilter filter = new CorsFilter(source);
        filter.setCorsProcessor(new ProblemCorsProcessor());

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    static class ProblemCorsProcessor extends DefaultCorsProcessor {
        @Override
        protected void rejectRequest(ServerHttpResponse response) throws IOException {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            String body = """
                    {"type":"%scors-rejected","title":"Origin not allowed","status":403,\
                    "detail":"This origin is not allowed to call the API from a browser."}"""
                    .formatted(Problems.TYPE_BASE);
            response.getBody().write(body.getBytes(StandardCharsets.UTF_8));
            response.flush();
        }
    }
}
