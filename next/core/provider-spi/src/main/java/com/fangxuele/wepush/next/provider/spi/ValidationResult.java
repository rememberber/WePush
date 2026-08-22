package com.fangxuele.wepush.next.provider.spi;

import java.util.List;

public record ValidationResult(List<Violation> violations) {
    public ValidationResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult invalid(String path, String code, String message) {
        return new ValidationResult(List.of(new Violation(path, code, message)));
    }

    public boolean validResult() {
        return violations.isEmpty();
    }

    public record Violation(String path, String code, String message) {
        public Violation {
            path = path == null ? "" : path;
            if (code == null || code.isBlank() || message == null || message.isBlank()) {
                throw new IllegalArgumentException("violation code and message are required");
            }
        }
    }
}
