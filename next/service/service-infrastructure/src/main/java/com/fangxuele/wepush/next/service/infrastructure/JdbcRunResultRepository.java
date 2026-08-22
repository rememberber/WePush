package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public final class JdbcRunResultRepository implements RunResultRepository {
    private static final String UPSERT = """
            INSERT INTO run_item_result
            (run_id, workspace_id, item_id, attempts, state, provider_code, diagnostic,
             external_request_id, completed_at, metadata_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (run_id, item_id) DO UPDATE SET
                attempts = excluded.attempts,
                state = excluded.state,
                provider_code = excluded.provider_code,
                diagnostic = excluded.diagnostic,
                external_request_id = excluded.external_request_id,
                completed_at = excluded.completed_at,
                metadata_json = excluded.metadata_json
            WHERE excluded.attempts > run_item_result.attempts
            """;

    private final JdbcTemplate jdbc;

    public JdbcRunResultRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(List<RunItemResultRecord> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(UPSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                RunItemResultRecord result = results.get(index);
                statement.setString(1, result.runId());
                statement.setString(2, result.workspaceId().value());
                statement.setString(3, result.itemId());
                statement.setInt(4, result.attempts());
                statement.setString(5, result.state().name());
                statement.setString(6, result.providerCode());
                statement.setString(7, result.diagnostic());
                statement.setString(8, result.externalRequestId());
                statement.setString(9, result.completedAt().toString());
                statement.setString(10, result.metadata().value());
            }

            @Override
            public int getBatchSize() {
                return results.size();
            }
        });
    }

    @Override
    public List<RunItemResultRecord> page(WorkspaceId workspaceId, String runId,
                                          Instant completedAfter, String itemIdAfter, int limit) {
        if (limit < 1 || limit > 501) {
            throw new IllegalArgumentException("result page limit must be between 1 and 501");
        }
        if (completedAfter == null) {
            return jdbc.query("""
                    SELECT * FROM run_item_result
                    WHERE workspace_id = ? AND run_id = ?
                    ORDER BY completed_at, item_id LIMIT ?
                    """, JdbcRows.RESULT, workspaceId.value(), runId, limit);
        }
        return jdbc.query("""
                SELECT * FROM run_item_result
                WHERE workspace_id = ? AND run_id = ?
                  AND (completed_at > ? OR (completed_at = ? AND item_id > ?))
                ORDER BY completed_at, item_id LIMIT ?
                """, JdbcRows.RESULT, workspaceId.value(), runId, completedAfter.toString(),
                completedAfter.toString(), itemIdAfter, limit);
    }
}
