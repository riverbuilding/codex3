package com.example.cart;

public record CartLine(Product product, int quantity) {
    public CartLine {
        if (product == null) {
            throw new IllegalArgumentException("CartLine product must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("CartLine quantity must be positive");
        }
    }

    public int lineTotalCents() {
        return product.priceCents() * quantity;
    }
}
