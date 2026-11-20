package com.example.expenses.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.NewExpense;
import com.example.expenses.service.ClaimLimitExceededException;
import com.example.expenses.service.ExpenseNotFoundException;
import com.example.expenses.service.ExpenseService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Web layer only: the real controller and error handler, with the service mocked. */
@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExpenseService service;

    private static final Expense FUEL = new Expense(1, "Amina Otieno", "Fuel for client visit",
            LocalDate.parse("2026-11-18"), new BigDecimal("120.00"), new BigDecimal("50.00"),
            Instant.parse("2026-11-19T08:00:00Z"));

    private ResultActions submit(String json) throws Exception {
        return mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String body(String employee, String description, String date, String receipt, String claim) {
        return """
                {"employee": %s, "description": %s, "expenseDate": %s, "receiptTotal": %s, "claimAmount": %s}
                """.formatted(employee, description, date, receipt, claim);
    }

    @Test
    void validSubmissionReturns201WithTheSavedExpense() throws Exception {
        given(service.submit(any())).willReturn(FUEL);

        submit(body("\"Amina Otieno\"", "\"Fuel for client visit\"", "\"2026-11-18\"", "120.00", "50.00"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/expenses/1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employee").value("Amina Otieno"))
                .andExpect(jsonPath("$.description").value("Fuel for client visit"))
                .andExpect(jsonPath("$.expenseDate").value("2026-11-18"))
                .andExpect(jsonPath("$.receiptTotal").value(120.00))
                .andExpect(jsonPath("$.claimAmount").value(50.00))
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(service).submit(new NewExpense("Amina Otieno", "Fuel for client visit",
                LocalDate.parse("2026-11-18"), new BigDecimal("120.00"), new BigDecimal("50.00")));
    }

    @Test
    void claimEqualToReceiptTotalIsValid() throws Exception {
        given(service.submit(any())).willReturn(FUEL);

        submit(body("\"A\"", "\"B\"", "\"2026-11-18\"", "150.00", "150.00")).andExpect(status().isCreated());
    }

    @Test
    void emptySubmissionNamesEveryFieldInOneResponseAndSavesNothing() throws Exception {
        submit("{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid expense submission"))
                .andExpect(jsonPath("$.detail").value("5 fields need correcting."))
                .andExpect(jsonPath("$.errors[*].field", contains(
                        "claimAmount", "description", "employee", "expenseDate", "receiptTotal")))
                .andExpect(jsonPath("$.errors[?(@.field=='employee')].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[?(@.field=='claimAmount')].message").value("must not be null"));

        verify(service, never()).submit(any());
    }

    @Test
    void customConstraintRefusesAClaimAboveTheReceiptTotal() throws Exception {
        submit(body("\"Amina\"", "\"Taxi\"", "\"2026-11-18\"", "50.00", "80.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("claimAmount"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be more than the receipt total"));

        verify(service, never()).submit(any());
    }

    @Test
    void customConstraintIsReportedAlongsideBuiltInFailures() throws Exception {
        submit(body("\"  \"", "\"Taxi\"", "\"2999-01-01\"", "50.00", "80.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("3 fields need correcting."))
                .andExpect(jsonPath("$.errors[*].message", containsInAnyOrder(
                        "must not be more than the receipt total",
                        "must not be blank",
                        "must be a date in the past or in the present")));
    }

    @ParameterizedTest(name = "claimAmount {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "0      | must be greater than 0",
            "-5.00  | must be greater than 0",
            "10.555 | numeric value out of bounds (<7 digits>.<2 digits> expected)",
    })
    void badAmountsAreRefusedOnThatField(String amount, String message) throws Exception {
        submit(body("\"Amina\"", "\"Taxi\"", "\"2026-11-18\"", "100.00", amount))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("claimAmount"))
                .andExpect(jsonPath("$.errors[*].message", contains(message)));

        verify(service, never()).submit(any());
    }

    @Test
    void wrongTypeNamesTheField() throws Exception {
        submit(body("\"Amina\"", "\"Taxi\"", "\"2026-11-18\"", "\"lots\"", "10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("receiptTotal"))
                .andExpect(jsonPath("$.errors[0].message").value("is not a valid amount in US dollars"));
    }

    @Test
    void impossibleDateNamesTheField() throws Exception {
        submit(body("\"Amina\"", "\"Taxi\"", "\"2026-13-40\"", "10.00", "10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("expenseDate"));
    }

    @Test
    void bodyThatIsNotJsonIsRefused() throws Exception {
        submit("not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON."));
    }

    @Test
    void overTheTeamLimitReturns422WithTheReason() throws Exception {
        given(service.submit(any())).willThrow(
                new ClaimLimitExceededException(new BigDecimal("150.00"), new BigDecimal("100.00")));

        submit(body("\"Amina\"", "\"Hotel\"", "\"2026-11-18\"", "150.00", "150.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Claim exceeds team limit"))
                .andExpect(jsonPath("$.detail").value(
                        "The claim of 150.00 USD exceeds this team's limit of 100.00 USD per claim."))
                .andExpect(jsonPath("$.maxClaimAmount").value(100.00));
    }

    @Test
    void listReturnsEveryExpense() throws Exception {
        given(service.findAll()).willReturn(List.of(FUEL));

        mvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getReturnsOneExpense() throws Exception {
        given(service.findById(1)).willReturn(FUEL);

        mvc.perform(get("/api/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").value("Amina Otieno"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        given(service.findById(99)).willThrow(new ExpenseNotFoundException(99));

        mvc.perform(get("/api/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Expense not found"))
                .andExpect(jsonPath("$.detail").value("No expense with id 99."));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/expenses/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }
}
