package com.example.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Flyway on its own, against an empty PostgreSQL 18 database. */
@Testcontainers
class MigrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_18);

    private static Flyway flyway(String schema, String target) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema)
                .target(target)
                .load();
    }

    private static JdbcTemplate jdbc(String schema) {
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl() + "&currentSchema=" + schema,
                postgres.getUsername(), postgres.getPassword());
        return new JdbcTemplate(ds);
    }

    @Test
    void allMigrationsApplyToAnEmptyDatabase() {
        MigrateResult result = flyway("empty_db", "latest").migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(2);
        assertThat(result.targetSchemaVersion).isEqualTo("2");
        assertThat(jdbc("empty_db").queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'empty_db' and table_name <> 'flyway_schema_history'
                order by table_name""", String.class))
                .containsExactly("category", "product", "product_category", "stock_adjustment");
    }

    @Test
    void v2KeepsRecordsWrittenUnderV1() {
        flyway("upgrade", "1").migrate();
        JdbcTemplate db = jdbc("upgrade");
        db.update("insert into product (name, quantity) values ('Notebooks', 25)");
        db.update("insert into category (name) values ('Stationery')");
        db.update("insert into product_category values (1, 1)");

        MigrateResult upgrade = flyway("upgrade", "latest").migrate();

        assertThat(upgrade.migrationsExecuted).isEqualTo(1);
        Map<String, Object> notebooks = db.queryForMap("select name, quantity, version from product");
        assertThat(notebooks).containsEntry("name", "Notebooks").containsEntry("quantity", 25).containsEntry("version", 0L);
        assertThat(db.queryForObject("select count(*) from product_category", Integer.class)).isEqualTo(1);
    }
}
