package com.fangxuele.wepush.next.service.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DatabaseMigrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsTheCurrentSchemaWithValidOutboxAndWorkspaceBindingForeignKeys() {
        try (var dataSource = SQLiteDatabase.create(temporaryDirectory.resolve("migration.db"))) {
            Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration/sqlite")
                    .validateMigrationNaming(true).load().migrate();

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertTrue(columns(jdbc, "agent_enrollment_token").contains("workspace_id"));
            assertTrue(columns(jdbc, "api_principal").contains("system_role"));
            assertTrue(foreignKeys(jdbc, "agent_enrollment_token").contains("workspace"));
            var outboxForeignKeys = foreignKeys(jdbc, "agent_message_outbox");
            assertTrue(outboxForeignKeys.containsAll(
                    java.util.List.of("workspace", "run_instance", "agent_lease")));
        }
    }

    @Test
    void promotesAnExistingBootstrapPrincipalWhenUpgradingToVersionEleven() {
        try (var dataSource = SQLiteDatabase.create(temporaryDirectory.resolve("upgrade.db"))) {
            Flyway flyway = Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration/sqlite")
                    .target("10")
                    .validateMigrationNaming(true).load();
            flyway.migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("""
                    INSERT INTO api_principal(id, name, status, created_at)
                    VALUES ('principal_bootstrap', 'Bootstrap Administrator', 'ACTIVE',
                            '2026-08-22T00:00:00Z')
                    """);

            Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration/sqlite")
                    .validateMigrationNaming(true).load().migrate();

            assertEquals("SYSTEM_ADMIN", jdbc.queryForObject("""
                    SELECT system_role FROM api_principal WHERE id = 'principal_bootstrap'
                    """, String.class));
        }
    }

    private static java.util.List<String> columns(JdbcTemplate jdbc, String table) {
        return jdbc.query("PRAGMA table_info(" + table + ")", (row, ignored) -> row.getString("name"));
    }

    private static java.util.List<String> foreignKeys(JdbcTemplate jdbc, String table) {
        return jdbc.query("PRAGMA foreign_key_list(" + table + ")",
                (row, ignored) -> row.getString("table"));
    }
}
