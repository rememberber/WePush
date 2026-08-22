package com.fangxuele.wepush.next.service.app;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class DesktopCorsConfiguration implements WebMvcConfigurer {
    private static final String DESKTOP_ORIGIN = "wepush://app";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(DESKTOP_ORIGIN)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key", "Last-Event-ID")
                .exposedHeaders("Location", "ETag", "Retry-After")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
