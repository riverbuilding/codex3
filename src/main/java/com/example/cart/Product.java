package com.example.cart;

public record Product(String id, String name, int priceCents) {
    public Product {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Product id must not be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be null or blank");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("Product priceCents must not be negative");
        }
    }
}
