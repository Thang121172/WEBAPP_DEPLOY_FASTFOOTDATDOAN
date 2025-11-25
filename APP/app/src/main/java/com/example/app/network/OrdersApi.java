package com.example.app.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * OrdersApi
 *
 * Base URL: BackendConfig -> http://<ip>:8000/api/
 * Các path dưới đây là đường dẫn tương đối.
 *
 * Lưu ý:
 * - Authorization/Content-Type đã được gắn bởi OkHttp Interceptor
 * (BackendConfig/AuthClient).
 * - Dùng Map<String,Object> để linh hoạt theo schema backend hiện tại.
 */
public interface OrdersApi {

    /* ================== CREATE / DETAIL ================== */

    /** Tạo đơn hàng mới */
    @Headers("Accept: application/json")
    @POST("orders")
    Call<Map<String, Object>> createOrder(@Body Map<String, Object> body);

    /** Lấy chi tiết một đơn hàng theo id/code */
    @Headers("Accept: application/json")
    @GET("orders/{id}")
    Call<Map<String, Object>> getOrder(@Path("id") String id);

    /* ================== LISTING (CUSTOMER) ================== */

    /**
     * Danh sách đơn của khách (có phân trang & lọc trạng thái).
     * status gợi ý: pending|preparing|ready|delivering|delivered|cancelled
     */
    @Headers("Accept: application/json")
    @GET("customer/orders")
    Call<List<Map<String, Object>>> listOrders(
            @Query("status") String status,
            @Query("page") Integer page,
            @Query("page_size") Integer pageSize);

    /** Đơn gần đây cho màn Home (giữ tương thích) */
    @Headers("Accept: application/json")
    @GET("customer/recent-orders")
    Call<List<Map<String, Object>>> getRecentOrders(@Query("limit") int limit);

    /* ================== PRICE / PROMO ================== */

    /**
     * Ước tính phí/tổng trước khi tạo đơn.
     * body gợi ý: { items: [...], address_id / lat,lng, voucher_code }
     * response gợi ý: { subtotal, delivery_fee, discount, total, eta_minutes }
     */
    @Headers("Accept: application/json")
    @POST("orders/estimate")
    Call<Map<String, Object>> estimate(@Body Map<String, Object> body);

    /**
     * Áp voucher/mã khuyến mãi để tính lại tổng.
     * body gợi ý: { items: [...], voucher_code, address_id / lat,lng }
     */
    @Headers("Accept: application/json")
    @POST("orders/apply-voucher")
    Call<Map<String, Object>> applyVoucher(@Body Map<String, Object> body);

    /* ================== ORDER ACTIONS (CUSTOMER) ================== */

    /** Huỷ đơn (khi chưa vào preparing). body có thể kèm reason */
    @Headers("Accept: application/json")
    @POST("orders/{id}/cancel")
    Call<Map<String, Object>> cancelOrder(
            @Path("id") String id,
            @Body Map<String, Object> body);

    /** Xác nhận đã nhận hàng (hoàn tất phía khách) */
    @Headers("Accept: application/json")
    @POST("orders/{id}/confirm-received")
    Call<Map<String, Object>> confirmReceived(@Path("id") String id);

    /** Theo dõi tiến trình đơn (timeline events) */
    @Headers("Accept: application/json")
    @GET("orders/{id}/tracking")
    Call<List<Map<String, Object>>> getTracking(@Path("id") String id);

    /* ================== PAYMENT (OPTIONAL) ================== */

    /**
     * Khởi tạo thanh toán online.
     * body gợi ý: { method: "vnpay|momo|zalopay|cod", return_url, ... }
     * response: { payment_url | payment_intent, ... }
     */
    @Headers("Accept: application/json")
    @POST("orders/{id}/pay")
    Call<Map<String, Object>> initiatePayment(
            @Path("id") String id,
            @Body Map<String, Object> body);

    /**
     * (Nếu cần) xác nhận thanh toán sau khi redirect/callback.
     * body gợi ý: { transaction_id, status, signature, ... }
     */
    @Headers("Accept: application/json")
    @POST("orders/{id}/pay/confirm")
    Call<Map<String, Object>> confirmPayment(
            @Path("id") String id,
            @Body Map<String, Object> body);
}
