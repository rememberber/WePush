package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.util.Arrays;

public final class SecretApplicationService {
    private final WorkspaceRepository workspaces;
    private final SecretStore secrets;
    private final TransactionRunner transactions;

    public SecretApplicationService(WorkspaceRepository workspaces, SecretStore secrets,
                                    TransactionRunner transactions) {
        this.workspaces = workspaces;
        this.secrets = secrets;
        this.transactions = transactions;
    }

    public SecretMetadata replace(WorkspaceId workspaceId, SecretRef ref, char[] value) {
        if (value == null || value.length == 0) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "SECRET_VALUE_REQUIRED",
                    "Secret value must not be empty");
        }
        char[] owned = value.clone();
        try {
            return transactions.required(() -> {
                ApplicationSupport.requireWorkspace(workspaces, workspaceId);
                return secrets.put(workspaceId, ref, owned);
            });
        } finally {
            Arrays.fill(owned, '\0');
            Arrays.fill(value, '\0');
        }
    }

    public SecretMetadata metadata(WorkspaceId workspaceId, SecretRef ref) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return secrets.metadata(workspaceId, ref).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "SECRET_NOT_FOUND",
                        "Secret is not configured"));
    }
}
