package com.example.cart;

import java.math.BigInteger;

public record CartLine(Product product, int quantity) {
    public CartLine {
        if (product == null) {
            throw new IllegalArgumentException("CartLine product must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("CartLine quantity must be positive");
        }
    }

    public BigInteger lineTotalCents() {
        return BigInteger.valueOf(product.priceCents())
                .multiply(BigInteger.valueOf(quantity));
    }
}
