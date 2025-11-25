package com.example.app.model;

public class OrderSummary {

    public String orderId; // ví dụ "FF2025" hoặc "123"
    public String status; // ví dụ "delivered", "preparing", ...
    public double total; // tổng tiền đơn hàng
    public String createdAt; // thời gian đặt đơn, dạng chuỗi ngắn hiển thị

    public OrderSummary() {
    }

    public OrderSummary(String orderId, String status, double total, String createdAt) {
        this.orderId = orderId;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
    }
}
