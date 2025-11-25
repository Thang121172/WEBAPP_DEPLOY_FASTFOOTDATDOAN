package com.example.app.model;

import java.io.Serializable;

/**
 * Lớp Model đại diện cho một Dòng sản phẩm trong Đơn hàng (Order).
 * Bao gồm các thông tin chi tiết của một mặt hàng được đặt.
 */
public class OrderItem implements Serializable {

    // Đây là các trường dữ liệu phổ biến cho một mặt hàng trong đơn hàng
    private String menu_item_id; // ID sản phẩm
    private String name; // Tên sản phẩm
    private int qty; // Số lượng
    private double price; // Giá tại thời điểm đặt hàng
    private String brand; // Tên thương hiệu/quán ăn (tùy chọn)

    public OrderItem() {}

    // Constructor đầy đủ (tùy chọn)
    public OrderItem(String menu_item_id, String name, int qty, double price, String brand) {
        this.menu_item_id = menu_item_id;
        this.name = name;
        this.qty = qty;
        this.price = price;
        this.brand = brand;
    }

    // --- Getters and Setters ---

    public String getMenu_item_id() { return menu_item_id; }
    public void setMenu_item_id(String menu_item_id) { this.menu_item_id = menu_item_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}