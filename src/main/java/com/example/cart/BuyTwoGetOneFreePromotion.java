package com.example.cart;

import java.util.HashSet;
import java.util.Set;

public class BuyTwoGetOneFreePromotion implements Promotion {
    private final Set<String> eligibleProductIds;

    public BuyTwoGetOneFreePromotion(Set<String> eligibleProductIds) {
        if (eligibleProductIds == null) {
            throw new IllegalArgumentException("eligibleProductIds must not be null");
        }

        Set<String> validated = new HashSet<>();
        for (String id : eligibleProductIds) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("eligible product id must not be null or blank");
            }
            validated.add(id);
        }

        this.eligibleProductIds = Set.copyOf(validated);
    }

    @Override
    public int discountCents(CartView cart) {
        if (cart == null) {
            throw new IllegalArgumentException("cart must not be null");
        }

        long discount = 0;
        for (CartLine line : cart.lines()) {
            if (!eligibleProductIds.contains(line.product().id())) {
                continue;
            }
            int freeItems = line.quantity() / 3;
            discount += (long) freeItems * line.product().priceCents();
        }

        return Math.toIntExact(discount);
    }
}
