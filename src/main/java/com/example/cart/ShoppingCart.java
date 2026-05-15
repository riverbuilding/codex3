package com.example.cart;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCart {
    private final Map<String, CartLine> linesByProductId = new HashMap<>();

    public void addItem(Product product, int quantity) {
        validateProductNotNull(product);
        validatePositiveQuantity(quantity, "addItem");

        String productId = product.id();
        CartLine existing = linesByProductId.get(productId);
        if (existing == null) {
            linesByProductId.put(productId, new CartLine(product, quantity));
            return;
        }

        int mergedQuantity = existing.quantity() + quantity;
        linesByProductId.put(productId, new CartLine(product, mergedQuantity));
    }

    public void removeItem(Product product, int quantity) {
        validateProductNotNull(product);
        validatePositiveQuantity(quantity, "removeItem");

        String productId = product.id();
        CartLine existing = linesByProductId.get(productId);
        if (existing == null) {
            return;
        }

        if (quantity >= existing.quantity()) {
            linesByProductId.remove(productId);
            return;
        }

        linesByProductId.put(productId, new CartLine(existing.product(), existing.quantity() - quantity));
    }

    public void updateQuantity(Product product, int quantity) {
        validateProductNotNull(product);
        if (quantity < 0) {
            throw new IllegalArgumentException("updateQuantity quantity must be >= 0");
        }

        String productId = product.id();
        if (quantity == 0) {
            linesByProductId.remove(productId);
            return;
        }

        linesByProductId.put(productId, new CartLine(product, quantity));
    }

    public BigInteger subtotalCents() {
        return linesByProductId.values().stream()
                .map(CartLine::lineTotalCents)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public String display() {
        List<CartLine> sortedLines = new ArrayList<>(linesByProductId.values());
        sortedLines.sort(Comparator.comparing(line -> line.product().id()));

        StringBuilder builder = new StringBuilder();
        for (CartLine line : sortedLines) {
            Product product = line.product();
            builder.append("id=").append(product.id())
                    .append(", name=").append(product.name())
                    .append(", qty=").append(line.quantity())
                    .append(", unitPriceCents=").append(product.priceCents())
                    .append(", lineTotalCents=").append(line.lineTotalCents())
                    .append(System.lineSeparator());
        }

        builder.append("subtotalCents=").append(subtotalCents());
        return builder.toString();
    }

    public int lineQuantity(String productId) {
        CartLine line = linesByProductId.get(productId);
        return line == null ? 0 : line.quantity();
    }

    private static void validateProductNotNull(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }
    }

    private static void validatePositiveQuantity(int quantity, String operation) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(operation + " quantity must be positive");
        }
    }
}
