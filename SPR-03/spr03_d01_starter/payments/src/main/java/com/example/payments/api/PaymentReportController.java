package com.example.payments.api;

import com.example.payments.report.PaymentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-reports")
@Tag(name = "Payment reports")
public class PaymentReportController {

    private final PaymentReportService service;

    public PaymentReportController(PaymentReportService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Receive a payment report from the provider",
            description = "Marks the invoice paid. The same report again gets the same answer and changes nothing.")
    @ApiResponse(responseCode = "200", description = "Applied (or already applied)")
    @ApiResponse(responseCode = "400", description = "Invalid report",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "409", description = "Invoice was cancelled",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "422", description = "Cannot apply: unknown checkout, not paid, wrong amount, or event id reused",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "502", description = "Invoices service failed",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "504", description = "Invoices service did not answer in time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public ResponseEntity<PaymentReportResponse> receive(@Valid @RequestBody PaymentReportRequest report) {
        PaymentReportService.Result result = service.apply(report);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) {
            response.header("Idempotent-Replayed", "true");
        }
        return response.body(result.response());
    }
}
