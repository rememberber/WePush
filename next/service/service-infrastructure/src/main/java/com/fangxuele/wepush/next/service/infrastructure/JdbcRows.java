package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.ArtifactDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRecipient;
import com.fangxuele.wepush.next.service.domain.IdempotencyRecord;
import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunCommandRecord;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.RunSnapshot;
import com.fangxuele.wepush.next.service.domain.RunStatus;
import com.fangxuele.wepush.next.service.domain.Workspace;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class JdbcRows {
    static final RowMapper<Workspace> WORKSPACE = (rs, row) -> new Workspace(
            workspaceId(rs), rs.getString("name"), Workspace.Status.valueOf(rs.getString("status")),
            instant(rs, "created_at"), rs.getLong("version"));

    static final RowMapper<AccountDefinition> ACCOUNT = (rs, row) -> new AccountDefinition(
            rs.getString("id"), workspaceId(rs), rs.getString("name"), provider(rs),
            json(rs, "configuration_json"), AccountDefinition.Status.valueOf(rs.getString("status")),
            instant(rs, "created_at"), instant(rs, "updated_at"), rs.getLong("version"));

    static final RowMapper<MessageDefinition> MESSAGE = (rs, row) -> new MessageDefinition(
            rs.getString("id"), workspaceId(rs), rs.getString("name"), provider(rs),
            rs.getInt("current_revision"), rs.getString("schema_version"), json(rs, "content_json"),
            rs.getString("content_hash"), MessageDefinition.Status.valueOf(rs.getString("status")),
            instant(rs, "created_at"), instant(rs, "updated_at"), rs.getLong("version"));

    static final RowMapper<AudienceDefinition> AUDIENCE = (rs, row) -> new AudienceDefinition(
            rs.getString("id"), workspaceId(rs), rs.getString("name"), rs.getString("current_snapshot_id"),
            rs.getInt("current_revision"), rs.getLong("record_count"), rs.getString("content_hash"),
            AudienceDefinition.Status.valueOf(rs.getString("status")), instant(rs, "created_at"),
            instant(rs, "updated_at"), rs.getLong("version"));

    static final RowMapper<AudienceRecipient> RECIPIENT = (rs, row) -> new AudienceRecipient(
            rs.getLong("sequence"), rs.getString("item_id"), json(rs, "fields_json"));

    static final RowMapper<JobDefinition> JOB = (rs, row) -> new JobDefinition(
            rs.getString("id"), workspaceId(rs), rs.getString("name"), rs.getString("account_id"),
            rs.getString("message_id"), rs.getString("audience_id"), json(rs, "policies_json"),
            rs.getInt("enabled") != 0, rs.getInt("archived") != 0,
            instant(rs, "created_at"), instant(rs, "updated_at"),
            rs.getLong("version"));

    static final RowMapper<RunDefinition> RUN = (rs, row) -> new RunDefinition(
            rs.getString("id"), workspaceId(rs), rs.getString("job_id"),
            RunStatus.valueOf(rs.getString("status")), rs.getString("state_reason"),
            rs.getInt("dry_run") != 0, rs.getString("source_run_id"), rs.getString("retry_states"),
            rs.getLong("total"), rs.getLong("succeeded"),
            rs.getLong("failed"), rs.getLong("unknown_count"), rs.getLong("unsent"),
            rs.getLong("skipped"), rs.getLong("retried"), instant(rs, "created_at"),
            nullableInstant(rs, "started_at"), nullableInstant(rs, "ended_at"),
            instant(rs, "updated_at"), rs.getLong("version"));

    static final RowMapper<RunSnapshot> SNAPSHOT = (rs, row) -> new RunSnapshot(
            rs.getString("id"), rs.getString("run_id"), workspaceId(rs), provider(rs),
            json(rs, "account_configuration_json"), json(rs, "message_content_json"),
            json(rs, "policies_json"), rs.getString("audience_snapshot_id"),
            rs.getString("content_hash"));

    static final RowMapper<RunEventRecord> EVENT = (rs, row) -> new RunEventRecord(
            rs.getString("run_id"), workspaceId(rs), rs.getLong("sequence"), rs.getString("type"),
            instant(rs, "occurred_at"), json(rs, "payload_json"),
            RunEventRecord.Severity.valueOf(rs.getString("severity")));

    static final RowMapper<RunItemResultRecord> RESULT = (rs, row) -> new RunItemResultRecord(
            rs.getString("run_id"), workspaceId(rs), rs.getString("item_id"), rs.getInt("attempts"),
            ItemState.valueOf(rs.getString("state")), rs.getString("provider_code"),
            rs.getString("diagnostic"), rs.getString("external_request_id"),
            instant(rs, "completed_at"), json(rs, "metadata_json"));

    static final RowMapper<RunCommandRecord> COMMAND = (rs, row) -> new RunCommandRecord(
            rs.getString("id"), workspaceId(rs), rs.getString("run_id"), rs.getString("type"),
            json(rs, "payload_json"), RunCommandRecord.Status.valueOf(rs.getString("status")),
            rs.getString("result_code"), rs.getString("result_message"), instant(rs, "created_at"),
            nullableInstant(rs, "acknowledged_at"));

    static final RowMapper<IdempotencyRecord> IDEMPOTENCY = (rs, row) -> new IdempotencyRecord(
            workspaceId(rs), rs.getString("scope"), rs.getString("key_hash"),
            rs.getString("request_hash"), rs.getString("resource_id"), rs.getInt("response_status"),
            instant(rs, "created_at"), instant(rs, "expires_at"));

    static final RowMapper<ArtifactDefinition> ARTIFACT = (rs, row) -> new ArtifactDefinition(
            rs.getString("id"), workspaceId(rs), rs.getString("run_id"), rs.getString("type"),
            rs.getString("backend"), rs.getString("location"), rs.getString("original_name"),
            rs.getString("content_type"), rs.getLong("size"), rs.getString("sha256"),
            ArtifactDefinition.State.valueOf(rs.getString("state")), instant(rs, "expires_at"),
            rs.getInt("pinned") != 0, rs.getInt("legal_hold") != 0, instant(rs, "created_at"),
            nullableInstant(rs, "ready_at"), nullableInstant(rs, "deleted_at"),
            rs.getString("last_error"), rs.getLong("version"));

    private JdbcRows() {
    }

    private static WorkspaceId workspaceId(ResultSet rs) throws SQLException {
        return new WorkspaceId(rs.getString("workspace_id"));
    }

    private static ProviderRef provider(ResultSet rs) throws SQLException {
        return new ProviderRef(rs.getString("provider_id"), rs.getString("provider_version"));
    }

    private static JsonDocument json(ResultSet rs, String column) throws SQLException {
        return new JsonDocument(rs.getString(column));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return Instant.parse(rs.getString(column));
    }

    private static Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : Instant.parse(value);
    }
}
