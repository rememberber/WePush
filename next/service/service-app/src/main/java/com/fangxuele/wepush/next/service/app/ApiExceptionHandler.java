package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.ProblemResponse;
import com.fangxuele.wepush.next.service.application.ApplicationProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
final class ApiExceptionHandler {
    @ExceptionHandler(ApplicationProblem.class)
    ResponseEntity<ProblemResponse> applicationProblem(ApplicationProblem exception,
                                                       HttpServletRequest request) {
        HttpStatus status = switch (exception.kind()) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        List<ProblemResponse.FieldError> errors = exception.violations().stream()
                .map(item -> new ProblemResponse.FieldError(item.path(), item.code(), item.message())).toList();
        return response(status, exception.code(), exception.getMessage(), errors, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class})
    ResponseEntity<ProblemResponse> badRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", safeMessage(exception), List.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemResponse> conflict(DataIntegrityViolationException exception,
                                             HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "RESOURCE_CONFLICT",
                "A resource with the same identity or name already exists", List.of(), request);
    }

    private static ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String detail,
                                                            List<ProblemResponse.FieldError> errors,
                                                            HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-ID");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        ProblemResponse body = new ProblemResponse("https://wepush.dev/problems/" + code.toLowerCase(),
                status.getReasonPhrase(), status.value(), code, detail, traceId, errors);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private static String safeMessage(Exception exception) {
        return exception instanceof MissingRequestHeaderException missing
                ? "Required header is missing: " + missing.getHeaderName()
                : "Request body or parameters are invalid";
    }
}
