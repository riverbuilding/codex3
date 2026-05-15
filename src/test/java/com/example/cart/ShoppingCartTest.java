package com.example.cart;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShoppingCartTest {

    @Test
    void addNewItem() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);

        cart.addItem(apple, 2);

        assertEquals(2, cart.lineQuantity("p1"));
    }

    @Test
    void addSameItemTwiceMergesQuantity() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);

        cart.addItem(apple, 2);
        cart.addItem(apple, 3);

        assertEquals(5, cart.lineQuantity("p1"));
    }

    @Test
    void removePartialQuantity() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);
        cart.addItem(apple, 5);

        cart.removeItem(apple, 2);

        assertEquals(3, cart.lineQuantity("p1"));
    }

    @Test
    void removeQuantityGreaterThanCurrentRemovesLine() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);
        cart.addItem(apple, 2);

        cart.removeItem(apple, 3);

        assertEquals(0, cart.lineQuantity("p1"));
    }

    @Test
    void removeMissingProductIsNoOp() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);

        cart.removeItem(apple, 1);

        assertEquals(0, cart.lineQuantity("p1"));
    }

    @Test
    void updateQuantity() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);
        cart.addItem(apple, 2);

        cart.updateQuantity(apple, 9);

        assertEquals(9, cart.lineQuantity("p1"));
    }

    @Test
    void updateQuantityToZeroRemovesLine() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);
        cart.addItem(apple, 2);

        cart.updateQuantity(apple, 0);

        assertEquals(0, cart.lineQuantity("p1"));
    }

    @Test
    void subtotalCalculation() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("p1", "Apple", 125), 2);
        cart.addItem(new Product("p2", "Banana", 99), 3);

        assertEquals(BigInteger.valueOf(547), cart.subtotalCents());
    }

    @Test
    void subtotalSupportsValuesBeyondIntRange() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("p1", "Expensive", Integer.MAX_VALUE), 2);

        assertEquals(BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.valueOf(2)), cart.subtotalCents());
    }

    @Test
    void invalidProduct() {
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "A", 1));
        assertThrows(IllegalArgumentException.class, () -> new Product(" ", "A", 1));
        assertThrows(IllegalArgumentException.class, () -> new Product("p1", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Product("p1", " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new Product("p1", "A", -1));

        ShoppingCart cart = new ShoppingCart();
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cart.removeItem(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cart.updateQuantity(null, 1));
    }

    @Test
    void invalidQuantities() {
        ShoppingCart cart = new ShoppingCart();
        Product apple = new Product("p1", "Apple", 125);

        assertThrows(IllegalArgumentException.class, () -> cart.addItem(apple, 0));
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(apple, -1));
        assertThrows(IllegalArgumentException.class, () -> cart.removeItem(apple, 0));
        assertThrows(IllegalArgumentException.class, () -> cart.removeItem(apple, -1));
        assertThrows(IllegalArgumentException.class, () -> cart.updateQuantity(apple, -1));
    }

    @Test
    void deterministicDisplayOrdering() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("p2", "Banana", 99), 1);
        cart.addItem(new Product("p1", "Apple", 125), 2);

        String output = cart.display();
        String expected = String.join(System.lineSeparator(),
                "id=p1, name=Apple, qty=2, unitPriceCents=125, lineTotalCents=250",
                "id=p2, name=Banana, qty=1, unitPriceCents=99, lineTotalCents=99",
                "subtotalCents=349");

        assertEquals(expected, output);
    }
}
