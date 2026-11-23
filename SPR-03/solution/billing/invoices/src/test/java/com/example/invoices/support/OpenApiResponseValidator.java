package com.example.invoices.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks a JSON response body against a schema in this service's OpenAPI document.
 *
 * <pre>
 * String doc = OpenApiResponseValidator.fetchDocument(mvc);
 * OpenApiResponseValidator.assertMatches(doc, "MySchemaName", responseBody);
 * </pre>
 */
public final class OpenApiResponseValidator {

    private OpenApiResponseValidator() {
    }

    /** Returns the OpenAPI document that springdoc serves at /v3/api-docs. */
    public static String fetchDocument(MockMvc mvc) throws Exception {
        return mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
    }

    /** Fails with every validation error when the body does not match components.schemas.&lt;schemaName&gt;. */
    public static void assertMatches(String openApiDocument, String schemaName, String jsonBody) {
        // Turn the whole document into one root schema that points at the named schema,
        // so every other $ref inside the document still resolves.
        String root = "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + " \"$ref\": \"#/components/schemas/" + schemaName + "\","
                + openApiDocument.strip().substring(1);
        Schema schema = SchemaRegistry
                .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(root);
        List<com.networknt.schema.Error> errors = schema.validate(jsonBody, InputFormat.JSON);
        if (!errors.isEmpty()) {
            throw new AssertionError("Response does not match schema " + schemaName + ": " + errors);
        }
    }
}
