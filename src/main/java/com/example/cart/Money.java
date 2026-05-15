package com.example.cart;

public record Money(String currency, long minorUnits) {
    public Money {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be null or blank");
        }
        currency = currency.toUpperCase();
        if (minorUnits < 0) {
            throw new IllegalArgumentException("minorUnits must be >= 0");
        }
    }

    public static Money zero(String currency) {
        return new Money(currency, 0);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(currency, Math.addExact(minorUnits, other.minorUnits));
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        long result = Math.subtractExact(minorUnits, other.minorUnits);
        if (result < 0) {
            throw new IllegalArgumentException("money result must not be negative");
        }
        return new Money(currency, result);
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        return new Money(currency, Math.multiplyExact(minorUnits, quantity));
    }

    private void requireSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("other money must not be null");
        }
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currencies must match");
        }
    }
}
