package com.example.payments.clients;

import java.net.http.HttpClient;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/** Declarative clients on RestClient; each group takes its base URL and timeouts from configuration. */
@Configuration
@ImportHttpServices(group = "invoices", types = InvoicesClient.class)
@ImportHttpServices(group = "provider", types = ProviderClient.class)
public class HttpClientsConfig {

    /**
     * Java's HttpClient offers plain-HTTP servers an upgrade to HTTP/2 (h2c), and Spring sends the
     * JSON body in chunks. WireMock does not handle that combination, so every checkout came back
     * 400 or reset. Plain HTTP/1.1 avoids the upgrade.
     */
    @Bean
    ClientHttpRequestFactoryBuilder<?> httpOneOneRequestFactory() {
        return ClientHttpRequestFactoryBuilder.jdk()
                .withHttpClientCustomizer(builder -> builder.version(HttpClient.Version.HTTP_1_1));
    }
}
