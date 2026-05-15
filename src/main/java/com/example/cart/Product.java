package com.example.cart;

public record Product(String id, String name, Money price) {
    public Product {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Product id must not be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be null or blank");
        }
        if (price == null) {
            throw new IllegalArgumentException("Product price must not be null");
        }
    }
}
