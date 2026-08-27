package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AuditEventRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

public final class JdbcAuditEventRepository implements AuditEventRepository {
    private final JdbcTemplate jdbc;

    public JdbcAuditEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(AuditEvent value) {
        jdbc.update("""
                INSERT INTO audit_event(id, workspace_id, actor_type, actor_id, action,
                                        resource_type, resource_id, result, details_json, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.workspaceId(), value.actorType(), value.actorId(),
                value.action(), value.resourceType(), value.resourceId(), value.result(),
                value.details().value(), value.occurredAt().toString());
    }

    @Override
    public List<AuditEvent> list(String workspaceId, int limit) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("audit limit is invalid");
        return jdbc.query("""
                SELECT * FROM audit_event WHERE workspace_id = ? OR workspace_id IS NULL
                ORDER BY occurred_at DESC, id DESC LIMIT ?
                """, (rs, ignored) -> new AuditEvent(rs.getString("id"),
                rs.getString("workspace_id"), rs.getString("actor_type"),
                rs.getString("actor_id"), rs.getString("action"),
                rs.getString("resource_type"), rs.getString("resource_id"),
                rs.getString("result"), new JsonDocument(rs.getString("details_json")),
                Instant.parse(rs.getString("occurred_at"))), workspaceId, limit);
    }

    @Override
    public List<AuditEvent> page(String workspaceId, ResourcePageQuery query) {
        String select = "SELECT * FROM audit_event WHERE (workspace_id = ? OR workspace_id IS NULL)";
        StringBuilder sql = new StringBuilder(select);
        List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(workspaceId);
        if (query.name() != null) {
            sql.append(" AND LOWER(COALESCE(actor_id, '') || ' ' || COALESCE(action, '') || ' ' || "
                    + "COALESCE(resource_type, '') || ' ' || COALESCE(resource_id, '')) LIKE ?");
            parameters.add("%" + query.name().toLowerCase() + "%");
        }
        if (query.status() != null) {
            sql.append(" AND result = ?");
            parameters.add(query.status());
        }
        if (query.from() != null) { sql.append(" AND occurred_at >= ?"); parameters.add(query.from().toString()); }
        if (query.to() != null) { sql.append(" AND occurred_at <= ?"); parameters.add(query.to().toString()); }
        if (query.beforeCreatedAt() != null) {
            sql.append(" AND (occurred_at < ? OR (occurred_at = ? AND id < ?))");
            parameters.add(query.beforeCreatedAt().toString());
            parameters.add(query.beforeCreatedAt().toString());
            parameters.add(query.beforeId());
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ?");
        parameters.add(query.limit());
        return jdbc.query(sql.toString(), (rs, ignored) -> new AuditEvent(rs.getString("id"),
                rs.getString("workspace_id"), rs.getString("actor_type"), rs.getString("actor_id"),
                rs.getString("action"), rs.getString("resource_type"), rs.getString("resource_id"),
                rs.getString("result"), new JsonDocument(rs.getString("details_json")),
                Instant.parse(rs.getString("occurred_at"))), parameters.toArray());
    }
}
