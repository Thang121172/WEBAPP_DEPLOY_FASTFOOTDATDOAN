package com.example.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * CartRequest Model
 * Dùng để gửi lên Server khi thêm món vào giỏ hàng (API POST /cart/add).
 */
public class CartRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("item_id")
    private String itemId;

    @SerializedName("quantity")
    private int quantity; // Số lượng muốn thêm/giảm (ví dụ: +1)

    // Constructor
    public CartRequest(String userId, String itemId, int quantity) {
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}