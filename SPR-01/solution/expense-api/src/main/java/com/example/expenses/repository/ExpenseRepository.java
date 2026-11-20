package com.example.expenses.repository;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.NewExpense;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {

    Expense save(NewExpense expense, Instant submittedAt);

    List<Expense> findAll();

    Optional<Expense> findById(long id);
}
