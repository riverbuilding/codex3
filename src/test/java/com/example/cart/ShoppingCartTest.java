package com.example.cart;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

        assertEquals(547, cart.subtotalCents());
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
                "subtotalCents=349",
                "discountCents=0",
                "totalCents=349");

        assertEquals(expected, output);
    }

    @Test
    void noPromotionsDiscountZeroAndTotalEqualsSubtotal() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("p1", "Apple", 100), 2);

        assertEquals(200, cart.subtotalCents());
        assertEquals(0, cart.discountCents());
        assertEquals(200, cart.totalCents());
    }

    @Test
    void buyTwoGetOneQuantityOneDiscountZero() {
        ShoppingCart cart = cartWithB2G1("p1");
        cart.addItem(new Product("p1", "Apple", 100), 1);

        assertEquals(0, cart.discountCents());
    }

    @Test
    void buyTwoGetOneQuantityTwoDiscountZero() {
        ShoppingCart cart = cartWithB2G1("p1");
        cart.addItem(new Product("p1", "Apple", 100), 2);

        assertEquals(0, cart.discountCents());
    }

    @Test
    void buyTwoGetOneQuantityThreeGivesOneFree() {
        ShoppingCart cart = cartWithB2G1("p1");
        cart.addItem(new Product("p1", "Apple", 100), 3);

        assertEquals(100, cart.discountCents());
        assertEquals(200, cart.totalCents());
    }

    @Test
    void buyTwoGetOneQuantitySixGivesTwoFree() {
        ShoppingCart cart = cartWithB2G1("p1");
        cart.addItem(new Product("p1", "Apple", 100), 6);

        assertEquals(200, cart.discountCents());
        assertEquals(400, cart.totalCents());
    }

    @Test
    void promotionAppliesOnlyToEligibleProducts() {
        ShoppingCart cart = cartWithB2G1("p1");
        cart.addItem(new Product("p1", "Apple", 100), 3);
        cart.addItem(new Product("p2", "Banana", 80), 3);

        assertEquals(100, cart.discountCents());
    }

    @Test
    void multipleEligibleProductsCalculateCorrectly() {
        ShoppingCart cart = cartWithB2G1("p1", "p2");
        cart.addItem(new Product("p1", "Apple", 100), 3);
        cart.addItem(new Product("p2", "Banana", 50), 6);

        assertEquals(200, cart.discountCents());
    }

    @Test
    void multiplePromotionsAreAdditive() {
        Promotion p1 = new BuyTwoGetOneFreePromotion(Set.of("p1"));
        Promotion p2 = new BuyTwoGetOneFreePromotion(Set.of("p2"));
        ShoppingCart cart = new ShoppingCart(List.of(p1, p2));
        cart.addItem(new Product("p1", "Apple", 100), 3);
        cart.addItem(new Product("p2", "Banana", 60), 3);

        assertEquals(160, cart.discountCents());
        assertEquals(320, cart.totalCents());
    }

    @Test
    void totalNeverGoesBelowZero() {
        Promotion massiveDiscount = new Promotion() {
            @Override
            public int discountCents(CartView cart) {
                return 1_000_000;
            }
        };
        ShoppingCart cart = new ShoppingCart(List.of(massiveDiscount));
        cart.addItem(new Product("p1", "Apple", 100), 1);

        assertEquals(100, cart.discountCents());
        assertEquals(0, cart.totalCents());
    }

    @Test
    void linesDoesNotExposeMutableInternalState() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("p2", "Banana", 50), 1);
        cart.addItem(new Product("p1", "Apple", 100), 1);

        List<CartLine> lines = cart.lines();
        assertEquals("p1", lines.get(0).product().id());
        assertThrows(UnsupportedOperationException.class,
                () -> lines.add(new CartLine(new Product("p3", "Cherry", 70), 1)));
    }

    @Test
    void promotionValidationRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new BuyTwoGetOneFreePromotion(null));
        assertThrows(IllegalArgumentException.class, () -> new BuyTwoGetOneFreePromotion(Set.of("p1", " ")));

        Promotion promotion = new BuyTwoGetOneFreePromotion(Set.of("p1"));
        assertThrows(IllegalArgumentException.class, () -> promotion.discountCents(null));

        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart(null));
        assertThrows(IllegalArgumentException.class, () -> new ShoppingCart(java.util.Collections.singletonList((Promotion) null)));
    }

    private static ShoppingCart cartWithB2G1(String... eligibleIds) {
        return new ShoppingCart(List.of(new BuyTwoGetOneFreePromotion(Set.of(eligibleIds))));
    }
}
