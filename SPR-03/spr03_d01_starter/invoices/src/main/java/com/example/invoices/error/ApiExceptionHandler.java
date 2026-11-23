package com.example.invoices.error;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Every refusal leaves as application/problem+json with type, title, status and detail.
 * Extends ResponseEntityExceptionHandler so Spring's own errors (404 path, 405, 415, ...) get the
 * same shape, and no internal details (stack traces, SQL, class names) are returned.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApi(ApiException e) {
        return ResponseEntity.status(e.status())
                .body(Problems.of(e.status(), e.typeName(), e.title(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(Problems.of(status, "internal-error", "Internal error",
                "Something went wrong on our side. Try again later."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .sorted((a, b) -> a.get("field").compareTo(b.get("field")))
                .toList();
        String detail = "Fix these fields: " + String.join("; ",
                errors.stream().map(e -> e.get("field") + " " + e.get("message")).toList()) + ".";
        ProblemDetail problem = Problems.of(status, "invalid-request", "Invalid request", detail);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(status).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "The request body is missing or is not valid JSON.";
        if (ex.getCause() instanceof InvalidFormatException invalid && invalid.getTargetType().isEnum()) {
            detail = "%s must be one of %s.".formatted(field(invalid),
                    List.of(invalid.getTargetType().getEnumConstants()));
        } else if (ex.getCause() instanceof DatabindException databind && !databind.getPath().isEmpty()) {
            detail = "%s has a value of the wrong type.".formatted(field(databind));
        }
        return ResponseEntity.status(status).body(Problems.of(status, "invalid-request", "Invalid request", detail));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Class<?> type = ex.getRequiredType();
        boolean whole = type == long.class || type == Long.class || type == int.class || type == Integer.class;
        String detail = "%s must be %s.".formatted(ex.getPropertyName(), whole ? "a whole number" : "a valid value");
        return ResponseEntity.status(status).body(Problems.of(status, "invalid-request", "Invalid request", detail));
    }

    /** Spring's own errors: keep their status and detail, use our type and title. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail problem
                && (problem.getType() == null || "about:blank".equals(problem.getType().toString()))) {
            ProblemDetail ours = Problems.of(statusCode, Problems.typeNameFor(statusCode),
                    problem.getTitle() == null ? "Request refused" : problem.getTitle(),
                    problem.getDetail() == null ? "The request was refused." : problem.getDetail());
            return super.handleExceptionInternal(ex, ours, headers, statusCode, request);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static String field(DatabindException e) {
        return String.join(".", e.getPath().stream().map(r -> String.valueOf(r.getPropertyName())).toList());
    }
}
