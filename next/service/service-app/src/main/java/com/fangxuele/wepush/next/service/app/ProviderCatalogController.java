package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.service.api.ProviderSummaryResponse;
import com.fangxuele.wepush.next.service.application.ProviderCatalogQuery;
import com.fangxuele.wepush.next.service.application.ProviderView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/providers")
final class ProviderCatalogController {
    private final ProviderCatalogQuery query;

    ProviderCatalogController(ProviderCatalogQuery query) {
        this.query = query;
    }

    @GetMapping
    List<ProviderSummaryResponse> listProviders() {
        return query.list().stream().map(ProviderCatalogController::response).toList();
    }

    @GetMapping(
            value = "/{providerId}/versions/{version}/schemas/{schemaKind}",
            produces = "application/schema+json")
    ResponseEntity<byte[]> getSchema(
            @PathVariable String providerId,
            @PathVariable String version,
            @PathVariable String schemaKind
    ) {
        ProviderView provider = query.find(providerId, version)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ConfigDocument schema = switch (schemaKind.toLowerCase(Locale.ROOT)) {
            case "account" -> provider.accountSchema();
            case "message" -> provider.messageSchema();
            case "recipient" -> provider.recipientSchema();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/schema+json"))
                .body(schema.canonicalContent());
    }

    private static ProviderSummaryResponse response(ProviderView provider) {
        String base = "/api/v1/providers/%s/versions/%s/schemas/"
                .formatted(provider.providerId(), provider.implementationVersion());
        return new ProviderSummaryResponse(
                provider.providerId(),
                provider.displayName(),
                provider.implementationVersion(),
                provider.capabilities(),
                provider.maximumConcurrency(),
                new ProviderSummaryResponse.Links(
                        base + "account", base + "message", base + "recipient"));
    }
}
