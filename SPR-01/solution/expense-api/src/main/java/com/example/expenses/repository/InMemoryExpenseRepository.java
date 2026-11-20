package com.example.expenses.repository;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.NewExpense;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/** Keeps expenses in memory, in id order. Lost when the application stops. */
@Repository
public class InMemoryExpenseRepository implements ExpenseRepository {

    private final ConcurrentSkipListMap<Long, Expense> expenses = new ConcurrentSkipListMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public Expense save(NewExpense e, Instant submittedAt) {
        long id = nextId.getAndIncrement();
        Expense saved = new Expense(id, e.employee(), e.description(), e.expenseDate(),
                e.receiptTotal(), e.claimAmount(), submittedAt);
        expenses.put(id, saved);
        return saved;
    }

    @Override
    public List<Expense> findAll() {
        return List.copyOf(expenses.values());
    }

    @Override
    public Optional<Expense> findById(long id) {
        return Optional.ofNullable(expenses.get(id));
    }
}
