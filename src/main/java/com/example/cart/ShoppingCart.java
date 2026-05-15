package com.example.cart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCart implements CartView {
    private final String currency;
    private final Map<String, CartLine> linesByProductId = new HashMap<>();
    private final List<Promotion> promotions;

    public ShoppingCart(String currency) {
        this(currency, List.of());
    }

    public ShoppingCart(String currency, List<Promotion> promotions) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be null or blank");
        }
        this.currency = currency.toUpperCase();

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
        validateProductCurrency(product);
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
        validateProductCurrency(product);
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
    public Money subtotal() {
        Money subtotal = Money.zero(currency);
        for (CartLine line : linesByProductId.values()) {
            subtotal = subtotal.plus(line.lineTotal());
        }
        return subtotal;
    }

    public Money discountTotal() {
        Money discount = Money.zero(currency);
        for (Promotion promotion : promotions) {
            discount = discount.plus(promotion.discount(this));
        }

        Money subtotal = subtotal();
        if (discount.minorUnits() > subtotal.minorUnits()) {
            return subtotal;
        }
        return discount;
    }

    public Money total() {
        return subtotal().minus(discountTotal());
    }

    public String display() {
        StringBuilder builder = new StringBuilder();
        for (CartLine line : lines()) {
            Product product = line.product();
            builder.append("id=").append(product.id())
                    .append(", name=").append(product.name())
                    .append(", qty=").append(line.quantity())
                    .append(", unitPrice=").append(product.price().currency()).append(" ").append(product.price().minorUnits())
                    .append(", lineTotal=").append(line.lineTotal().currency()).append(" ").append(line.lineTotal().minorUnits())
                    .append(System.lineSeparator());
        }

        builder.append("subtotal=").append(subtotal().currency()).append(" ").append(subtotal().minorUnits()).append(System.lineSeparator())
                .append("discount=").append(discountTotal().currency()).append(" ").append(discountTotal().minorUnits()).append(System.lineSeparator())
                .append("total=").append(total().currency()).append(" ").append(total().minorUnits());
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

    private void validateProductCurrency(Product product) {
        if (!currency.equals(product.price().currency())) {
            throw new IllegalArgumentException("Product currency must match cart currency");
        }
    }

    private static void validatePositiveQuantity(int quantity, String operation) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(operation + " quantity must be positive");
        }
    }
}
