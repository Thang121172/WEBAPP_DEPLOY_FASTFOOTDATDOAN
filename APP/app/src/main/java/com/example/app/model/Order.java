package com.example.app.model;

import java.util.List;
import java.util.Map;

/**
 * Lớp Model đại diện cho thông tin chi tiết đầy đủ của một Đơn hàng.
 * Đã loại bỏ hoàn toàn các thư viện liên quan đến cơ sở dữ liệu.
 */
public class Order {
    private String orderId;
    private String orderStatus;
    private String merchantName;
    private String deliveryAddress;
    private String creationTime;
    private double totalAmount;
    private double shippingFee;
    private String paymentMethod;

    // Sử dụng List<Map<String, Object>> để tương thích với OrderItemMapAdapter
    private List<Map<String, Object>> items;

    // Lưu ý: Chúng ta cần một Model OrderHistory riêng để làm việc với OrderHistoryAdapter

    public Order() {
    }

    // Constructor đầy đủ (tùy chọn)

    // --- Getters and Setters ---

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    // Thêm các getters/setters khác nếu cần
    // ...
}