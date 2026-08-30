package com.fangxuele.wepush.next.service.api;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenApiContractTest {
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "patch", "options", "head", "trace");

    @Test
    void contractHasUniqueOperationsAndResolvableLocalReferences() {
        Map<String, Object> document = load("/openapi/openapi.yaml");

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

    @Test
    void preservesTheStableOneDotZeroContractWhileAllowingCompatibleAdditions() {
        Map<String, Object> baseline = load("/compatibility/1.0.0-openapi.yaml");
        Map<String, Object> current = load("/openapi/openapi.yaml");
        assertCompatible(baseline, current, "openapi");
        assertNoNewRequiredOperationInputs(baseline, current);
    }

    private static Map<String, Object> load(String resource) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setCodePointLimit(4 * 1024 * 1024);
        try (InputStream source = OpenApiContractTest.class.getResourceAsStream(resource)) {
            assertNotNull(source, "OpenAPI resource must be packaged: " + resource);
            return new Yaml(new SafeConstructor(options)).load(source);
        } catch (java.io.IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void assertCompatible(Object baseline, Object current, String path) {
        if (baseline instanceof Map<?, ?> baselineMap) {
            assertTrue(current instanceof Map<?, ?>, "Stable object was removed or changed: " + path);
            Map<?, ?> currentMap = (Map<?, ?>) current;
            baselineMap.forEach((key, expected) -> {
                String child = path + "." + key;
                if ("openapi.info.version".equals(child)) return;
                assertTrue(currentMap.containsKey(key), "Stable field was removed: " + child);
                assertCompatible(expected, currentMap.get(key), child);
            });
            return;
        }
        if (baseline instanceof List<?> baselineList) {
            assertTrue(current instanceof List<?>, "Stable list was removed or changed: " + path);
            List<?> currentList = (List<?>) current;
            if (path.endsWith(".required") || path.endsWith(".enum") || path.endsWith(".type")) {
                assertTrue(currentList.containsAll(baselineList),
                        "Stable required/enum/type values were removed: " + path);
                return;
            }
            if (path.endsWith(".parameters")) {
                for (Object expected : baselineList) {
                    Object actual = findParameter(currentList, expected);
                    assertNotNull(actual, "Stable parameter was removed: " + path + " " + expected);
                    assertCompatible(expected, actual, path + "[" + parameterKey(expected) + "]");
                }
                return;
            }
            assertTrue(currentList.size() >= baselineList.size(), "Stable list entries were removed: " + path);
            for (int index = 0; index < baselineList.size(); index++) {
                assertCompatible(baselineList.get(index), currentList.get(index), path + "[" + index + "]");
            }
            return;
        }
        assertTrue(Objects.equals(baseline, current),
                "Stable value changed at " + path + ": expected " + baseline + ", got " + current);
    }

    private static void assertNoNewRequiredOperationInputs(Map<String, Object> baseline,
                                                            Map<String, Object> current) {
        Map<String, Object> baselinePaths = map(baseline.get("paths"), "baseline paths");
        Map<String, Object> currentPaths = map(current.get("paths"), "current paths");
        baselinePaths.forEach((path, baselineItemValue) -> {
            Map<String, Object> baselineItem = map(baselineItemValue, path);
            Map<String, Object> currentItem = map(currentPaths.get(path), path);
            for (String method : HTTP_METHODS) {
                if (!baselineItem.containsKey(method)) continue;
                Map<String, Object> baselineOperation = map(baselineItem.get(method), method + " " + path);
                Map<String, Object> currentOperation = map(currentItem.get(method), method + " " + path);
                Map<String, Object> expectedParameters = parameters(baselineItem, baselineOperation);
                Map<String, Object> actualParameters = parameters(currentItem, currentOperation);
                actualParameters.forEach((key, value) -> {
                    if (!expectedParameters.containsKey(key)) {
                        assertTrue(!Boolean.TRUE.equals(map(value, key).get("required")),
                                "Compatible additions cannot add a required parameter to " + method + " " + path);
                    }
                });
                Object expectedBody = baselineOperation.get("requestBody");
                Object actualBody = currentOperation.get("requestBody");
                if (expectedBody == null && actualBody != null) {
                    assertTrue(!Boolean.TRUE.equals(map(actualBody, path + ".requestBody").get("required")),
                            "Compatible additions cannot add a required body to " + method + " " + path);
                } else if (expectedBody != null && actualBody != null
                        && !Boolean.TRUE.equals(map(expectedBody, path + ".requestBody").get("required"))) {
                    assertTrue(!Boolean.TRUE.equals(map(actualBody, path + ".requestBody").get("required")),
                            "An optional request body became required for " + method + " " + path);
                }
            }
        });
    }

    private static Map<String, Object> parameters(Map<String, Object> pathItem,
                                                   Map<String, Object> operation) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object value : list(pathItem.get("parameters"))) result.put(parameterKey(value), value);
        for (Object value : list(operation.get("parameters"))) result.put(parameterKey(value), value);
        return result;
    }

    private static Object findParameter(List<?> values, Object expected) {
        String key = parameterKey(expected);
        return values.stream().filter(value -> parameterKey(value).equals(key)).findFirst().orElse(null);
    }

    private static String parameterKey(Object value) {
        Map<String, Object> parameter = map(value, "parameter");
        if (parameter.containsKey("$ref")) return "$ref:" + parameter.get("$ref");
        return parameter.get("in") + ":" + parameter.get("name");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return value == null ? List.of() : (List<Object>) value;
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
