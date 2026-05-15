package com.example.cart;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShoppingCartTest {

    @Test
    void moneyValidationAndNormalization() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Money(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new Money("USD", -1));

        Money money = new Money("usd", 10);
        assertEquals("USD", money.currency());
    }

    @Test
    void moneyPlusAndMultiply() {
        Money a = new Money("USD", 100);
        Money b = new Money("USD", 50);

        assertEquals(new Money("USD", 150), a.plus(b));
        assertEquals(new Money("USD", 300), a.multiply(3));
        assertThrows(IllegalArgumentException.class, () -> a.plus(new Money("EUR", 1)));
    }

    @Test
    void productUsesMoneyPrice() {
        Product product = new Product("p1", "Apple", new Money("USD", 125));
        assertEquals(new Money("USD", 125), product.price());
        assertThrows(IllegalArgumentException.class, () -> new Product("p1", "Apple", null));
    }

    @Test
    void shoppingCartRejectsDifferentCurrencyProducts() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product eurProduct = new Product("p1", "Apple", new Money("EUR", 100));
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(eurProduct, 1));
    }

    @Test
    void subtotalReturnsMoneyAndEmptyCartSubtotalIsZeroInCartCurrency() {
        ShoppingCart cart = new ShoppingCart("usd");
        assertEquals(new Money("USD", 0), cart.subtotal());

        cart.addItem(new Product("p1", "Apple", new Money("USD", 125)), 2);
        cart.addItem(new Product("p2", "Banana", new Money("USD", 99)), 3);
        assertEquals(new Money("USD", 547), cart.subtotal());
    }

    @Test
    void buyTwoGetOneFreePromotionReturnsMoneyDiscount() {
        ShoppingCart cart = new ShoppingCart("USD", List.of(new BuyTwoGetOneFreePromotion(Set.of("p1"))));
        cart.addItem(new Product("p1", "Apple", new Money("USD", 100)), 3);

        assertEquals(new Money("USD", 100), cart.discountTotal());
        assertEquals(new Money("USD", 200), cart.total());
    }

    @Test
    void discountTotalSumsPromotionsAndCapsAtSubtotal() {
        Promotion p1 = c -> new Money("USD", 150);
        Promotion p2 = c -> new Money("USD", 100);

        ShoppingCart cart = new ShoppingCart("USD", List.of(p1, p2));
        cart.addItem(new Product("p1", "Apple", new Money("USD", 200)), 1);

        assertEquals(new Money("USD", 200), cart.discountTotal());
        assertEquals(new Money("USD", 0), cart.total());
    }

    @Test
    void totalEqualsSubtotalMinusDiscount() {
        ShoppingCart cart = new ShoppingCart("USD", List.of(new BuyTwoGetOneFreePromotion(Set.of("p1"))));
        cart.addItem(new Product("p1", "Apple", new Money("USD", 100)), 6);

        assertEquals(new Money("USD", 600), cart.subtotal());
        assertEquals(new Money("USD", 200), cart.discountTotal());
        assertEquals(new Money("USD", 400), cart.total());
    }

    @Test
    void linesAreDeterministicAndReadOnly() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(new Product("p2", "Banana", new Money("USD", 99)), 1);
        cart.addItem(new Product("p1", "Apple", new Money("USD", 125)), 2);

        List<CartLine> lines = cart.lines();
        assertEquals("p1", lines.get(0).product().id());
        assertThrows(UnsupportedOperationException.class,
                () -> lines.add(new CartLine(new Product("p3", "Cherry", new Money("USD", 70)), 1)));
    }

    @Test
    void displayIncludesCurrencySubtotalDiscountAndTotal() {
        ShoppingCart cart = new ShoppingCart("USD", List.of(new BuyTwoGetOneFreePromotion(Set.of("p1"))));
        cart.addItem(new Product("p1", "Apple", new Money("USD", 100)), 3);

        String output = cart.display();
        String expected = String.join(System.lineSeparator(),
                "id=p1, name=Apple, qty=3, unitPrice=USD 100, lineTotal=USD 300",
                "subtotal=USD 300",
                "discount=USD 100",
                "total=USD 200");

        assertEquals(expected, output);
    }

    @Test
    void previousCartBehaviorStillWorks() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product apple = new Product("p1", "Apple", new Money("USD", 100));
        cart.addItem(apple, 5);
        cart.removeItem(apple, 2);
        assertEquals(3, cart.lineQuantity("p1"));
        cart.updateQuantity(apple, 0);
        assertEquals(0, cart.lineQuantity("p1"));
        cart.removeItem(apple, 1);
        assertEquals(0, cart.lineQuantity("p1"));
    }

    @Test
    void promotionAndConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart(null));
        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart(" "));
        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart("USD", null));
        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart("USD", Collections.singletonList((Promotion) null)));

        assertThrows(IllegalArgumentException.class, () -> new BuyTwoGetOneFreePromotion(null));
        assertThrows(IllegalArgumentException.class, () -> new BuyTwoGetOneFreePromotion(Set.of("p1", " ")));

        Promotion promotion = new BuyTwoGetOneFreePromotion(Set.of("p1"));
        assertThrows(IllegalArgumentException.class, () -> promotion.discount(null));
    }
}
