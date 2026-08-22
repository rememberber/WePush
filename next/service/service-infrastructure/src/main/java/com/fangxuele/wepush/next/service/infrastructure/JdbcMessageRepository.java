package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
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
}
