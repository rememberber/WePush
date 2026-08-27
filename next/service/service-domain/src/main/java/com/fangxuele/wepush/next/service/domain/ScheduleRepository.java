package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    void create(ScheduleDefinition schedule);

    Optional<ScheduleDefinition> findById(WorkspaceId workspaceId, String scheduleId);

    List<ScheduleDefinition> list(WorkspaceId workspaceId);

    List<ScheduleDefinition> page(WorkspaceId workspaceId, ResourcePageQuery query);

    List<ScheduleDefinition> listDue(Instant now, int limit);

    boolean advance(String scheduleId, long expectedVersion, Instant lastFireAt,
                    Instant nextFireAt, Instant updatedAt);

    boolean setEnabled(WorkspaceId workspaceId, String scheduleId, boolean enabled,
                       Instant nextFireAt, Instant updatedAt);

    boolean update(ScheduleDefinition schedule, long expectedVersion);

    boolean delete(WorkspaceId workspaceId, String scheduleId);
}
