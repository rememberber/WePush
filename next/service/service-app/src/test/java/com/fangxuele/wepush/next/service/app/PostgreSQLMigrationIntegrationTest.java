package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.infrastructure.PostgreSQLDatabase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class PostgreSQLMigrationIntegrationTest {
    @Test
    void migratesTheServerControlPlaneOnPostgreSql() throws Exception {
        String url = System.getenv("WEPUSH_TEST_POSTGRES_URL");
        assumeTrue(url != null && !url.isBlank(), "WEPUSH_TEST_POSTGRES_URL is not configured");
        String username = environment("WEPUSH_TEST_POSTGRES_USERNAME", "wepush");
        String password = environment("WEPUSH_TEST_POSTGRES_PASSWORD", "wepush-test");
        String schema = "wepush_test_" + UUID.randomUUID().toString().replace("-", "");

        try (var administration = PostgreSQLDatabase.create(url, username, password, 2)) {
            try (Connection connection = administration.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + schema);
            }
        }

        try {
            String schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            try (var dataSource = PostgreSQLDatabase.create(schemaUrl, username, password, 4)) {
                Flyway.configure().dataSource(dataSource).defaultSchema(schema).schemas(schema)
                        .locations("classpath:db/migration/sqlite")
                        .validateMigrationNaming(true).load().migrate();
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                assertEquals(1, jdbc.queryForObject(
                        "SELECT COUNT(*) FROM workspace WHERE id = 'ws_default'", Integer.class));
                assertEquals(1, jdbc.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = ? AND table_name = 'agent_enrollment_token'
                          AND column_name = 'workspace_id'
                        """, Integer.class, schema));
                assertEquals(1, jdbc.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = ? AND table_name = 'api_principal'
                          AND column_name = 'system_role'
                        """, Integer.class, schema));
                Integer foreignKeys = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.table_constraints
                        WHERE table_schema = ? AND table_name = 'agent_message_outbox'
                          AND constraint_type = 'FOREIGN KEY'
                        """, Integer.class, schema);
                assertTrue(foreignKeys != null && foreignKeys >= 3);
            }
        } finally {
            try (var administration = PostgreSQLDatabase.create(url, username, password, 2);
                 Connection connection = administration.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
