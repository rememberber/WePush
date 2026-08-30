package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AccountAuthCircuit;
import com.fangxuele.wepush.next.service.domain.AccountAuthCircuitRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class JdbcAccountAuthCircuitRepository implements AccountAuthCircuitRepository {
    private final JdbcTemplate jdbc;

    public JdbcAccountAuthCircuitRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AccountAuthCircuit> find(WorkspaceId workspaceId, String accountId) {
        return jdbc.query("""
                SELECT * FROM account_auth_circuit WHERE workspace_id = ? AND account_id = ?
                """, this::row, workspaceId.value(), accountId).stream().findFirst();
    }

    @Override
    public Optional<String> accountForRun(WorkspaceId workspaceId, String runId) {
        return jdbc.query("""
                SELECT j.account_id FROM run_instance r JOIN job_definition j
                  ON j.id = r.job_id AND j.workspace_id = r.workspace_id
                WHERE r.workspace_id = ? AND r.id = ?
                """, (rs, ignored) -> rs.getString(1), workspaceId.value(), runId).stream().findFirst();
    }

    @Override
    public AccountAuthCircuit recordFailure(WorkspaceId workspaceId, String accountId, String runId,
                                            Instant now, int threshold, Duration window,
                                            Duration openDuration) {
        jdbc.update("""
                INSERT INTO account_auth_circuit
                (workspace_id, account_id, failure_runs, first_failure_at, last_failure_at,
                 open_until, last_run_id, version)
                VALUES (?, ?, 0, NULL, NULL, NULL, '', 0)
                ON CONFLICT (workspace_id, account_id) DO NOTHING
                """, workspaceId.value(), accountId);
        int inserted = jdbc.update("""
                INSERT INTO account_auth_failure_run(workspace_id, account_id, run_id, detected_at)
                VALUES (?, ?, ?, ?) ON CONFLICT (workspace_id, account_id, run_id) DO NOTHING
                """, workspaceId.value(), accountId, runId, now.toString());
        if (inserted == 0) return find(workspaceId, accountId).orElseGet(() -> empty(workspaceId, accountId));
        jdbc.update("""
                UPDATE account_auth_circuit SET version = version
                WHERE workspace_id = ? AND account_id = ?
                """, workspaceId.value(), accountId);
        AccountAuthCircuit current = find(workspaceId, accountId).orElseThrow();
        boolean outsideWindow = current.firstFailureAt() == null
                || current.firstFailureAt().plus(window).isBefore(now);
        int failures = outsideWindow ? 1 : current.failureRuns() + 1;
        Instant first = outsideWindow ? now : current.firstFailureAt();
        Instant openUntil = failures >= threshold ? now.plus(openDuration) : null;
        jdbc.update("""
                UPDATE account_auth_circuit
                SET failure_runs = ?, first_failure_at = ?, last_failure_at = ?, open_until = ?,
                    last_run_id = ?, version = version + 1
                WHERE workspace_id = ? AND account_id = ?
                """, failures, first.toString(), now.toString(), text(openUntil), runId,
                workspaceId.value(), accountId);
        return find(workspaceId, accountId).orElseThrow();
    }

    @Override
    public void reset(WorkspaceId workspaceId, String accountId) {
        jdbc.update("""
                DELETE FROM account_auth_failure_run WHERE workspace_id = ? AND account_id = ?
                """, workspaceId.value(), accountId);
        jdbc.update("""
                UPDATE account_auth_circuit
                SET failure_runs = 0, first_failure_at = NULL, last_failure_at = NULL,
                    open_until = NULL, last_run_id = '', version = version + 1
                WHERE workspace_id = ? AND account_id = ?
                """, workspaceId.value(), accountId);
    }

    private AccountAuthCircuit row(ResultSet rs, int ignored) throws SQLException {
        return new AccountAuthCircuit(new WorkspaceId(rs.getString("workspace_id")), rs.getString("account_id"),
                rs.getInt("failure_runs"), instant(rs.getString("first_failure_at")),
                instant(rs.getString("last_failure_at")), instant(rs.getString("open_until")),
                rs.getString("last_run_id"), rs.getLong("version"));
    }

    private static AccountAuthCircuit empty(WorkspaceId workspaceId, String accountId) {
        return new AccountAuthCircuit(workspaceId, accountId, 0, null, null, null, "", 0);
    }

    private static String text(Instant value) { return value == null ? null : value.toString(); }
    private static Instant instant(String value) { return value == null ? null : Instant.parse(value); }
}
