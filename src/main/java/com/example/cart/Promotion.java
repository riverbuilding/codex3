package com.example.cart;

public interface Promotion {
    Money discount(CartView cart);
}
