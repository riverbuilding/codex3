package com.example.cart;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShoppingCartTest {

    @Test
    void newCartStartsEmpty() {
        ShoppingCart cart = new ShoppingCart("USD");
        assertEquals(CartStatus.EMPTY, cart.status());
    }

    @Test
    void addFirstItemTransitionsEmptyToActive() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        assertEquals(CartStatus.ACTIVE, cart.status());
    }

    @Test
    void removingLastItemTransitionsActiveToEmpty() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product p = product("p1", 100);
        cart.addItem(p, 1);
        cart.removeItem(p, 1);
        assertEquals(CartStatus.EMPTY, cart.status());
    }

    @Test
    void updateQuantityZeroTransitionsActiveToEmpty() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product p = product("p1", 100);
        cart.addItem(p, 2);
        cart.updateQuantity(p, 0);
        assertEquals(CartStatus.EMPTY, cart.status());
    }

    @Test
    void startCheckoutTransitionsActiveToCheckout() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        cart.startCheckout();
        assertEquals(CartStatus.CHECKOUT, cart.status());
    }

    @Test
    void startCheckoutFromEmptyFails() {
        ShoppingCart cart = new ShoppingCart("USD");
        assertThrows(IllegalStateException.class, cart::startCheckout);
    }

    @Test
    void cancelCheckoutTransitionsCheckoutToActive() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        cart.startCheckout();
        cart.cancelCheckout();
        assertEquals(CartStatus.ACTIVE, cart.status());
    }

    @Test
    void cancelCheckoutFromActiveFails() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        assertThrows(IllegalStateException.class, cart::cancelCheckout);
    }

    @Test
    void markPaidTransitionsCheckoutToPaid() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        cart.startCheckout();
        cart.markPaid();
        assertEquals(CartStatus.PAID, cart.status());
    }

    @Test
    void markPaidFromActiveFails() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        assertThrows(IllegalStateException.class, cart::markPaid);
    }

    @Test
    void markFulfilledTransitionsPaidToFulfilled() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        cart.startCheckout();
        cart.markPaid();
        cart.markFulfilled();
        assertEquals(CartStatus.FULFILLED, cart.status());
    }

    @Test
    void markFulfilledFromCheckoutFails() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        cart.startCheckout();
        assertThrows(IllegalStateException.class, cart::markFulfilled);
    }

    @Test
    void cannotAddItemInCheckout() {
        ShoppingCart cart = new ShoppingCart("USD");
        cart.addItem(product("p1", 100), 1);
        cart.startCheckout();
        assertThrows(IllegalStateException.class, () -> cart.addItem(product("p2", 50), 1));
    }

    @Test
    void cannotRemoveItemInCheckout() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product p = product("p1", 100);
        cart.addItem(p, 1);
        cart.startCheckout();
        assertThrows(IllegalStateException.class, () -> cart.removeItem(p, 1));
    }

    @Test
    void cannotUpdateQuantityInCheckout() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product p = product("p1", 100);
        cart.addItem(p, 1);
        cart.startCheckout();
        assertThrows(IllegalStateException.class, () -> cart.updateQuantity(p, 2));
    }

    @Test
    void cannotMutateCartInPaid() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product p = product("p1", 100);
        cart.addItem(p, 1);
        cart.startCheckout();
        cart.markPaid();

        assertThrows(IllegalStateException.class, () -> cart.addItem(product("p2", 10), 1));
        assertThrows(IllegalStateException.class, () -> cart.removeItem(p, 1));
        assertThrows(IllegalStateException.class, () -> cart.updateQuantity(p, 2));
    }

    @Test
    void cannotMutateCartInFulfilled() {
        ShoppingCart cart = new ShoppingCart("USD");
        Product p = product("p1", 100);
        cart.addItem(p, 1);
        cart.startCheckout();
        cart.markPaid();
        cart.markFulfilled();

        assertThrows(IllegalStateException.class, () -> cart.addItem(product("p2", 10), 1));
        assertThrows(IllegalStateException.class, () -> cart.removeItem(p, 1));
        assertThrows(IllegalStateException.class, () -> cart.updateQuantity(p, 2));
    }

    @Test
    void displayIncludesCartStatus() {
        ShoppingCart cart = new ShoppingCart("USD", List.of(new BuyTwoGetOneFreePromotion(Set.of("p1"))));
        cart.addItem(product("p1", 100), 3);

        String output = cart.display();
        String expected = String.join(System.lineSeparator(),
                "id=p1, name=P-p1, qty=3, unitPrice=USD 100, lineTotal=USD 300",
                "subtotal=USD 300",
                "discount=USD 100",
                "total=USD 200",
                "status=ACTIVE");
        assertEquals(expected, output);
    }

    @Test
    void moneyAndPromotionAndCurrencyBehaviorStillWorks() {
        Money money = new Money("usd", 10);
        assertEquals("USD", money.currency());
        assertEquals(new Money("USD", 30), money.multiply(3));

        ShoppingCart cart = new ShoppingCart("USD", List.of(new BuyTwoGetOneFreePromotion(Set.of("p1"))));
        cart.addItem(product("p1", 100), 3);
        assertEquals(new Money("USD", 300), cart.subtotal());
        assertEquals(new Money("USD", 100), cart.discountTotal());
        assertEquals(new Money("USD", 200), cart.total());

        assertThrows(IllegalArgumentException.class, () -> cart.addItem(new Product("e1", "EUR", new Money("EUR", 100)), 1));
        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart("USD", Collections.singletonList((Promotion) null)));
    }

    private static Product product(String id, long priceMinorUnits) {
        return new Product(id, "P-" + id, new Money("USD", priceMinorUnits));
    }
}
