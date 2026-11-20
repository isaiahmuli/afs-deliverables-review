package com.example.expenses.service;

import com.example.expenses.config.ExpenseLimitsProperties;
import com.example.expenses.domain.Expense;
import com.example.expenses.domain.NewExpense;
import com.example.expenses.repository.ExpenseRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

/** Business rules for expenses. Knows nothing about HTTP. */
@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseLimitsProperties limits;
    private final Clock clock;

    public ExpenseService(ExpenseRepository repository, ExpenseLimitsProperties limits, Clock clock) {
        this.repository = repository;
        this.limits = limits;
        this.clock = clock;
    }

    /** Saves the expense, or throws {@link ClaimLimitExceededException} and saves nothing. */
    public Expense submit(NewExpense expense) {
        if (expense.claimAmount().compareTo(limits.maxClaimAmount()) > 0) {
            throw new ClaimLimitExceededException(expense.claimAmount(), limits.maxClaimAmount());
        }
        return repository.save(expense, clock.instant());
    }

    public List<Expense> findAll() {
        return repository.findAll();
    }

    public Expense findById(long id) {
        return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    }
}
