package com.example.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * CartResponse Model
 * Dữ liệu trả về từ API về trạng thái giỏ hàng.
 * Bao gồm tổng tiền và danh sách các món hàng (tùy chọn).
 */
public class CartResponse {
    // ID người dùng (có thể dùng để xác nhận)
    @SerializedName("user_id")
    private String userId;

    // Tổng tiền của giỏ hàng (sử dụng long để xử lý số nguyên lớn)
    @SerializedName("total_amount")
    private long totalAmount;

    // Danh sách các món hàng trong giỏ
    @SerializedName("items")
    private List<CartItem> items;

    // Getters
    public String getUserId() {
        return userId;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public List<CartItem> getItems() {
        return items;
    }

    // Setters (tùy chọn, nếu cần chỉnh sửa sau khi nhận data)
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    /**
     * Inner class đại diện cho một món hàng cụ thể trong Giỏ hàng (CartItem).
     */
    public static class CartItem {
        @SerializedName("item_id")
        private String itemId;

        @SerializedName("name")
        private String name;

        @SerializedName("price")
        private long price; // Giá đơn vị

        @SerializedName("quantity")
        private int quantity; // Số lượng

        // Getters and Setters cho CartItem
        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}
