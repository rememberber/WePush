package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.AccountAuthCircuit;
import com.fangxuele.wepush.next.service.domain.AccountAuthCircuitRepository;
import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class AccountAuthCircuitService {
    private final AccountAuthCircuitRepository circuits;
    private final JsonCodec json;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final int threshold;
    private final Duration window;
    private final Duration openDuration;

    public AccountAuthCircuitService(AccountAuthCircuitRepository circuits, JsonCodec json,
                                     TransactionRunner transactions, Clock clock, int threshold,
                                     Duration window, Duration openDuration) {
        this.circuits = circuits;
        this.json = json;
        this.transactions = transactions;
        this.clock = clock;
        if (threshold < 1 || window == null || window.isNegative() || window.isZero()
                || openDuration == null || openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("Account authentication circuit settings are invalid");
        }
        this.threshold = threshold;
        this.window = window;
        this.openDuration = openDuration;
    }

    public void requireClosed(WorkspaceId workspaceId, String accountId) {
        AccountAuthCircuit circuit = state(workspaceId, accountId);
        if (circuit.openAt(clock.instant())) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "ACCOUNT_AUTH_CIRCUIT_OPEN",
                    "Account authentication circuit is open until " + circuit.openUntil()
                            + "; fix credentials and reset it, or wait for the cooldown");
        }
    }

    public void record(WorkspaceId workspaceId, String runId, List<RunItemResultRecord> results) {
        if (results == null || results.stream().noneMatch(this::authenticationFailure)) return;
        String accountId = circuits.accountForRun(workspaceId, runId).orElse(null);
        if (accountId == null) return;
        transactions.required(() -> circuits.recordFailure(workspaceId, accountId, runId,
                clock.instant(), threshold, window, openDuration));
    }

    public AccountAuthCircuit state(WorkspaceId workspaceId, String accountId) {
        return circuits.find(workspaceId, accountId).orElseGet(() ->
                new AccountAuthCircuit(workspaceId, accountId, 0, null, null, null, "", 0));
    }

    public AccountAuthCircuit reset(WorkspaceId workspaceId, String accountId) {
        return transactions.required(() -> {
            circuits.reset(workspaceId, accountId);
            return state(workspaceId, accountId);
        });
    }

    private boolean authenticationFailure(RunItemResultRecord value) {
        Map<?, ?> metadata = json.read(value.metadata(), Map.class);
        return "AUTHENTICATION".equals(String.valueOf(metadata.get("wepush.errorCategory")));
    }
}
