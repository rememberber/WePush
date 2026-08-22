package com.fangxuele.wepush.next.service.domain;

public record WorkspaceId(String value) {
    public WorkspaceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("workspace ID must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
