package com.example.app.network;

import android.content.Context;

import java.util.Map;

import retrofit2.Callback;

/**
 * OrdersClient
 *
 * Wrapper cấp cao cho OrdersApi, giúp gọi các API liên quan tới đơn hàng.
 *
 * Sử dụng:
 * - Base URL: BackendConfig -> http://<ip>:8000/api/
 * - Authorization Header tự động gắn qua Interceptor trong
 * BackendConfig/AuthClient
 *
 * Đảm bảo:
 * - Mọi lệnh gọi dùng chung Retrofit instance (timeout, logging,...)
 */
public class OrdersClient {

    private final OrdersApi api;

    public OrdersClient(Context ctx) {
        // Khởi tạo retrofit interface từ config dùng chung
        this.api = BackendConfig.getRetrofit(ctx).create(OrdersApi.class);
    }

    /**
     * Gửi yêu cầu tạo đơn hàng mới.
     * 
     * @param body JSON body: { "items": [...], "address": "...", ... }
     * @param cb   callback để nhận phản hồi (thành công/thất bại)
     */
    public void createOrder(Map<String, Object> body, Callback<Map<String, Object>> cb) {
        if (cb == null)
            return;
        api.createOrder(body).enqueue(cb);
    }

    /**
     * Lấy chi tiết đơn hàng theo ID.
     * 
     * @param id mã định danh đơn hàng
     * @param cb callback nhận kết quả
     */
    public void getOrder(String id, Callback<Map<String, Object>> cb) {
        if (cb == null)
            return;
        api.getOrder(id).enqueue(cb);
    }

    /**
     * Hủy đơn hàng.
     * 
     * @param orderId ID đơn hàng cần hủy
     * @param reason Lý do hủy (có thể null)
     * @param cb callback nhận kết quả
     */
    public void cancelOrder(String orderId, String reason, Callback<Map<String, Object>> cb) {
        if (cb == null)
            return;
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        if (reason != null && !reason.trim().isEmpty()) {
            body.put("reason", reason);
        }
        api.cancelOrder(orderId, body).enqueue(cb);
    }
}
