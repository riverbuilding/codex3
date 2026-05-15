package com.example.cart;

import java.util.List;

public interface CartView {
    List<CartLine> lines();

    Money subtotal();
}
