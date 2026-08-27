package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository {
    void append(AuditEvent event);

    List<AuditEvent> list(String workspaceId, int limit);

    List<AuditEvent> page(String workspaceId, ResourcePageQuery query);

    record AuditEvent(String id, String workspaceId, String actorType, String actorId,
                      String action, String resourceType, String resourceId, String result,
                      JsonDocument details, Instant occurredAt) {
    }
}
