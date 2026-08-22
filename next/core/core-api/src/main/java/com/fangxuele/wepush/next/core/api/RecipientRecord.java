package com.fangxuele.wepush.next.core.api;

import java.util.Map;

public record RecipientRecord(String itemId, long sequence, Map<String, RecipientValue> fields) {
    public RecipientRecord {
        itemId = ApiChecks.notBlank(itemId, "itemId");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative");
        }
        if (fields == null) {
            throw new IllegalArgumentException("fields must not be null");
        }
        fields = Map.copyOf(fields);
    }
}
