package com.example.payments.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;

/**
 * Documents the problem+json body every refusal uses. Only for the OpenAPI document: responses are
 * built as Spring ProblemDetail. Unlike springdoc's generated ProblemDetail schema, this one marks
 * type, title, status and detail as required, so validating against it means something.
 */
@Schema(name = "Problem", description = "RFC 9457 problem details")
public record ProblemSchema(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://billing.example.com/problems/not-found") URI type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Not found") String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "404") int status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Invoice INV-1001 does not exist.") String detail,
        @Schema(example = "/api/v1/invoices/1001") String instance,
        @Schema(description = "Only for invalid fields") List<FieldError> errors) {

    @Schema(name = "FieldError")
    public record FieldError(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "total") String field,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "must be above zero") String message) {
    }
}
