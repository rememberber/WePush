package com.fangxuele.wepush.next.service.app;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class OpenApiController {
    @GetMapping("/openapi.yaml")
    ResponseEntity<Resource> openApi() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(new ClassPathResource("openapi/openapi.yaml"));
    }
}
