package com.example.app.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * MenuApi
 *
 * Base URL: BackendConfig -> http://<host>:8000/api/
 * Các path bên dưới là đường dẫn tương đối.
 *
 * Lưu ý:
 * - Authorization + Content-Type đã được gắn qua OkHttp Interceptor
 * (BackendConfig/AuthClient).
 * - Nếu backend dùng "lng" thay vì "lon", đổi @Query tương ứng hoặc map cả hai
 * phía server.
 */
public interface MenuApi {

    /* ===== FEATURED / FALLBACK ===== */

    /** Danh sách món nổi bật (có thể phân trang nếu backend hỗ trợ) */
    @Headers("Accept: application/json")
    @GET("menus/featured")
    Call<List<Map<String, Object>>> getFeaturedItems(
            @Query("page") Integer page,
            @Query("page_size") Integer pageSize);

    /** Overload giữ tương thích cũ (không truyền phân trang) */
    @Headers("Accept: application/json")
    @GET("menus/featured")
    Call<List<Map<String, Object>>> getFeaturedItems();

    /* ===== NEARBY ===== */

    /**
     * Món/cửa hàng gần vị trí hiện tại.
     * Chú ý: nếu backend dùng "lng" thay vì "lon", hãy đổi @Query("lon")
     * -> @Query("lng").
     */
    @Headers("Accept: application/json")
    @GET("menus/nearby")
    Call<List<Map<String, Object>>> getNearbyStores(
            @Query("lat") Double latitude,
            @Query("lon") Double longitude);

    /* ===== FULL MENU / CATEGORIES ===== */

    /** Toàn bộ menu (tuỳ backend), có thể kèm phân trang để tránh tải lớn */
    @Headers("Accept: application/json")
    @GET("menus")
    Call<List<Map<String, Object>>> getMenu(
            @Query("page") Integer page,
            @Query("page_size") Integer pageSize);

    /** Overload giữ tương thích cũ */
    @Headers("Accept: application/json")
    @GET("menus")
    Call<List<Map<String, Object>>> getMenu();

    /* ===== STORE-SCOPED MENU ===== */

    /** Lấy menu theo cửa hàng (store) nếu backend có tách theo merchant */
    @Headers("Accept: application/json")
    @GET("stores/{storeId}/menus")
    Call<List<Map<String, Object>>> getStoreMenu(
            @Path("storeId") String storeId,
            @Query("page") Integer page,
            @Query("page_size") Integer pageSize);

    /* ===== SEARCH / DETAIL ===== */

    /** Tìm kiếm món/cửa hàng theo từ khoá */
    @Headers("Accept: application/json")
    @GET("menus/search")
    Call<List<Map<String, Object>>> searchMenus(
            @Query("q") String query,
            @Query("page") Integer page,
            @Query("page_size") Integer pageSize);

    /** Lấy chi tiết một món */
    @Headers("Accept: application/json")
    @GET("menus/{itemId}")
    Call<Map<String, Object>> getMenuItem(
            @Path("itemId") String itemId);
}
