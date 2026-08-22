package com.fangxuele.wepush.next.service.domain;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {
    void create(MessageDefinition message);

    Optional<MessageDefinition> findById(WorkspaceId workspaceId, String messageId);

    List<MessageDefinition> list(WorkspaceId workspaceId);
}
