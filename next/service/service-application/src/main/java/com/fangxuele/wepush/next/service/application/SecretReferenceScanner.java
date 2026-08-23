package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.service.domain.JsonDocument;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds canonical SecretRef value objects without depending on a concrete Provider implementation. */
final class SecretReferenceScanner {
    private final JsonCodec json;

    SecretReferenceScanner(JsonCodec json) {
        this.json = json;
    }

    List<SecretRef> scan(JsonDocument... documents) {
        Map<String, SecretRef> found = new LinkedHashMap<>();
        for (JsonDocument document : documents) {
            visit(json.read(document, Object.class), found);
        }
        return List.copyOf(found.values());
    }

    private void visit(Object value, Map<String, SecretRef> found) {
        if (value instanceof Map<?, ?> map) {
            SecretRef ref = reference(map);
            if (ref != null) found.putIfAbsent(identity(ref), ref);
            map.values().forEach(item -> visit(item, found));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(item -> visit(item, found));
        }
    }

    private static SecretRef reference(Map<?, ?> value) {
        if (value.size() != 3 || !(value.get("namespace") instanceof String namespace)
                || !(value.get("name") instanceof String name)
                || !(value.get("version") instanceof String version)) {
            return null;
        }
        try {
            return new SecretRef(namespace, name, version);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String identity(SecretRef ref) {
        return ref.namespace() + '\0' + ref.name() + '\0' + ref.version();
    }
}
