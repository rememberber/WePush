package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

/** A bounded, keyset-paginated resource query. */
public record ResourcePageQuery(String name, String status, Instant from, Instant to,
                                Instant beforeCreatedAt, String beforeId, int limit) {
    public ResourcePageQuery {
        name = name == null || name.isBlank() ? null : name.trim();
        status = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("page limit must be between 1 and 200");
        }
        if ((beforeCreatedAt == null) != (beforeId == null)) {
            throw new IllegalArgumentException("page position is incomplete");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("page time range is invalid");
        }
    }
}
