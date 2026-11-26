package com.example.stock.service;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String name) {
        super("No category named '" + name + "'.");
    }
}
