package com.fangxuele.wepush.next.service.api;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenApiContractTest {
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "patch", "options", "head", "trace");

    @Test
    void contractHasUniqueOperationsAndResolvableLocalReferences() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setCodePointLimit(4 * 1024 * 1024);
        Map<String, Object> document;
        try (InputStream source = getClass().getResourceAsStream("/openapi/openapi.yaml")) {
            assertNotNull(source, "OpenAPI resource must be packaged");
            document = new Yaml(new SafeConstructor(options)).load(source);
        } catch (java.io.IOException impossible) {
            throw new IllegalStateException(impossible);
        }

        assertEquals("3.1.0", document.get("openapi"));
        Map<String, Object> paths = map(document.get("paths"), "paths");
        assertTrue(!paths.isEmpty(), "OpenAPI paths must not be empty");
        Set<String> operationIds = new HashSet<>();
        paths.forEach((path, rawPathItem) -> {
            assertTrue(path.startsWith("/"), "API path must start with '/': " + path);
            Map<String, Object> pathItem = map(rawPathItem, path);
            pathItem.forEach((method, rawOperation) -> {
                if (!HTTP_METHODS.contains(method)) return;
                Map<String, Object> operation = map(rawOperation, method + " " + path);
                String operationId = String.valueOf(operation.getOrDefault("operationId", ""));
                assertTrue(!operationId.isBlank(), "operationId is required for " + method + " " + path);
                assertTrue(operationIds.add(operationId), "duplicate operationId: " + operationId);
                assertTrue(map(operation.get("responses"), operationId + ".responses").size() > 0,
                        "responses are required for " + operationId);
            });
        });
        walk(document, document);
    }

    private static void walk(Object value, Map<String, Object> root) {
        if (value instanceof Map<?, ?> values) {
            Object reference = values.get("$ref");
            if (reference != null) resolve(root, String.valueOf(reference));
            values.values().forEach(child -> walk(child, root));
        } else if (value instanceof List<?> values) {
            values.forEach(child -> walk(child, root));
        }
    }

    private static void resolve(Map<String, Object> root, String reference) {
        assertTrue(reference.startsWith("#/"), "only local OpenAPI references are allowed: " + reference);
        Object current = root;
        for (String token : reference.substring(2).split("/")) {
            String decoded = token.replace("~1", "/").replace("~0", "~");
            current = map(current, reference).get(decoded);
            assertNotNull(current, "unresolved OpenAPI reference: " + reference);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String label) {
        assertTrue(value instanceof Map<?, ?>, label + " must be an object");
        return (Map<String, Object>) value;
    }
}
