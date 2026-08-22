package com.fangxuele.wepush.next.service.domain;

import java.util.List;
import java.util.Optional;

public interface JobRepository {
    void create(JobDefinition job);

    Optional<JobDefinition> findById(WorkspaceId workspaceId, String jobId);

    List<JobDefinition> list(WorkspaceId workspaceId);
}
