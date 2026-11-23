package com.example.invoices.error;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Builds the one problem+json shape every refusal uses. */
public final class Problems {

    public static final String TYPE_BASE = "https://billing.example.com/problems/";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String typeName, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + typeName));
        problem.setTitle(title);
        return problem;
    }

    /** Type name for a status when Spring itself raised the error. */
    public static String typeNameFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "not-found";
            case 409 -> "invalid-state";
            case 500 -> "internal-error";
            default -> "invalid-request";
        };
    }
}
