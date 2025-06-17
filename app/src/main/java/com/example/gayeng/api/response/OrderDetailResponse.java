package com.example.gayeng.api.response;

import com.example.gayeng.model.Order;
import com.google.gson.annotations.SerializedName;

public class OrderDetailResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("order")
    private Order order;

    public int getStatus() { return status; }
    public Order getOrder() { return order; }
}