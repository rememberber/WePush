package com.fangxuele.wepush.next.provider.spi;

import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.SecretResolver;

public record ProviderOpenContext(
        RunExecutionSpec spec,
        SecretResolver secretResolver,
        ExecutionClock clock
) {
    public ProviderOpenContext {
        if (spec == null || secretResolver == null || clock == null) {
            throw new IllegalArgumentException("provider open context is incomplete");
        }
    }
}
