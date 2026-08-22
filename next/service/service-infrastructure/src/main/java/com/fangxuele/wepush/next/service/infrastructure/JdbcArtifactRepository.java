package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.ArtifactDefinition;
import com.fangxuele.wepush.next.service.domain.ArtifactRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcArtifactRepository implements ArtifactRepository {
    private final JdbcTemplate jdbc;

    public JdbcArtifactRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(ArtifactDefinition artifact) {
        jdbc.update("""
                INSERT INTO artifact_record
                (id, workspace_id, run_id, type, backend, location, original_name, content_type,
                 size, sha256, state, expires_at, pinned, legal_hold, created_at, ready_at,
                 deleted_at, last_error, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, artifact.id(), artifact.workspaceId().value(), artifact.runId(), artifact.type(),
                artifact.backend(), artifact.location(), artifact.originalName(), artifact.contentType(),
                artifact.size(), artifact.sha256(), artifact.state().name(), text(artifact.expiresAt()),
                artifact.pinned() ? 1 : 0, artifact.legalHold() ? 1 : 0, text(artifact.createdAt()),
                text(artifact.readyAt()), text(artifact.deletedAt()), artifact.lastError(), artifact.version());
    }

    @Override
    public Optional<ArtifactDefinition> findById(WorkspaceId workspaceId, String artifactId) {
        return jdbc.query("SELECT * FROM artifact_record WHERE workspace_id = ? AND id = ?",
                JdbcRows.ARTIFACT, workspaceId.value(), artifactId).stream().findFirst();
    }

    @Override
    public Optional<ArtifactDefinition> findReadyByRunAndType(
            WorkspaceId workspaceId, String runId, String type) {
        return jdbc.query("""
                SELECT * FROM artifact_record
                WHERE workspace_id = ? AND run_id = ? AND type = ? AND state = 'READY'
                ORDER BY created_at DESC, id LIMIT 1
                """, JdbcRows.ARTIFACT, workspaceId.value(), runId, type).stream().findFirst();
    }

    @Override
    public List<ArtifactDefinition> listForRun(WorkspaceId workspaceId, String runId) {
        return jdbc.query("""
                SELECT * FROM artifact_record
                WHERE workspace_id = ? AND run_id = ? AND state != 'DELETED'
                ORDER BY created_at DESC, id
                """, JdbcRows.ARTIFACT, workspaceId.value(), runId);
    }

    @Override
    public void markReady(WorkspaceId workspaceId, String artifactId, long size,
                          String sha256, Instant readyAt) {
        int changed = jdbc.update("""
                UPDATE artifact_record
                SET size = ?, sha256 = ?, state = 'READY', ready_at = ?, last_error = '', version = version + 1
                WHERE workspace_id = ? AND id = ? AND state = 'UPLOADING'
                """, size, sha256, text(readyAt), workspaceId.value(), artifactId);
        if (changed != 1) {
            throw new IllegalStateException("artifact cannot become ready: " + artifactId);
        }
    }

    @Override
    public void markFailed(WorkspaceId workspaceId, String artifactId, String error) {
        jdbc.update("""
                UPDATE artifact_record
                SET state = 'FAILED', last_error = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND state IN ('UPLOADING', 'DELETING')
                """, error == null ? "artifact operation failed" : error, workspaceId.value(), artifactId);
    }

    @Override
    public List<ArtifactDefinition> claimExpired(Instant now, int limit) {
        if (limit < 1 || limit > 1_000) {
            throw new IllegalArgumentException("retention limit must be between 1 and 1000");
        }
        List<ArtifactDefinition> candidates = jdbc.query("""
                SELECT * FROM artifact_record
                WHERE state IN ('READY', 'FAILED') AND expires_at <= ? AND pinned = 0 AND legal_hold = 0
                ORDER BY expires_at, id LIMIT ?
                """, JdbcRows.ARTIFACT, text(now), limit);
        List<ArtifactDefinition> claimed = new ArrayList<>();
        for (ArtifactDefinition candidate : candidates) {
            int changed = jdbc.update("""
                    UPDATE artifact_record SET state = 'DELETING', version = version + 1
                    WHERE id = ? AND workspace_id = ? AND state = ?
                      AND expires_at <= ? AND pinned = 0 AND legal_hold = 0
                    """, candidate.id(), candidate.workspaceId().value(), candidate.state().name(), text(now));
            if (changed == 1) {
                claimed.add(candidate);
            }
        }
        return List.copyOf(claimed);
    }

    @Override
    public void markDeleted(WorkspaceId workspaceId, String artifactId, Instant deletedAt) {
        int changed = jdbc.update("""
                UPDATE artifact_record
                SET state = 'DELETED', deleted_at = ?, last_error = '', version = version + 1
                WHERE workspace_id = ? AND id = ? AND state = 'DELETING'
                """, text(deletedAt), workspaceId.value(), artifactId);
        if (changed != 1) {
            throw new IllegalStateException("artifact cannot become deleted: " + artifactId);
        }
    }

    private static String text(Instant value) {
        return value == null ? null : value.toString();
    }
}
