package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.JsonDocument;

public interface JsonCodec {
    JsonDocument canonicalize(Object value);

    <T> T read(JsonDocument document, Class<T> type);
}
