package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRecipient;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
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
    public List<AudienceDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query) {
        JdbcPageQueries.Query page = JdbcPageQueries.build(SELECT, "d.workspace_id", workspaceId.value(),
                "d.name", "d.status", "d.created_at", "d.id", query);
        return jdbc.query(page.sql(), JdbcRows.AUDIENCE, page.parameters());
    }

    @Override
    public boolean updateMetadata(AudienceDefinition audience, long expectedVersion) {
        return jdbc.update("""
                UPDATE audience_definition SET name = ?, status = ?, updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ?
                """, audience.name(), audience.status().name(), audience.updatedAt().toString(),
                audience.workspaceId().value(), audience.id(), expectedVersion) == 1;
    }

    @Override
    public boolean createRevision(AudienceDefinition audience, List<AudienceRecipient> recipients,
                                  long expectedVersion) {
        jdbc.update("""
                INSERT INTO audience_snapshot
                (id, audience_id, workspace_id, revision, record_count, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, audience.snapshotId(), audience.id(), audience.workspaceId().value(), audience.revision(),
                audience.recordCount(), audience.contentHash(), audience.updatedAt().toString());
        insertRecipients(audience, recipients);
        return jdbc.update("""
                UPDATE audience_definition
                SET name = ?, current_snapshot_id = ?, current_revision = ?, status = ?,
                    updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ? AND current_revision = ?
                """, audience.name(), audience.snapshotId(), audience.revision(), audience.status().name(),
                audience.updatedAt().toString(), audience.workspaceId().value(), audience.id(), expectedVersion,
                audience.revision() - 1) == 1;
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

    @Override
    public List<AudienceRecipient> recipientsForRun(WorkspaceId workspaceId, String snapshotId, String runId) {
        return jdbc.query("""
                SELECT r.sequence, r.item_id, r.fields_json
                FROM audience_recipient r
                WHERE r.workspace_id = ? AND r.snapshot_id = ?
                  AND (NOT EXISTS (SELECT 1 FROM run_retry_item x WHERE x.run_id = ?)
                       OR EXISTS (SELECT 1 FROM run_retry_item x WHERE x.run_id = ? AND x.item_id = r.item_id))
                ORDER BY r.sequence
                """, JdbcRows.RECIPIENT, workspaceId.value(), snapshotId, runId, runId);
    }

    private void insertRecipients(AudienceDefinition audience, List<AudienceRecipient> recipients) {
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
}
