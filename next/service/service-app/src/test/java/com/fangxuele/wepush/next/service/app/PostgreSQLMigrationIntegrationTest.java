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
                Flyway betaFlyway = Flyway.configure().dataSource(dataSource).defaultSchema(schema).schemas(schema)
                        .locations("classpath:db/migration/sqlite")
                        .target("13").validateMigrationNaming(true).load();
                betaFlyway.migrate();
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                jdbc.update("""
                        INSERT INTO workspace(id, name, status, created_at, version)
                        VALUES ('ws_beta_user', 'Beta PostgreSQL data', 'ACTIVE',
                                '2026-08-28T00:00:00Z', 9)
                        """);
                Flyway stableFlyway = Flyway.configure().dataSource(dataSource)
                        .defaultSchema(schema).schemas(schema)
                        .locations("classpath:db/migration/sqlite")
                        .validateMigrationNaming(true).load();
                stableFlyway.migrate();
                assertEquals("17", stableFlyway.info().current().getVersion().getVersion());
                assertEquals(1, jdbc.queryForObject(
                        "SELECT COUNT(*) FROM workspace WHERE id = 'ws_default'", Integer.class));
                assertEquals("Beta PostgreSQL data", jdbc.queryForObject(
                        "SELECT name FROM workspace WHERE id = 'ws_beta_user'", String.class));
                assertEquals(9, jdbc.queryForObject(
                        "SELECT version FROM workspace WHERE id = 'ws_beta_user'", Integer.class));
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
                assertEquals("1.x", jdbc.queryForObject("""
                        SELECT compatibility_line FROM wepush_release_compatibility WHERE id = 1
                        """, String.class));
                assertEquals("0.1.0-beta.1", jdbc.queryForObject("""
                        SELECT minimum_upgrade_version FROM wepush_release_compatibility WHERE id = 1
                        """, String.class));
                assertEquals(1, jdbc.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.tables
                        WHERE table_schema = ? AND table_name = 'workspace_policy'
                        """, Integer.class, schema));
                assertEquals(1, jdbc.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.tables
                        WHERE table_schema = ? AND table_name = 'account_auth_circuit'
                        """, Integer.class, schema));
                assertEquals(1, jdbc.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.tables
                        WHERE table_schema = ? AND table_name = 'artifact_multipart_upload'
                        """, Integer.class, schema));
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
