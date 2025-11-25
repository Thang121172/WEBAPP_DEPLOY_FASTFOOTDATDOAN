package com.example.app.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
// Tùy chọn: import com.google.gson.annotations.SerializedName; nếu bạn dùng Gson/Retrofit

/**
 * Lớp Model đại diện cho một món ăn/mục trong Menu, được trả về từ API/Database.
 * Dùng để hiển thị danh sách món ăn và cung cấp thông tin cơ bản khi thêm vào CartItem.
 */
public class FoodItem implements Serializable {

    // Tên biến này cần khớp với tên trường JSON trả về từ API của bạn
    private String id;          // ID món ăn gốc (cũng là itemId trong CartItem)
    private String name;
    private BigDecimal price;
    private String description;
    private String restaurantName; // Tên quán/thương hiệu (để tính brand totals trong giỏ hàng)

    // Cần có constructor rỗng cho Retrofit/Gson
    public FoodItem() {}

    // Constructor đầy đủ
    public FoodItem(String id, String name, BigDecimal price, String description, String restaurantName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.restaurantName = restaurantName;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getDescription() { return description; }
    public String getRestaurantName() { return restaurantName; }

    // --- Setters (nếu cần) ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    // Optional: Ghi đè toString() để dễ dàng debug
    @Override
    public String toString() {
        return "FoodItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", restaurantName='" + restaurantName + '\'' +
                '}';
    }
}