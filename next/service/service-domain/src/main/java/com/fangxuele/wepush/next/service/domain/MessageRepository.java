package com.fangxuele.wepush.next.service.domain;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {
    void create(MessageDefinition message);

    Optional<MessageDefinition> findById(WorkspaceId workspaceId, String messageId);

    List<MessageDefinition> list(WorkspaceId workspaceId);

    List<MessageDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query);

    boolean updateMetadata(MessageDefinition message, long expectedVersion);

    boolean createRevision(MessageDefinition message, long expectedVersion);

    Optional<MessageRevision> findRevision(WorkspaceId workspaceId, String messageId, int revision);

    List<MessageRevision> revisions(WorkspaceId workspaceId, String messageId,
                                    int beforeRevision, int limit);
}
