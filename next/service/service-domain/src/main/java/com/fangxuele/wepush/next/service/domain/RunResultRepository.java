package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface RunResultRepository {
    void append(List<RunItemResultRecord> results);

    List<RunItemResultRecord> page(WorkspaceId workspaceId, String runId,
                                   Instant completedAfter, String itemIdAfter, int limit);

    long countByStates(WorkspaceId workspaceId, String runId, Set<String> states);
}
