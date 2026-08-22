package com.fangxuele.wepush.next.service.domain;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    void create(AccountDefinition account);

    Optional<AccountDefinition> findById(WorkspaceId workspaceId, String accountId);

    List<AccountDefinition> list(WorkspaceId workspaceId);
}
