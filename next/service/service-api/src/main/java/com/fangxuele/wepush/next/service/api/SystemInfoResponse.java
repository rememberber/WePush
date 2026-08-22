package com.fangxuele.wepush.next.service.api;

import java.time.Instant;

public record SystemInfoResponse(
        String product,
        String version,
        String mode,
        Instant serverTime
) {
}
