package com.example.app.api;

import com.example.app.model.MenuItem;
import com.example.app.model.Restaurant;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Interface định nghĩa các lời gọi API liên quan đến Restaurant.
 */
public interface RestaurantApiService {

    // DANH SÁCH CỬA HÀNG
    // Ví dụ: GET http://10.0.2.2:8000/api/v1/restaurants
    @GET("api/v1/restaurants")
    Call<List<Restaurant>> getRestaurants();

    // DANH SÁCH CỬA HÀNG GẦN NHẤT (với location)
    // Ví dụ: GET http://10.0.2.2:8000/api/v1/restaurants/nearby?lat=10.762622&lng=106.660172
    @GET("api/v1/restaurants/nearby")
    Call<List<Restaurant>> getNearbyRestaurants(
            @retrofit2.http.Query("lat") double latitude,
            @retrofit2.http.Query("lng") double longitude
    );

    // CHI TIẾT 1 CỬA HÀNG
    // Ví dụ: GET http://10.0.2.2:8000/api/v1/restaurants/{id}
    @GET("api/v1/restaurants/{id}")
    Call<Restaurant> getRestaurantDetail(@Path("id") String restaurantId);

    // MENU CỦA 1 CỬA HÀNG
    // Ví dụ: GET http://10.0.2.2:8000/api/v1/restaurants/{id}/menu
    @GET("api/v1/restaurants/{id}/menu")
    Call<List<MenuItem>> getRestaurantMenu(@Path("id") String restaurantId);
}
