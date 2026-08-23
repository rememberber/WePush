package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record ScheduleDefinition(String id, WorkspaceId workspaceId, String jobId, String name,
                                 String cronExpression, String timezone, MisfirePolicy misfirePolicy,
                                 boolean enabled, Instant nextFireAt, Instant lastFireAt,
                                 Instant createdAt, Instant updatedAt, long version) {
    public ScheduleDefinition {
        id = DomainChecks.text(id, "schedule id");
        jobId = DomainChecks.text(jobId, "schedule job id");
        name = DomainChecks.text(name, "schedule name");
        cronExpression = DomainChecks.text(cronExpression, "schedule cron");
        timezone = DomainChecks.text(timezone, "schedule timezone");
        if (workspaceId == null || misfirePolicy == null || nextFireAt == null
                || createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt)
                || version < 0) {
            throw new IllegalArgumentException("schedule definition is incomplete");
        }
    }

    public enum MisfirePolicy { FIRE_ONCE, SKIP }
}
