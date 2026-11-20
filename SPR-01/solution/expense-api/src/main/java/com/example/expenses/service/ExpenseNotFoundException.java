package com.example.expenses.service;

public class ExpenseNotFoundException extends RuntimeException {

    private final long id;

    public ExpenseNotFoundException(long id) {
        super("No expense with id " + id + ".");
        this.id = id;
    }

    public long id() {
        return id;
    }
}
