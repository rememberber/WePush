package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

public final class JdbcAccountRepository implements AccountRepository {
    private final JdbcTemplate jdbc;

    public JdbcAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void create(AccountDefinition account) {
        jdbc.update("""
                INSERT INTO account_definition
                (id, workspace_id, name, provider_id, provider_version, configuration_json, status,
                 created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, account.id(), account.workspaceId().value(), account.name(), account.provider().providerId(),
                account.provider().implementationVersion(), account.configuration().value(), account.status().name(),
                account.createdAt().toString(), account.updatedAt().toString(), account.version());
    }

    @Override
    public Optional<AccountDefinition> findById(WorkspaceId workspaceId, String accountId) {
        return jdbc.query("SELECT * FROM account_definition WHERE workspace_id = ? AND id = ?",
                JdbcRows.ACCOUNT, workspaceId.value(), accountId).stream().findFirst();
    }

    @Override
    public List<AccountDefinition> list(WorkspaceId workspaceId) {
        return jdbc.query("SELECT * FROM account_definition WHERE workspace_id = ? ORDER BY created_at DESC, id",
                JdbcRows.ACCOUNT, workspaceId.value());
    }

    @Override
    public List<AccountDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query) {
        JdbcPageQueries.Query page = JdbcPageQueries.build("SELECT * FROM account_definition",
                "workspace_id", workspaceId.value(), "name", "status", "created_at", "id", query);
        return jdbc.query(page.sql(), JdbcRows.ACCOUNT, page.parameters());
    }

    @Override
    public boolean update(AccountDefinition account, long expectedVersion) {
        return jdbc.update("""
                UPDATE account_definition
                SET name = ?, configuration_json = ?, status = ?, updated_at = ?, version = version + 1
                WHERE workspace_id = ? AND id = ? AND version = ?
                """, account.name(), account.configuration().value(), account.status().name(),
                account.updatedAt().toString(), account.workspaceId().value(), account.id(), expectedVersion) == 1;
    }
}
