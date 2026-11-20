package com.example.stock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * The whole application against PostgreSQL 18 in a container. Not wrapped in a test transaction,
 * so commits, rollbacks and concurrent transactions behave as they do in production.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(locations = "classpath:test.properties")
public @interface IntegrationTest {
}
