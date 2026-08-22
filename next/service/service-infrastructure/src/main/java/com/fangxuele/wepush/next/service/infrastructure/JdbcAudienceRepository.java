package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRecipient;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

public final class JdbcAudienceRepository implements AudienceRepository {
    private static final String SELECT = """
            SELECT d.*, s.record_count, s.content_hash
            FROM audience_definition d
            JOIN audience_snapshot s ON s.id = d.current_snapshot_id
            """;

    private final JdbcTemplate jdbc;

    public JdbcAudienceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(AudienceDefinition audience, List<AudienceRecipient> recipients) {
        jdbc.update("""
                INSERT INTO audience_definition
                (id, workspace_id, name, current_snapshot_id, current_revision, status,
                 created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, audience.id(), audience.workspaceId().value(), audience.name(), audience.snapshotId(),
                audience.revision(), audience.status().name(), audience.createdAt().toString(),
                audience.updatedAt().toString(), audience.version());
        jdbc.update("""
                INSERT INTO audience_snapshot
                (id, audience_id, workspace_id, revision, record_count, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, audience.snapshotId(), audience.id(), audience.workspaceId().value(), audience.revision(),
                audience.recordCount(), audience.contentHash(), audience.createdAt().toString());
        jdbc.batchUpdate("""
                INSERT INTO audience_recipient
                (snapshot_id, workspace_id, sequence, item_id, fields_json)
                VALUES (?, ?, ?, ?, ?)
                """, recipients, 250, (PreparedStatement statement, AudienceRecipient recipient) -> {
            statement.setString(1, audience.snapshotId());
            statement.setString(2, audience.workspaceId().value());
            statement.setLong(3, recipient.sequence());
            statement.setString(4, recipient.itemId());
            statement.setString(5, recipient.fields().value());
        });
    }

    @Override
    public Optional<AudienceDefinition> findById(WorkspaceId workspaceId, String audienceId) {
        return jdbc.query(SELECT + " WHERE d.workspace_id = ? AND d.id = ?", JdbcRows.AUDIENCE,
                workspaceId.value(), audienceId).stream().findFirst();
    }

    @Override
    public List<AudienceDefinition> list(WorkspaceId workspaceId) {
        return jdbc.query(SELECT + " WHERE d.workspace_id = ? ORDER BY d.created_at DESC, d.id",
                JdbcRows.AUDIENCE, workspaceId.value());
    }

    @Override
    public List<AudienceRecipient> recipients(WorkspaceId workspaceId, String snapshotId) {
        return jdbc.query("""
                SELECT sequence, item_id, fields_json
                FROM audience_recipient
                WHERE workspace_id = ? AND snapshot_id = ?
                ORDER BY sequence
                """, JdbcRows.RECIPIENT, workspaceId.value(), snapshotId);
    }
}
