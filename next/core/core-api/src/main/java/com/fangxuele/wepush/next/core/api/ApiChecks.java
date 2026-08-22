package com.fangxuele.wepush.next.core.api;

final class ApiChecks {
    private ApiChecks() {
    }

    static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
