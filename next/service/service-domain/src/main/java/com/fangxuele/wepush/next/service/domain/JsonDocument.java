package com.fangxuele.wepush.next.service.domain;

public record JsonDocument(String value) {
    public JsonDocument {
        value = DomainChecks.text(value, "JSON document").trim();
        if (!(value.startsWith("{") && value.endsWith("}"))
                && !(value.startsWith("[") && value.endsWith("]"))) {
            throw new IllegalArgumentException("JSON document must be an object or array");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
