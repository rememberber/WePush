package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.ResourceIdGenerator;

import java.util.UUID;

public final class UuidResourceIdGenerator implements ResourceIdGenerator {
    @Override
    public String next(String resourcePrefix) {
        if (resourcePrefix == null || resourcePrefix.isBlank()) {
            throw new IllegalArgumentException("resource prefix must not be blank");
        }
        return resourcePrefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
