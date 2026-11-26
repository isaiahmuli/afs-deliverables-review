package com.example.stock.service;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("No product with id " + id + ".");
    }
}
