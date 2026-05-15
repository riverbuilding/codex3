package com.example.cart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCart implements CartView {
    private final Map<String, CartLine> linesByProductId = new HashMap<>();
    private final List<Promotion> promotions;

    public ShoppingCart() {
        this(List.of());
    }

    public ShoppingCart(List<Promotion> promotions) {
        if (promotions == null) {
            throw new IllegalArgumentException("promotions must not be null");
        }
        for (Promotion promotion : promotions) {
            if (promotion == null) {
                throw new IllegalArgumentException("promotion must not be null");
            }
        }
        this.promotions = List.copyOf(promotions);
    }

    public void addItem(Product product, int quantity) {
        validateProductNotNull(product);
        validatePositiveQuantity(quantity, "addItem");

        String productId = product.id();
        CartLine existing = linesByProductId.get(productId);
        if (existing == null) {
            linesByProductId.put(productId, new CartLine(product, quantity));
            return;
        }

        int mergedQuantity = Math.addExact(existing.quantity(), quantity);
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

    @Override
    public List<CartLine> lines() {
        List<CartLine> sortedLines = new ArrayList<>(linesByProductId.values());
        sortedLines.sort(Comparator.comparing(line -> line.product().id()));
        return List.copyOf(sortedLines);
    }

    @Override
    public int subtotalCents() {
        long subtotal = 0;
        for (CartLine line : linesByProductId.values()) {
            subtotal += line.lineTotalCents();
        }
        return Math.toIntExact(subtotal);
    }

    public int discountCents() {
        long discount = 0;
        for (Promotion promotion : promotions) {
            discount += promotion.discountCents(this);
        }
        if (discount <= 0) {
            return 0;
        }
        int subtotal = subtotalCents();
        if (discount > subtotal) {
            return subtotal;
        }
        return (int) discount;
    }

    public int totalCents() {
        int subtotal = subtotalCents();
        int discount = discountCents();
        int total = subtotal - discount;
        return Math.max(0, total);
    }

    public String display() {
        StringBuilder builder = new StringBuilder();
        for (CartLine line : lines()) {
            Product product = line.product();
            builder.append("id=").append(product.id())
                    .append(", name=").append(product.name())
                    .append(", qty=").append(line.quantity())
                    .append(", unitPriceCents=").append(product.priceCents())
                    .append(", lineTotalCents=").append(line.lineTotalCents())
                    .append(System.lineSeparator());
        }

        builder.append("subtotalCents=").append(subtotalCents()).append(System.lineSeparator())
                .append("discountCents=").append(discountCents()).append(System.lineSeparator())
                .append("totalCents=").append(totalCents());
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
