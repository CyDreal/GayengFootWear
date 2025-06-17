package com.example.gayeng.api.response;

import com.example.gayeng.model.Cart;

public class CartResponse extends BaseResponse {
    private Cart cart;

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }
}
