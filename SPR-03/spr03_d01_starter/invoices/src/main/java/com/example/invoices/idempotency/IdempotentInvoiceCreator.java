package com.example.invoices.idempotency;

import com.example.invoices.InvoiceService;
import com.example.invoices.api.CreateInvoiceRequest;
import com.example.invoices.api.InvoiceResponse;
import com.example.invoices.error.IdempotencyKeyReusedException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Create with an Idempotency-Key: the first request creates the invoice and stores its response in
 * the same transaction; a replay with the same content gets the stored response; the same key with
 * different content is refused.
 */
@Service
public class IdempotentInvoiceCreator {

    public record Result(InvoiceResponse body, URI location, boolean replayed) {
    }

    private final IdempotencyRecordRepository records;
    private final InvoiceService invoices;
    private final JsonMapper json;

    public IdempotentInvoiceCreator(IdempotencyRecordRepository records, InvoiceService invoices, JsonMapper json) {
        this.records = records;
        this.invoices = invoices;
        this.json = json;
    }

    @Transactional
    public Result create(String key, CreateInvoiceRequest request, Function<Long, URI> locationOf) {
        String hash = fingerprint(request);
        Optional<Result> earlier = replay(key, hash);
        if (earlier.isPresent()) {
            return earlier.get();
        }
        InvoiceResponse body = InvoiceResponse.from(invoices.create(request));
        URI location = locationOf.apply(body.id());
        records.saveAndFlush(new IdempotencyRecord(key, hash, 201, json.writeValueAsString(body), location.toString()));
        return new Result(body, location, false);
    }

    /** The stored response for this key, if any; refuses a key reused with different content. */
    @Transactional(readOnly = true)
    public Optional<Result> replay(String key, CreateInvoiceRequest request) {
        return replay(key, fingerprint(request));
    }

    private Optional<Result> replay(String key, String hash) {
        return records.findById(key).map(record -> {
            if (!record.getRequestHash().equals(hash)) {
                throw new IdempotencyKeyReusedException();
            }
            return new Result(json.readValue(record.getResponseBody(), InvoiceResponse.class),
                    URI.create(record.getLocation()), true);
        });
    }

    /** Same content means same customer, description and amount (1200 and 1200.00 are equal). */
    static String fingerprint(CreateInvoiceRequest r) {
        String canonical = String.join("\u0000", r.customer().strip(), r.description().strip(),
                r.total().stripTrailingZeros().toPlainString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
