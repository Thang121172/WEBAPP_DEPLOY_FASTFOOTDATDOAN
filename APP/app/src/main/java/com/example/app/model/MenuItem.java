package com.example.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * MenuItem
 *
 * Đại diện cho 1 món ăn / cửa hàng gợi ý xuất hiện trong HomeFragment.
 *
 * Map trực tiếp JSON từ backend:
 *  { id, restaurant_id, name, description, price, image_url }
 */
public class MenuItem {

    // JSON: "id": 1
    @SerializedName("id")
    public String id;

    // JSON: "restaurant_id": 1
    @SerializedName("restaurant_id")
    public int restaurantId;

    // JSON: "name": "Demo Burger" -> dùng như title trong app
    @SerializedName("name")
    public String title;

    // JSON: "description": "..."
    @SerializedName("description")
    public String description;

    // JSON: "price": 50000.00
    @SerializedName("price")
    public double price;

    // JSON: "image_url": "https://..."
    @SerializedName("image_url")
    public String imageUrl; // có thể null nếu backend chưa trả

    // THÊM TRƯỜNG CHO LOGIC GIỎ HÀNG:
    // Trường này lưu số lượng món ăn khi nó được thêm vào CartManager.
    public int quantity = 0;

    // ctor rỗng: tiện cho việc tự map từ Map<String,Object>
    public MenuItem() {
    }

    // ctor đầy đủ: tiện tạo thủ công
    public MenuItem(String id, String title, String description, double price, String imageUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    /**
     * Copy Constructor.
     * Dùng để tạo bản sao của đối tượng khi thêm vào Giỏ hàng
     * để tránh thay đổi trực tiếp item gốc trong danh sách menu.
     */
    public MenuItem(MenuItem other) {
        this.id = other.id;
        this.restaurantId = other.restaurantId;
        this.title = other.title;
        this.description = other.description;
        this.price = other.price;
        this.imageUrl = other.imageUrl;
        this.quantity = other.quantity;
    }

    // --- GETTERs ---

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    // --- SETTERs cần thiết ---

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }
}
