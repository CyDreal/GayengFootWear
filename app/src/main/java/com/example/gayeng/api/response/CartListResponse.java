package com.example.gayeng.api.response;

import com.example.gayeng.model.Cart;

import java.util.List;

public class CartListResponse {
    private int status;
    private List<Cart> carts;
    private int total;

    public int getStatus() {
        return status;
    }

    public List<Cart> getCarts() {
        return carts;
    }

    public int getTotal() {
        return total;
    }
}
