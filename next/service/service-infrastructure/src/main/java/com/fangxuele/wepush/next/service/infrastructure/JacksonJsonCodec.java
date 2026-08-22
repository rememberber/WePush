package com.fangxuele.wepush.next.service.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.domain.JsonDocument;

public final class JacksonJsonCodec implements JsonCodec {
    private final ObjectMapper mapper;

    public JacksonJsonCodec() {
        this(new ObjectMapper()
                .findAndRegisterModules()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
    }

    public JacksonJsonCodec(ObjectMapper mapper) {
        this.mapper = mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    @Override
    public JsonDocument canonicalize(Object value) {
        try {
            Object tree = value instanceof String text ? mapper.readTree(text) : value;
            return new JsonDocument(mapper.writeValueAsString(tree));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("invalid JSON document", exception);
        }
    }

    @Override
    public <T> T read(JsonDocument document, Class<T> type) {
        try {
            return mapper.readValue(document.value(), type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("invalid stored JSON document", exception);
        }
    }
}
