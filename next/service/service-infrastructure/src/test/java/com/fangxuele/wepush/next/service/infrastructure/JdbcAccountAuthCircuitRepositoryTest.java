package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JdbcAccountAuthCircuitRepositoryTest {
    @TempDir Path temporary;

    @Test
    void countsAuthenticationFailuresOncePerRunOpensAndResetsCircuit() {
        try (var dataSource = SQLiteDatabase.create(temporary.resolve("circuit.db"))) {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/sqlite")
                    .validateMigrationNaming(true).load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            fixture(jdbc);
            var repository = new JdbcAccountAuthCircuitRepository(jdbc);
            WorkspaceId workspace = new WorkspaceId("ws_default");
            Instant now = Instant.parse("2026-08-29T00:00:00Z");

            assertEquals("account_1", repository.accountForRun(workspace, "run_1").orElseThrow());
            repository.recordFailure(workspace, "account_1", "run_1", now, 3,
                    Duration.ofMinutes(15), Duration.ofMinutes(15));
            var duplicate = repository.recordFailure(workspace, "account_1", "run_1", now.plusSeconds(1), 3,
                    Duration.ofMinutes(15), Duration.ofMinutes(15));
            assertEquals(1, duplicate.failureRuns());
            repository.recordFailure(workspace, "account_1", "run_2", now.plusSeconds(2), 3,
                    Duration.ofMinutes(15), Duration.ofMinutes(15));
            var open = repository.recordFailure(workspace, "account_1", "run_3", now.plusSeconds(3), 3,
                    Duration.ofMinutes(15), Duration.ofMinutes(15));
            assertEquals(3, open.failureRuns());
            assertTrue(open.openAt(now.plusSeconds(4)));

            repository.reset(workspace, "account_1");
            var reset = repository.find(workspace, "account_1").orElseThrow();
            assertEquals(0, reset.failureRuns());
            assertFalse(reset.openAt(now));
        }
    }

    private static void fixture(JdbcTemplate jdbc) {
        String now = "2026-08-29T00:00:00Z";
        jdbc.update("""
                INSERT INTO account_definition(id, workspace_id, name, provider_id, provider_version,
                  configuration_json, status, created_at, updated_at, version)
                VALUES ('account_1','ws_default','Account','wepush.http','0.1.0','{}','ACTIVE',?,?,0)
                """, now, now);
        jdbc.update("""
                INSERT INTO message_definition(id, workspace_id, name, provider_id, provider_version,
                  current_revision, status, created_at, updated_at, version)
                VALUES ('message_1','ws_default','Message','wepush.http','0.1.0',1,'ACTIVE',?,?,0)
                """, now, now);
        jdbc.update("""
                INSERT INTO audience_definition(id, workspace_id, name, current_snapshot_id,
                  current_revision, status, created_at, updated_at, version)
                VALUES ('audience_1','ws_default','Audience','snapshot_1',1,'ACTIVE',?,?,0)
                """, now, now);
        jdbc.update("""
                INSERT INTO job_definition(id, workspace_id, name, account_id, message_id, audience_id,
                  policies_json, enabled, created_at, updated_at, version)
                VALUES ('job_1','ws_default','Job','account_1','message_1','audience_1','{}',1,?,?,0)
                """, now, now);
        for (int index = 1; index <= 3; index++) {
            jdbc.update("""
                    INSERT INTO run_instance(id, workspace_id, job_id, status, state_reason, dry_run,
                      created_at, updated_at, version)
                    VALUES (?, 'ws_default','job_1','FAILED','test',0,?,?,0)
                    """, "run_" + index, now, now);
        }
    }
}
