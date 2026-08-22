package com.fangxuele.wepush.next.service.api;

import java.util.List;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String traceId,
        List<FieldError> errors
) {
    public ProblemResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public record FieldError(String path, String code, String message) {
    }
}
