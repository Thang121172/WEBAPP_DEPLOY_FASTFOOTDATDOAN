package com.example.app.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface AdminApi {
    
    // Thống kê tổng quan
    @GET("admin/stats")
    Call<Map<String, Object>> getStats();
    
    // Thống kê chi tiết
    @GET("admin/stats/detailed")
    Call<Map<String, Object>> getDetailedStats();
    
    // Lấy danh sách đơn hàng
    @GET("admin/orders")
    Call<Map<String, Object>> getOrders(@Query("status") String status);
    
    // Lấy danh sách yêu cầu hủy đơn
    @GET("admin/cancel-requests")
    Call<Map<String, Object>> getCancelRequests();
    
    // Duyệt hủy đơn
    @POST("admin/orders/{id}/approve-cancel")
    Call<Map<String, Object>> approveCancel(@Path("id") int orderId);
    
    // Từ chối yêu cầu hủy
    @POST("admin/orders/{id}/reject-cancel")
    Call<Map<String, Object>> rejectCancel(@Path("id") int orderId, @Body Map<String, String> body);
    
    // Lấy danh sách người dùng
    @GET("admin/users")
    Call<Map<String, Object>> getUsers(@Query("role") String role, @Query("search") String search);
    
    // Cập nhật trạng thái người dùng (khóa/mở khóa)
    @PATCH("admin/users/{id}/status")
    Call<Map<String, Object>> updateUserStatus(@Path("id") int userId, @Body Map<String, Boolean> body);
}

