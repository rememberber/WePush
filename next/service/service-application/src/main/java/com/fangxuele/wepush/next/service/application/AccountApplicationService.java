package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ConnectionTestResult;
import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.time.Duration;

public final class AccountApplicationService {
    private final WorkspaceRepository workspaces;
    private final AccountRepository accounts;
    private final ProviderRegistry providers;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final SecretStore secrets;
    private final Clock clock;

    public AccountApplicationService(WorkspaceRepository workspaces, AccountRepository accounts,
                                     ProviderRegistry providers, JsonCodec json, ResourceIdGenerator ids,
                                     TransactionRunner transactions, SecretStore secrets, Clock clock) {
        this.workspaces = workspaces;
        this.accounts = accounts;
        this.providers = providers;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.secrets = secrets;
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

    public AccountDefinition update(WorkspaceId workspaceId, String accountId, UpdateAccount command) {
        return transactions.required(() -> {
            AccountDefinition current = get(workspaceId, accountId);
            ProviderFactory provider = ApplicationSupport.requireProvider(providers, current.provider());
            JsonDocument configuration = command.configuration() == null ? current.configuration()
                    : json.canonicalize(command.configuration());
            ApplicationSupport.requireValid(provider.validateAccount(
                    ApplicationSupport.config(configuration, provider.descriptor().accountSchema())));
            AccountDefinition.Status status = command.status() == null ? current.status()
                    : AccountDefinition.Status.valueOf(command.status().toUpperCase());
            AccountDefinition updated = new AccountDefinition(current.id(), workspaceId,
                    command.name() == null ? current.name() : ApplicationSupport.text(command.name(), "name"),
                    current.provider(), configuration, status, current.createdAt(), clock.instant(),
                    current.version() + 1);
            if (!accounts.update(updated, current.version())) conflict("Account", accountId);
            return updated;
        });
    }

    public ConnectionTestResult testConnection(WorkspaceId workspaceId, String accountId, Duration timeout) {
        AccountDefinition account = get(workspaceId, accountId);
        if (account.status() == AccountDefinition.Status.ARCHIVED) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "ACCOUNT_ARCHIVED",
                    "Archived Account cannot be tested");
        }
        Duration effective = timeout == null ? Duration.ofSeconds(10) : timeout;
        if (effective.isNegative() || effective.isZero() || effective.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_TEST_TIMEOUT",
                    "Connection test timeout must be between zero and 30 seconds");
        }
        ProviderFactory provider = ApplicationSupport.requireProvider(providers, account.provider());
        return provider.testConnection(
                ApplicationSupport.config(account.configuration(), provider.descriptor().accountSchema()),
                ref -> secrets.resolve(workspaceId, ref), effective);
    }

    private static void conflict(String type, String id) {
        throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RESOURCE_VERSION_CONFLICT",
                type + " was changed concurrently: " + id);
    }

    public record CreateAccount(String name, String providerId, String providerVersion, Object configuration) {
    }

    public record UpdateAccount(String name, Object configuration, String status) { }
}
