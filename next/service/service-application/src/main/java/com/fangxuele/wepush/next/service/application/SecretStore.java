package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.util.Optional;

public interface SecretStore {
    SecretMetadata put(WorkspaceId workspaceId, SecretRef ref, char[] value);

    Optional<SecretMetadata> metadata(WorkspaceId workspaceId, SecretRef ref);

    SecretValue resolve(WorkspaceId workspaceId, SecretRef ref);
}
