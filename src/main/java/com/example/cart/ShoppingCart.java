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
    private CartStatus status = CartStatus.EMPTY;

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
        requireMutable("addItem");
        validateProductNotNull(product);
        validateProductCurrency(product);
        validatePositiveQuantity(quantity, "addItem");

        String productId = product.id();
        CartLine existing = linesByProductId.get(productId);
        if (existing == null) {
            linesByProductId.put(productId, new CartLine(product, quantity));
            refreshStatusFromLines();
            return;
        }

        int mergedQuantity = Math.addExact(existing.quantity(), quantity);
        linesByProductId.put(productId, new CartLine(product, mergedQuantity));
        refreshStatusFromLines();
    }

    public void removeItem(Product product, int quantity) {
        requireMutable("removeItem");
        validateProductNotNull(product);
        validatePositiveQuantity(quantity, "removeItem");

        String productId = product.id();
        CartLine existing = linesByProductId.get(productId);
        if (existing == null) {
            return;
        }

        if (quantity >= existing.quantity()) {
            linesByProductId.remove(productId);
            refreshStatusFromLines();
            return;
        }

        linesByProductId.put(productId, new CartLine(existing.product(), existing.quantity() - quantity));
        refreshStatusFromLines();
    }

    public void updateQuantity(Product product, int quantity) {
        requireMutable("updateQuantity");
        validateProductNotNull(product);
        validateProductCurrency(product);
        if (quantity < 0) {
            throw new IllegalArgumentException("updateQuantity quantity must be >= 0");
        }

        String productId = product.id();
        if (quantity == 0) {
            linesByProductId.remove(productId);
            refreshStatusFromLines();
            return;
        }

        linesByProductId.put(productId, new CartLine(product, quantity));
        refreshStatusFromLines();
    }


    public CartStatus status() {
        return status;
    }

    public void startCheckout() {
        requireStatus(CartStatus.ACTIVE, "startCheckout");
        transitionTo(CartStatus.CHECKOUT, "startCheckout");
    }

    public void cancelCheckout() {
        requireStatus(CartStatus.CHECKOUT, "cancelCheckout");
        transitionTo(CartStatus.ACTIVE, "cancelCheckout");
    }

    public void markPaid() {
        requireStatus(CartStatus.CHECKOUT, "markPaid");
        transitionTo(CartStatus.PAID, "markPaid");
    }

    public void markFulfilled() {
        requireStatus(CartStatus.PAID, "markFulfilled");
        transitionTo(CartStatus.FULFILLED, "markFulfilled");
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
                .append("total=").append(total().currency()).append(" ").append(total().minorUnits()).append(System.lineSeparator())
                .append("status=").append(status);
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


    private void requireStatus(CartStatus expected, String operation) {
        if (status != expected) {
            throw new IllegalStateException("Cannot " + operation + " when cart status is " + status);
        }
    }

    private void requireMutable(String operation) {
        if (status != CartStatus.EMPTY && status != CartStatus.ACTIVE) {
            throw new IllegalStateException("Cannot " + operation + " when cart status is " + status);
        }
    }

    private void refreshStatusFromLines() {
        if (linesByProductId.isEmpty()) {
            transitionTo(CartStatus.EMPTY, "refreshStatusFromLines");
        } else {
            transitionTo(CartStatus.ACTIVE, "refreshStatusFromLines");
        }
    }

    private void transitionTo(CartStatus nextStatus, String operation) {
        if (status == nextStatus) {
            return;
        }
        boolean allowed =
                (status == CartStatus.EMPTY && nextStatus == CartStatus.ACTIVE) ||
                (status == CartStatus.ACTIVE && nextStatus == CartStatus.EMPTY) ||
                (status == CartStatus.ACTIVE && nextStatus == CartStatus.CHECKOUT) ||
                (status == CartStatus.CHECKOUT && nextStatus == CartStatus.ACTIVE) ||
                (status == CartStatus.CHECKOUT && nextStatus == CartStatus.PAID) ||
                (status == CartStatus.PAID && nextStatus == CartStatus.FULFILLED);
        if (!allowed) {
            throw new IllegalStateException("Invalid cart status transition: " + status + " -> " + nextStatus);
        }
        status = nextStatus;
    }

    private static void validatePositiveQuantity(int quantity, String operation) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(operation + " quantity must be positive");
        }
    }
}
