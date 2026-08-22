package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class AccountApplicationService {
    private final WorkspaceRepository workspaces;
    private final AccountRepository accounts;
    private final ProviderRegistry providers;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public AccountApplicationService(WorkspaceRepository workspaces, AccountRepository accounts,
                                     ProviderRegistry providers, JsonCodec json, ResourceIdGenerator ids,
                                     TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.accounts = accounts;
        this.providers = providers;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public AccountDefinition create(WorkspaceId workspaceId, CreateAccount command) {
        return transactions.required(() -> {
            ApplicationSupport.requireWorkspace(workspaces, workspaceId);
            ProviderRef ref = new ProviderRef(ApplicationSupport.text(command.providerId(), "providerId"),
                    ApplicationSupport.text(command.providerVersion(), "providerVersion"));
            ProviderFactory provider = ApplicationSupport.requireProvider(providers, ref);
            JsonDocument configuration = json.canonicalize(command.configuration());
            ApplicationSupport.requireValid(provider.validateAccount(
                    ApplicationSupport.config(configuration, provider.descriptor().accountSchema())));
            Instant now = clock.instant();
            AccountDefinition account = new AccountDefinition(ids.next("acct"), workspaceId,
                    ApplicationSupport.text(command.name(), "name"), ref, configuration,
                    AccountDefinition.Status.ACTIVE, now, now, 0);
            accounts.create(account);
            return account;
        });
    }

    public AccountDefinition get(WorkspaceId workspaceId, String accountId) {
        return accounts.findById(workspaceId, accountId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account was not found: " + accountId));
    }

    public List<AccountDefinition> list(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return accounts.list(workspaceId);
    }

    public record CreateAccount(String name, String providerId, String providerVersion, Object configuration) {
    }
}
