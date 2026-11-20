package com.example.stock.domain;

final class Names {

    private Names() {
    }

    static String require(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return value.strip();
    }
}
