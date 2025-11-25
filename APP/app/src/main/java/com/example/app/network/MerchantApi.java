package com.example.app.network;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * MerchantApi
 *
 * Khớp với cách gọi trong MerchantHomeFragment:
 * - getMerchantOrders(status) -> GET
 * /merchant/orders?status=pending|preparing|ready
 * - acceptOrder(id) -> PATCH /merchant/orders/{id}/accept
 * - updateOrderStatus(id, body) -> PATCH /merchant/orders/{id}/status {
 * "status": "..." }
 *
 * Gợi ý map trạng thái ↔ UI tabs:
 * - "pending" => tab "Mới" (recycler_new)
 * - "preparing" => tab "Đang làm" (recycler_in_progress)
 * - "ready" => tab "Hoàn tất" (recycler_completed)
 *
 * Lưu ý:
 * - Base URL lấy từ AuthClient.getRetrofit().
 * - Nếu backend bạn đặt dưới /api/merchant/... hãy đổi chuỗi trong annotation
 * cho khớp.
 */
public interface MerchantApi {

    /** Lấy danh sách đơn theo trạng thái bucket của Merchant. */
    @GET("merchant/orders")
    Call<List<Map<String, Object>>> getMerchantOrders(@Query("status") String status);

    /**
     * Merchant chấp nhận đơn: server chuyển trạng thái pending -> preparing (hoặc
     * confirm).
     */
    @PATCH("merchant/orders/{id}/accept")
    Call<Map<String, Object>> acceptOrder(@Path("id") String orderId);

    /** Cập nhật trạng thái đơn (partial update). */
    @PATCH("merchant/orders/{id}/status")
    Call<Map<String, Object>> updateOrderStatus(
            @Path("id") String orderId,
            @Body Map<String, Object> body);

    // ✅ FIX: Menu Management APIs
    /** Lấy danh sách món ăn của merchant */
    @GET("merchant/menu")
    Call<List<Map<String, Object>>> getMerchantMenu();

    /** Thêm món ăn mới */
    @POST("merchant/menu")
    Call<Map<String, Object>> addMenuItem(@Body Map<String, Object> body);

    /** Cập nhật món ăn */
    @PATCH("merchant/menu/{id}")
    Call<Map<String, Object>> updateMenuItem(
            @Path("id") String id,
            @Body Map<String, Object> body);

    /** Xóa món ăn */
    @DELETE("merchant/menu/{id}")
    Call<Map<String, Object>> deleteMenuItem(@Path("id") String id);

    // ✅ FIX: Revenue/Stats API
    /** Lấy doanh thu và thống kê */
    @GET("merchant/revenue")
    Call<Map<String, Object>> getRevenue(
            @Query("start_date") String startDate,
            @Query("end_date") String endDate);

    // ✅ Upload Image API
    /** Upload ảnh sản phẩm */
    @Multipart
    @POST("upload/image")
    Call<Map<String, Object>> uploadImage(@Part MultipartBody.Part image);
}
