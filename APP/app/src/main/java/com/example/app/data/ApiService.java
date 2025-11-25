package com.example.app.data;

import com.example.app.model.Restaurant;
import com.example.app.model.MenuItem;
import com.example.app.model.CartResponse;
import com.example.app.model.CartRequest;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Body;

/**
 * ApiService
 * - Định nghĩa các endpoint (tương tác với PGADMIN qua Backend Server).
 * - File này là một INTERFACE và phải được đặt trong file riêng biệt tên là ApiService.java
 */
public interface ApiService {

    // Lấy chi tiết Nhà hàng theo ID
    @GET("restaurants/{id}")
    Call<Restaurant> getRestaurantDetail(@Path("id") String restaurantId);

    // Lấy danh sách Menu của Nhà hàng
    @GET("restaurants/{id}/menu")
    Call<List<MenuItem>> getRestaurantMenu(@Path("id") String restaurantId);

    // Lấy trạng thái giỏ hàng hiện tại (bao gồm tổng tiền)
    @GET("cart/status/{userId}")
    Call<CartResponse> getCartStatus(@Path("userId") String userId);

    // Thêm món ăn vào giỏ hàng
    @POST("cart/add")
    Call<CartResponse> addItemToCart(@Body CartRequest request);
}
