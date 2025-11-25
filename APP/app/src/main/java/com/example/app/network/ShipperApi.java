package com.example.app.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * ShipperApi — endpoints cho vai SHIPPER.
 *
 * Base URL: BackendConfig.getRetrofit(context) → http://host:port/api/
 * (vì vậy KHÔNG thêm "api/" vào trước path bên dưới).
 *
 * Gợi ý backend (ví dụ):
 * - GET /shipper/orders?status=available|assigned|delivering|completed
 * - GET /shipper/orders/{id}
 * - POST /shipper/orders/{id}/accept
 * - PATCH/POST /shipper/orders/{id} (hoặc /{id}/status) body: { "status":
 * "...", "reason"?: "..." }
 * - POST /shipper/location body: { "lat": <double>, "lng": <double>,
 * "accuracy"?: <float> }
 */
public interface ShipperApi {

    /**
     * Lấy danh sách đơn theo trạng thái bucket.
     * 
     * @param status available | assigned | delivering | completed (có thể null nếu
     *               backend trả tất cả)
     */
    @GET("shipper/orders")
    Call<List<Map<String, Object>>> getShipperOrders(@Query("status") String status);

    /** Lấy chi tiết đơn. */
    @GET("shipper/orders/{id}")
    Call<Map<String, Object>> getShipperOrder(@Path("id") String orderId);

    /** Nhận/claim một đơn chưa ai nhận. */
    @POST("shipper/orders/{id}/accept")
    Call<Map<String, Object>> acceptOrder(@Path("id") String orderId);

    /**
     * Cập nhật trạng thái đơn trong hành trình giao:
     * status: arrived_store | picked_up | on_the_way | delivered | failed
     * Nếu failed có thể kèm "reason".
     *
     * NOTE: Nếu server không hỗ trợ PATCH, chuyển sang:
     * @POST("shipper/orders/{id}/status")
     */
    @PATCH("shipper/orders/{id}")
    Call<Map<String, Object>> updateOrderStatus(
            @Path("id") String orderId,
            @Body Map<String, Object> body);

    /** Cập nhật toạ độ hiện tại của shipper để realtime map. */
    @POST("shipper/location")
    Call<Map<String, Object>> updateLocation(@Body Map<String, Object> locationBody);

    /** Lấy doanh thu và thống kê của shipper. */
    @GET("shipper/revenue")
    Call<Map<String, Object>> getRevenue();

    /** Lấy thông tin profile của shipper. */
    @GET("shipper/profile")
    Call<Map<String, Object>> getProfile();

    /** Cập nhật thông tin profile của shipper (phone, vehicle_plate). */
    @PATCH("shipper/profile")
    Call<Map<String, Object>> updateProfile(@Body Map<String, Object> body);
}
