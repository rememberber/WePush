package com.fangxuele.wepush.next.provider.spi;

import java.time.Duration;

public record ConnectionTestResult(boolean successful, String code, String diagnostic, Duration latency) {
    public ConnectionTestResult {
        code = code == null ? "" : code;
        diagnostic = diagnostic == null ? "" : diagnostic;
        if (latency == null || latency.isNegative()) {
            throw new IllegalArgumentException("latency must be non-negative");
        }
    }
}
