package com.example.gayeng.api.response;

import com.example.gayeng.model.Order;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OrderResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("orders")
    private List<Order> orders;

    public int getStatus() { return status; }
    public List<Order> getOrders() { return orders; }
}