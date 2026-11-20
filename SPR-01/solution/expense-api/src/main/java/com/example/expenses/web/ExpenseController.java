package com.example.expenses.web;

import com.example.expenses.domain.Expense;
import com.example.expenses.service.ExpenseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP only: maps requests to the service and results to responses. */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> submit(@Valid @RequestBody ExpenseRequest request) {
        Expense saved = service.submit(request.toNewExpense());
        return ResponseEntity.created(URI.create("/api/expenses/" + saved.id()))
                .body(ExpenseResponse.from(saved));
    }

    @GetMapping
    public List<ExpenseResponse> list() {
        return service.findAll().stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable long id) {
        return ExpenseResponse.from(service.findById(id));
    }
}
