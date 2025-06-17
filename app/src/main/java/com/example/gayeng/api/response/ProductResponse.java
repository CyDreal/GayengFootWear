package com.example.gayeng.api.response;

import com.example.gayeng.model.Product;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProductResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("products")
    private List<Product> products;

    // Getters

    public int getStatus() {
        return status;
    }

    public List<Product> getProducts() {
        return products;
    }
}
