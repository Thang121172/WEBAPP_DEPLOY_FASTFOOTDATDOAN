package com.example.app.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * CustomerApi
 *
 * Base URL: BackendConfig -> http://<ip>:8000/api/
 * Các path ở đây là tương đối.
 *
 * Lưu ý:
 * - Authorization/Content-Type đã được gắn bởi OkHttp Interceptor
 * (BackendConfig/AuthClient).
 * - Dùng Map<String,Object> để giữ linh hoạt schema phía backend.
 */
public interface CustomerApi {

    /* ===== PROFILE ===== */

    /** Lấy hồ sơ khách hàng đã đăng nhập */
    @Headers("Accept: application/json")
    @GET("customer/profile")
    Call<Map<String, Object>> getProfile();

    /** Cập nhật hồ sơ (ví dụ: name, phone, avatar...) */
    @Headers("Accept: application/json")
    @PATCH("customer/profile")
    Call<Map<String, Object>> updateProfile(@Body Map<String, Object> body);

    /* ===== ADDRESSES ===== */

    /** Danh sách địa chỉ giao hàng của khách */
    @Headers("Accept: application/json")
    @GET("customer/addresses")
    Call<List<Map<String, Object>>> getAddresses();

    /** Tạo địa chỉ mới */
    @Headers("Accept: application/json")
    @POST("customer/addresses")
    Call<Map<String, Object>> createAddress(@Body Map<String, Object> body);

    /** Cập nhật một địa chỉ */
    @Headers("Accept: application/json")
    @PATCH("customer/addresses/{id}")
    Call<Map<String, Object>> updateAddress(
            @Path("id") String addressId,
            @Body Map<String, Object> body);

    /** Xoá một địa chỉ */
    @Headers("Accept: application/json")
    @DELETE("customer/addresses/{id}")
    Call<Map<String, Object>> deleteAddress(@Path("id") String addressId);

    /* ===== FAVORITES (store/item) ===== */

    /** Lấy danh sách yêu thích (lọc bằng type=item|store nếu cần) */
    @Headers("Accept: application/json")
    @GET("customer/favorites")
    Call<List<Map<String, Object>>> getFavorites(@Query("type") String type);

    /** Thêm vào yêu thích: body ví dụ { "type": "item", "id": "123" } */
    @Headers("Accept: application/json")
    @POST("customer/favorites")
    Call<Map<String, Object>> addFavorite(@Body Map<String, Object> body);

    /** Bỏ yêu thích */
    @Headers("Accept: application/json")
    @DELETE("customer/favorites/{type}/{id}")
    Call<Map<String, Object>> removeFavorite(
            @Path("type") String type, // item|store
            @Path("id") String id);

    /* ===== DEVICE TOKEN / PUSH ===== */

    /** Đăng ký device token (FCM...) để nhận push từ server */
    @Headers("Accept: application/json")
    @POST("customer/device-token")
    Call<Map<String, Object>> registerDeviceToken(@Body Map<String, Object> body);

    /* ===== ORDERS ===== */

    /** Lấy danh sách đơn hàng của khách (lọc theo trạng thái) */
    @Headers("Accept: application/json")
    @GET("customer/orders")
    Call<List<Map<String, Object>>> getOrders(@Query("status") String status);

    /**
     * Huỷ đơn hàng.
     * LƯU Ý: Giữ nguyên PATCH /customer/orders/{id}/cancel theo backend hiện tại
     * của bạn.
     * Nếu server của bạn dùng POST thay vì PATCH, đổi annotation sang @POST.
     * Nếu server yêu cầu PATCH /customer/orders/{id} với body
     * {"status":"cancelled"}, đổi path tương ứng.
     */
    @Headers("Accept: application/json")
    @PATCH("customer/orders/{id}/cancel")
    Call<Map<String, Object>> cancelOrder(
            @Path("id") String orderId,
            @Body Map<String, Object> body);
}
