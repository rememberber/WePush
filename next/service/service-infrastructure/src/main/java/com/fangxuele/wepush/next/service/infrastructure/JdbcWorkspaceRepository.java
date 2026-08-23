package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.Workspace;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.List;

public final class JdbcWorkspaceRepository implements WorkspaceRepository {
    private final JdbcTemplate jdbc;

    public JdbcWorkspaceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Workspace> findById(WorkspaceId workspaceId) {
        return jdbc.query("SELECT id AS workspace_id, name, status, created_at, version FROM workspace WHERE id = ?",
                JdbcRows.WORKSPACE, workspaceId.value()).stream().findFirst();
    }

    @Override
    public List<Workspace> list() {
        return jdbc.query("""
                SELECT id AS workspace_id, name, status, created_at, version
                FROM workspace ORDER BY created_at, id
                """, JdbcRows.WORKSPACE);
    }

    @Override
    public void create(Workspace value) {
        jdbc.update("INSERT INTO workspace(id, name, status, created_at, version) VALUES (?, ?, ?, ?, ?)",
                value.id().value(), value.name(), value.status().name(),
                value.createdAt().toString(), value.version());
    }

    @Override
    public void save(Workspace workspace, long expectedVersion) {
        int changed = jdbc.update("""
                UPDATE workspace SET name = ?, status = ?, version = version + 1
                WHERE id = ? AND version = ?
                """, workspace.name(), workspace.status().name(), workspace.id().value(), expectedVersion);
        if (changed != 1) {
            throw new IllegalStateException("workspace was concurrently modified: " + workspace.id());
        }
    }
}
