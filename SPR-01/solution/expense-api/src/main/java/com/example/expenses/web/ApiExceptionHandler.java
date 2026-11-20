package com.example.expenses.web;

import com.example.expenses.service.ClaimLimitExceededException;
import com.example.expenses.service.ExpenseNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

/**
 * Turns failures into RFC 9457 problem details. It extends Spring's base handler so that it,
 * not Spring Boot's default problem-details handler, answers validation and parsing errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public record FieldProblem(String field, String message) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldProblem(e.getField(), e.getDefaultMessage()))
                .distinct()
                .sorted(Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message))
                .toList();
        long fields = errors.stream().map(FieldProblem::field).distinct().count();
        return respond(ex, badRequest(
                fields == 1 ? "1 field needs correcting." : fields + " fields need correcting.", errors), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof JacksonException jackson) {
            String field = jackson.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            if (!field.isEmpty()) {
                return respond(ex, badRequest("1 field needs correcting.",
                        List.of(new FieldProblem(field, "is not a valid " + expectedType(field)))), request);
            }
        }
        return respond(ex, badRequest("The request body is missing or is not valid JSON.", List.of()), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return respond(ex, badRequest("1 field needs correcting.",
                List.of(new FieldProblem(name, "must be a whole number"))), request);
    }

    private ResponseEntity<Object> respond(Exception ex, ProblemDetail problem, WebRequest request) {
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    ProblemDetail notFound(ExpenseNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Expense not found");
        problem.setProperty("id", ex.id());
        return problem;
    }

    @ExceptionHandler(ClaimLimitExceededException.class)
    ProblemDetail overLimit(ClaimLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Claim exceeds team limit");
        problem.setProperty("claimAmount", ex.claimAmount());
        problem.setProperty("maxClaimAmount", ex.maxClaimAmount());
        return problem;
    }

    private static ProblemDetail badRequest(String detail, List<FieldProblem> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid expense submission");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static String expectedType(String field) {
        return Map.of(
                "expenseDate", "date (YYYY-MM-DD)",
                "receiptTotal", "amount in US dollars",
                "claimAmount", "amount in US dollars").getOrDefault(field, "value");
    }
}
