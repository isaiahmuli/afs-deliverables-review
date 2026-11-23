package com.example.invoices.api;

import com.example.invoices.InvoiceService;
import com.example.invoices.error.BadRequestException;
import com.example.invoices.idempotency.IdempotentInvoiceCreator;
import com.example.invoices.idempotency.IdempotentInvoiceCreator.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices")
@ApiResponse(responseCode = "400", description = "Invalid request",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
public class InvoiceController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final InvoiceService invoices;
    private final IdempotentInvoiceCreator idempotentCreator;

    public InvoiceController(InvoiceService invoices, IdempotentInvoiceCreator idempotentCreator) {
        this.invoices = invoices;
        this.idempotentCreator = idempotentCreator;
    }

    @PostMapping
    @Operation(summary = "Create a draft invoice")
    @ApiResponse(responseCode = "201", description = "Created; Location points at the invoice")
    @ApiResponse(responseCode = "422", description = "Idempotency-Key already used with different content",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public ResponseEntity<InvoiceResponse> create(
            @Parameter(description = "Send the same key again to replay the original response instead of creating a second invoice")
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody CreateInvoiceRequest request) {
        if (idempotencyKey == null) {
            InvoiceResponse body = InvoiceResponse.from(invoices.create(request));
            return ResponseEntity.created(locationOf(body.id())).body(body);
        }
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 255) {
            throw new BadRequestException("Idempotency-Key must be 1 to 255 characters.");
        }
        Result result;
        try {
            result = idempotentCreator.create(idempotencyKey, request, InvoiceController::locationOf);
        } catch (DataIntegrityViolationException raceWithSameKey) {
            // Another request with this key committed first: answer as a replay of it.
            result = idempotentCreator.replay(idempotencyKey, request).orElseThrow(() -> raceWithSameKey);
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.created(result.location());
        if (result.replayed()) {
            response.header("Idempotent-Replayed", "true");
        }
        return response.body(result.body());
    }

    @GetMapping
    @Operation(summary = "List invoices")
    public List<InvoiceResponse> list() {
        return invoices.list().stream().map(InvoiceResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "View an invoice")
    @ApiResponse(responseCode = "200", description = "The invoice")
    @ApiResponse(responseCode = "404", description = "No such invoice",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public InvoiceResponse get(@PathVariable long id) {
        return InvoiceResponse.from(invoices.get(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Change an invoice's status",
            description = "DRAFT→SENT, SENT→PAID, DRAFT or SENT→CANCELLED. Setting the current status changes nothing.")
    @ApiResponse(responseCode = "200", description = "The invoice after the change")
    @ApiResponse(responseCode = "404", description = "No such invoice",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "409", description = "Change not allowed from the current status",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public InvoiceResponse changeStatus(@PathVariable long id, @Valid @RequestBody StatusChangeRequest request) {
        return InvoiceResponse.from(invoices.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft invoice")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "No such invoice (also on a second delete)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    @ApiResponse(responseCode = "409", description = "Not a draft",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemSchema.class)))
    public ResponseEntity<Void> delete(@PathVariable long id) {
        invoices.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static URI locationOf(Long id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/invoices/{id}").buildAndExpand(id).toUri();
    }
}
