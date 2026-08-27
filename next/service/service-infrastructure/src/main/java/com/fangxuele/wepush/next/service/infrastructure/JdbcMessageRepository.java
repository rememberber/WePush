package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.MessageRevision;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

public final class JdbcMessageRepository implements MessageRepository {
    private static final String SELECT = """
            SELECT d.*, r.schema_version, r.content_json, r.content_hash
            FROM message_definition d
            JOIN message_revision r ON r.message_id = d.id AND r.revision = d.current_revision
            """;

    private final JdbcTemplate jdbc;

    public JdbcMessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(MessageDefinition message) {
        jdbc.update("""
                INSERT INTO message_definition
                (id, workspace_id, name, provider_id, provider_version, current_revision, status,
                 created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, message.id(), message.workspaceId().value(), message.name(), message.provider().providerId(),
                message.provider().implementationVersion(), message.revision(), message.status().name(),
                message.createdAt().toString(), message.updatedAt().toString(), message.version());
        jdbc.update("""
                INSERT INTO message_revision
                (message_id, workspace_id, revision, schema_version, content_json, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, message.id(), message.workspaceId().value(), message.revision(), message.schemaVersion(),
                message.content().value(), message.contentHash(), message.createdAt().toString());
    }

    @Override
    public Optional<MessageDefinition> findById(WorkspaceId workspaceId, String messageId) {
        return jdbc.query(SELECT + " WHERE d.workspace_id = ? AND d.id = ?", JdbcRows.MESSAGE,
                workspaceId.value(), messageId).stream().findFirst();
    }

    @Override
    public List<MessageDefinition> list(WorkspaceId workspaceId) {
        return jdbc.query(SELECT + " WHERE d.workspace_id = ? ORDER BY d.created_at DESC, d.id",
                JdbcRows.MESSAGE, workspaceId.value());
    }

    @Override
    public List<MessageDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query) {
        JdbcPageQueries.Query page = JdbcPageQueries.build(SELECT, "d.workspace_id", workspaceId.value(),
                "d.name", "d.status", "d.created_at", "d.id", query);
        return jdbc.query(page.sql(), JdbcRows.MESSAGE, page.parameters());
    }

    @Override
    public boolean updateMetadata(MessageDefinition message, long expectedVersion) {
        return jdbc.update("""
                UPDATE message_definition SET name = ?, status = ?, updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ?
                """, message.name(), message.status().name(), message.updatedAt().toString(),
                message.workspaceId().value(), message.id(), expectedVersion) == 1;
    }

    @Override
    public boolean createRevision(MessageDefinition message, long expectedVersion) {
        int changed = jdbc.update("""
                UPDATE message_definition
                SET name = ?, current_revision = ?, status = ?, updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ? AND current_revision = ?
                """, message.name(), message.revision(), message.status().name(), message.updatedAt().toString(),
                message.workspaceId().value(), message.id(), expectedVersion, message.revision() - 1);
        if (changed != 1) return false;
        jdbc.update("""
                INSERT INTO message_revision
                (message_id, workspace_id, revision, schema_version, content_json, content_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, message.id(), message.workspaceId().value(), message.revision(), message.schemaVersion(),
                message.content().value(), message.contentHash(), message.updatedAt().toString());
        return true;
    }

    @Override
    public Optional<MessageRevision> findRevision(WorkspaceId workspaceId, String messageId, int revision) {
        return jdbc.query("""
                SELECT * FROM message_revision
                WHERE workspace_id = ? AND message_id = ? AND revision = ?
                """, (rs, ignored) -> revision(rs), workspaceId.value(), messageId, revision)
                .stream().findFirst();
    }

    @Override
    public List<MessageRevision> revisions(WorkspaceId workspaceId, String messageId,
                                           int beforeRevision, int limit) {
        return jdbc.query("""
                SELECT * FROM message_revision
                WHERE workspace_id = ? AND message_id = ? AND revision < ?
                ORDER BY revision DESC LIMIT ?
                """, (rs, ignored) -> revision(rs), workspaceId.value(), messageId,
                beforeRevision <= 0 ? Integer.MAX_VALUE : beforeRevision, limit);
    }

    private static MessageRevision revision(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MessageRevision(rs.getString("message_id"), new WorkspaceId(rs.getString("workspace_id")),
                rs.getInt("revision"), rs.getString("schema_version"),
                new JsonDocument(rs.getString("content_json")), rs.getString("content_hash"),
                java.time.Instant.parse(rs.getString("created_at")));
    }
}
