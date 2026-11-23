package com.example.payments.api;

import com.example.payments.PaymentLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/payment-links")
@Tag(name = "Payment links")
@ApiResponse(responseCode = "400", description = "Invalid request",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
public class PaymentLinkController {

    private final PaymentLinkService service;

    public PaymentLinkController(PaymentLinkService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Get the payment link for a sent invoice",
            description = "Creates the link the first time (201); asking again returns the same link (200).")
    @ApiResponse(responseCode = "201", description = "Link created")
    @ApiResponse(responseCode = "200", description = "The existing link")
    @ApiResponse(responseCode = "404", description = "Unknown invoice",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "409", description = "Invoice is not sent",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "502", description = "Invoices service or provider failed",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "504", description = "Invoices service or provider did not answer in time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public ResponseEntity<PaymentLinkResponse> create(@Valid @RequestBody PaymentLinkRequest request) {
        PaymentLinkService.Result result = service.linkFor(request.invoiceId());
        PaymentLinkResponse body = PaymentLinkResponse.from(result.link());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/payment-links/{invoiceId}").buildAndExpand(body.invoiceId()).toUri()).body(body);
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "View an invoice's payment link")
    @ApiResponse(responseCode = "200", description = "The link")
    @ApiResponse(responseCode = "404", description = "No link for this invoice",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public PaymentLinkResponse get(@PathVariable long invoiceId) {
        return PaymentLinkResponse.from(service.get(invoiceId));
    }
}
