package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class ApplicationSupport {
    private ApplicationSupport() {
    }

    static void requireWorkspace(WorkspaceRepository workspaces, WorkspaceId workspaceId) {
        if (workspaces.findById(workspaceId).isEmpty()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "WORKSPACE_NOT_FOUND",
                    "Workspace was not found: " + workspaceId);
        }
    }

    static ProviderFactory requireProvider(ProviderRegistry providers, ProviderRef ref) {
        return providers.find(ref.providerId(), ref.implementationVersion())
                .orElseThrow(() -> new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE,
                        "PROVIDER_VERSION_UNAVAILABLE", "Provider version is not installed: " + ref));
    }

    static ConfigDocument config(com.fangxuele.wepush.next.service.domain.JsonDocument json,
                                 ConfigDocument schema) {
        return new ConfigDocument(schema.schemaId(), schema.schemaVersion(), schema.mediaType(),
                json.value().getBytes(StandardCharsets.UTF_8));
    }

    static void requireValid(ValidationResult validation) {
        if (validation.validResult()) {
            return;
        }
        throw new ApplicationProblem(ApplicationProblem.Kind.UNPROCESSABLE, "PROVIDER_CONFIG_INVALID",
                "Provider configuration is invalid", validation.violations().stream()
                .map(item -> new ApplicationProblem.FieldViolation(item.path(), item.code(), item.message()))
                .toList());
    }

    static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    static String text(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "FIELD_REQUIRED",
                    label + " must not be blank");
        }
        return value.trim();
    }
}
