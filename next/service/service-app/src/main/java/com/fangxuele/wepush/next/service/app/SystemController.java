package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.SystemInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
final class SystemController {
    private final String mode;

    SystemController(@Value("${wepush.mode:standalone}") String mode) {
        this.mode = mode;
    }

    @GetMapping("/info")
    SystemInfoResponse info() {
        return new SystemInfoResponse("WePush Next", "0.1.0-SNAPSHOT", mode, Instant.now());
    }
}
