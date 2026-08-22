package com.fangxuele.wepush.next.service.application;

import java.util.List;

public final class ApplicationProblem extends RuntimeException {
    private final Kind kind;
    private final String code;
    private final List<FieldViolation> violations;

    public ApplicationProblem(Kind kind, String code, String message) {
        this(kind, code, message, List.of());
    }

    public ApplicationProblem(Kind kind, String code, String message, List<FieldViolation> violations) {
        super(message);
        this.kind = kind;
        this.code = code;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public List<FieldViolation> violations() {
        return violations;
    }

    public enum Kind { BAD_REQUEST, NOT_FOUND, CONFLICT, UNPROCESSABLE }

    public record FieldViolation(String path, String code, String message) {
    }
}
