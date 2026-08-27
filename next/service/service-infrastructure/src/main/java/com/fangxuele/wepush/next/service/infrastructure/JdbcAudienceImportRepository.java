package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceImportRepository;
import com.fangxuele.wepush.next.service.domain.AudienceImportRow;
import com.fangxuele.wepush.next.service.domain.AudienceImportSession;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class JdbcAudienceImportRepository implements AudienceImportRepository {
    private final JdbcTemplate jdbc;

    public JdbcAudienceImportRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void create(AudienceImportSession value) {
        jdbc.update("""
                INSERT INTO audience_import_session
                (id, workspace_id, audience_id, name, format, item_id_column, field_mapping_json, status,
                 total_rows, accepted_rows, rejected_rows, duplicate_rows, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.workspaceId().value(), value.audienceId(), value.name(), value.format(),
                value.itemIdColumn(), value.fieldMapping().value(), value.status().name(), value.totalRows(),
                value.acceptedRows(), value.rejectedRows(), value.duplicateRows(), value.createdAt().toString(),
                value.updatedAt().toString());
    }

    @Override
    public void append(String importId, WorkspaceId workspaceId, List<AudienceImportRow> rows) {
        jdbc.batchUpdate("""
                INSERT INTO audience_import_row
                (import_id, workspace_id, sequence, item_id, fields_json, raw_line,
                 accepted, error_code, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, rows, 250, (PreparedStatement statement, AudienceImportRow row) -> {
            statement.setString(1, importId);
            statement.setString(2, workspaceId.value());
            statement.setLong(3, row.sequence());
            statement.setString(4, row.itemId());
            statement.setString(5, row.fields().value());
            statement.setString(6, row.rawLine());
            statement.setInt(7, row.accepted() ? 1 : 0);
            statement.setString(8, row.errorCode());
            statement.setString(9, row.errorMessage());
        });
    }

    @Override
    public AudienceImportSession finalizePreview(String importId, WorkspaceId workspaceId) {
        jdbc.update("""
                UPDATE audience_import_row AS current
                SET accepted = 0, error_code = 'DUPLICATE_ITEM_ID',
                    error_message = 'Duplicate itemId; the first row was kept'
                WHERE current.workspace_id = ? AND current.import_id = ? AND current.accepted = 1
                  AND EXISTS (SELECT 1 FROM audience_import_row earlier
                              WHERE earlier.import_id = current.import_id
                                AND earlier.item_id = current.item_id
                                AND earlier.accepted = 1
                                AND earlier.sequence < current.sequence)
                """, workspaceId.value(), importId);
        Instant now = Instant.now();
        jdbc.update("""
                UPDATE audience_import_session
                SET status = 'PREVIEW_READY',
                    total_rows = (SELECT COUNT(*) FROM audience_import_row WHERE import_id = ?),
                    accepted_rows = (SELECT COUNT(*) FROM audience_import_row WHERE import_id = ? AND accepted = 1),
                    rejected_rows = (SELECT COUNT(*) FROM audience_import_row WHERE import_id = ? AND accepted = 0),
                    duplicate_rows = (SELECT COUNT(*) FROM audience_import_row
                                      WHERE import_id = ? AND error_code = 'DUPLICATE_ITEM_ID'),
                    updated_at = ?
                WHERE workspace_id = ? AND id = ?
                """, importId, importId, importId, importId, now.toString(), workspaceId.value(), importId);
        return findById(workspaceId, importId).orElseThrow();
    }

    @Override
    public Optional<AudienceImportSession> findById(WorkspaceId workspaceId, String importId) {
        return jdbc.query("SELECT * FROM audience_import_session WHERE workspace_id = ? AND id = ?",
                (rs, ignored) -> session(rs), workspaceId.value(), importId).stream().findFirst();
    }

    @Override
    public List<AudienceImportRow> rows(WorkspaceId workspaceId, String importId,
                                        Boolean accepted, long afterSequence, int limit) {
        String filter = accepted == null ? "" : " AND accepted = ?";
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(workspaceId.value()); parameters.add(importId); parameters.add(afterSequence);
        if (accepted != null) parameters.add(accepted ? 1 : 0);
        parameters.add(limit);
        return jdbc.query("SELECT * FROM audience_import_row WHERE workspace_id = ? AND import_id = ? "
                        + "AND sequence > ?" + filter + " ORDER BY sequence LIMIT ?",
                (rs, ignored) -> new AudienceImportRow(rs.getLong("sequence"), rs.getString("item_id"),
                        new JsonDocument(rs.getString("fields_json")), rs.getString("raw_line"),
                        rs.getInt("accepted") != 0, rs.getString("error_code"),
                        rs.getString("error_message")), parameters.toArray());
    }

    @Override
    public String acceptedContentHash(WorkspaceId workspaceId, String importId) {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        jdbc.query("""
                SELECT item_id, fields_json FROM audience_import_row
                WHERE workspace_id = ? AND import_id = ? AND accepted = 1 ORDER BY sequence
                """, rs -> {
            digest.update(rs.getString("item_id").getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(rs.getString("fields_json").getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }, workspaceId.value(), importId);
        return HexFormat.of().formatHex(digest.digest());
    }

    @Override
    public void commitNew(AudienceImportSession session, AudienceDefinition audience) {
        insertDefinition(audience);
        insertSnapshotAndRows(session, audience);
        markCommitted(session, audience);
    }

    @Override
    public boolean commitRevision(AudienceImportSession session, AudienceDefinition audience,
                                  long expectedVersion) {
        insertSnapshotAndRows(session, audience);
        int changed = jdbc.update("""
                UPDATE audience_definition
                SET name = ?, current_snapshot_id = ?, current_revision = ?, status = ?,
                    updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ? AND current_revision = ?
                """, audience.name(), audience.snapshotId(), audience.revision(), audience.status().name(),
                audience.updatedAt().toString(), audience.workspaceId().value(), audience.id(), expectedVersion,
                audience.revision() - 1);
        if (changed == 1) markCommitted(session, audience);
        return changed == 1;
    }

    private void insertDefinition(AudienceDefinition value) {
        jdbc.update("""
                INSERT INTO audience_definition
                (id, workspace_id, name, current_snapshot_id, current_revision, status,
                 created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.workspaceId().value(), value.name(), value.snapshotId(), value.revision(),
                value.status().name(), value.createdAt().toString(), value.updatedAt().toString(), value.version());
    }

    private void insertSnapshotAndRows(AudienceImportSession session, AudienceDefinition audience) {
        jdbc.update("""
                INSERT INTO audience_snapshot
                (id, audience_id, workspace_id, revision, record_count, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, audience.snapshotId(), audience.id(), audience.workspaceId().value(), audience.revision(),
                audience.recordCount(), audience.contentHash(), audience.updatedAt().toString());
        jdbc.update("""
                INSERT INTO audience_recipient(snapshot_id, workspace_id, sequence, item_id, fields_json)
                SELECT ?, workspace_id,
                       ROW_NUMBER() OVER (ORDER BY sequence) - 1,
                       item_id, fields_json
                FROM audience_import_row
                WHERE workspace_id = ? AND import_id = ? AND accepted = 1
                ORDER BY sequence
                """, audience.snapshotId(), audience.workspaceId().value(), session.id());
    }

    private void markCommitted(AudienceImportSession session, AudienceDefinition audience) {
        jdbc.update("""
                UPDATE audience_import_session SET status = 'COMMITTED', audience_id = ?, updated_at = ?
                WHERE workspace_id = ? AND id = ? AND status = 'PREVIEW_READY'
                """, audience.id(), audience.updatedAt().toString(), audience.workspaceId().value(), session.id());
    }

    private static AudienceImportSession session(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AudienceImportSession(rs.getString("id"), new WorkspaceId(rs.getString("workspace_id")),
                rs.getString("audience_id"), rs.getString("name"), rs.getString("format"),
                rs.getString("item_id_column"), new JsonDocument(rs.getString("field_mapping_json")),
                AudienceImportSession.Status.valueOf(rs.getString("status")), rs.getLong("total_rows"),
                rs.getLong("accepted_rows"), rs.getLong("rejected_rows"), rs.getLong("duplicate_rows"),
                Instant.parse(rs.getString("created_at")), Instant.parse(rs.getString("updated_at")));
    }
}
