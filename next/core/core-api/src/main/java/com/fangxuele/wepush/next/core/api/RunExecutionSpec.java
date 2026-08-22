package com.fangxuele.wepush.next.core.api;

import java.time.Instant;
import java.util.Map;

public record RunExecutionSpec(
        String runId,
        ProviderRef provider,
        ConfigDocument accountConfig,
        ConfigDocument messageConfig,
        ExecutionPolicies policies,
        Map<String, String> attributes,
        boolean dryRun,
        Instant createdAt
) {
    public RunExecutionSpec {
        runId = ApiChecks.notBlank(runId, "runId");
        if (provider == null || accountConfig == null || messageConfig == null
                || policies == null || createdAt == null) {
            throw new IllegalArgumentException("run specification must not contain null values");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
