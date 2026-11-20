package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class StockTrackerApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    String ddlAuto;

    @Test
    void flywayBuildsTheSchemaAndHibernateOnlyValidatesIt() {
        // The context started, so Hibernate validated every mapping against Flyway's schema.
        assertThat(ddlAuto).isEqualTo("validate");
        assertThat(jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class))
                .containsExactly("1", "2");
        assertThat(jdbc.queryForObject("show server_version", String.class)).startsWith("18.");
    }
}
