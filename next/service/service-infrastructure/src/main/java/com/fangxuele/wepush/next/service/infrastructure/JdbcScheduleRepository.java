package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.ScheduleDefinition;
import com.fangxuele.wepush.next.service.domain.ScheduleRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class JdbcScheduleRepository implements ScheduleRepository {
    private final JdbcTemplate jdbc;

    public JdbcScheduleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(ScheduleDefinition value) {
        jdbc.update("""
                INSERT INTO schedule_definition
                (id, workspace_id, job_id, name, cron_expression, timezone, misfire_policy,
                 enabled, next_fire_at, last_fire_at, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, value.id(), value.workspaceId().value(), value.jobId(), value.name(),
                value.cronExpression(), value.timezone(), value.misfirePolicy().name(),
                value.enabled() ? 1 : 0, value.nextFireAt().toString(), text(value.lastFireAt()),
                value.createdAt().toString(), value.updatedAt().toString(), value.version());
    }

    @Override
    public Optional<ScheduleDefinition> findById(WorkspaceId workspaceId, String scheduleId) {
        return jdbc.query("SELECT * FROM schedule_definition WHERE workspace_id = ? AND id = ?",
                JdbcScheduleRepository::row, workspaceId.value(), scheduleId).stream().findFirst();
    }

    @Override
    public List<ScheduleDefinition> list(WorkspaceId workspaceId) {
        return jdbc.query("""
                SELECT * FROM schedule_definition WHERE workspace_id = ?
                ORDER BY created_at DESC, id
                """, JdbcScheduleRepository::row, workspaceId.value());
    }

    @Override
    public List<ScheduleDefinition> listDue(Instant now, int limit) {
        return jdbc.query("""
                SELECT * FROM schedule_definition
                WHERE enabled = 1 AND next_fire_at <= ? ORDER BY next_fire_at, id LIMIT ?
                """, JdbcScheduleRepository::row, now.toString(), limit);
    }

    @Override
    public boolean advance(String scheduleId, long expectedVersion, Instant lastFireAt,
                           Instant nextFireAt, Instant updatedAt) {
        return jdbc.update("""
                UPDATE schedule_definition SET last_fire_at = ?, next_fire_at = ?,
                    updated_at = ?, version = version + 1
                WHERE id = ? AND version = ? AND enabled = 1
                """, lastFireAt.toString(), nextFireAt.toString(), updatedAt.toString(),
                scheduleId, expectedVersion) == 1;
    }

    @Override
    public boolean setEnabled(WorkspaceId workspaceId, String scheduleId, boolean enabled,
                              Instant nextFireAt, Instant updatedAt) {
        return jdbc.update("""
                UPDATE schedule_definition SET enabled = ?, next_fire_at = ?, updated_at = ?,
                    version = version + 1 WHERE workspace_id = ? AND id = ?
                """, enabled ? 1 : 0, nextFireAt.toString(), updatedAt.toString(),
                workspaceId.value(), scheduleId) == 1;
    }

    @Override
    public boolean delete(WorkspaceId workspaceId, String scheduleId) {
        return jdbc.update("DELETE FROM schedule_definition WHERE workspace_id = ? AND id = ?",
                workspaceId.value(), scheduleId) == 1;
    }

    private static ScheduleDefinition row(ResultSet rs, int ignored) throws SQLException {
        return new ScheduleDefinition(rs.getString("id"), new WorkspaceId(rs.getString("workspace_id")),
                rs.getString("job_id"), rs.getString("name"), rs.getString("cron_expression"),
                rs.getString("timezone"), ScheduleDefinition.MisfirePolicy.valueOf(
                rs.getString("misfire_policy")), rs.getBoolean("enabled"),
                Instant.parse(rs.getString("next_fire_at")), instant(rs.getString("last_fire_at")),
                Instant.parse(rs.getString("created_at")), Instant.parse(rs.getString("updated_at")),
                rs.getLong("version"));
    }

    private static String text(Instant value) {
        return value == null ? null : value.toString();
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
