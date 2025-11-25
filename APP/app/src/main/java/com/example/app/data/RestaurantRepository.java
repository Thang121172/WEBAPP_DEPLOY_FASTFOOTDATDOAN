package com.example.app.data;

import android.util.Log;

import com.example.app.api.RestaurantApiService;
import com.example.app.api.RetrofitClient;
import com.example.app.model.MenuItem;
import com.example.app.model.Restaurant;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RestaurantRepository
 * - Chịu trách nhiệm giao tiếp với API Backend (qua RestaurantApiService) để tải dữ liệu.
 * - Dùng chung RetrofitClient với StoreDiscoveryFragment.
 */
public class RestaurantRepository {

    private static final String TAG = "RestaurantRepository";
    private static RestaurantRepository instance;

    // ✅ Dùng RestaurantApiService (chung với StoreDiscoveryFragment)
    private final RestaurantApiService apiService;

    // Singleton Pattern
    public static RestaurantRepository getInstance() {
        if (instance == null) {
            instance = new RestaurantRepository();
        }
        return instance;
    }

    private RestaurantRepository() {
        // ✅ Lấy service từ RetrofitClient (đã chạy OK ở list cửa hàng)
        this.apiService = RetrofitClient.getRestaurantService();
    }

    // MARK: - Callbacks Interface (Được sử dụng trong ViewModel)
    public interface RestaurantDetailCallback {
        void onRestaurantLoaded(Restaurant restaurant);
        void onError(String message);
    }

    public interface MenuCallback {
        void onMenuLoaded(List<MenuItem> menuItems);
        void onError(String message);
    }

    // MARK: - Logic tải dữ liệu (Sử dụng API)

    /**
     * Tải chi tiết nhà hàng dựa trên ID từ API.
     */
    public void loadRestaurantDetail(String restaurantId, RestaurantDetailCallback callback) {
        Log.d(TAG, "Gọi API loadRestaurantDetail, id = " + restaurantId);

        apiService.getRestaurantDetail(restaurantId).enqueue(new Callback<Restaurant>() {
            @Override
            public void onResponse(Call<Restaurant> call, Response<Restaurant> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Load chi tiết nhà hàng OK");
                    callback.onRestaurantLoaded(response.body());
                } else {
                    String error = "Không tải được chi tiết nhà hàng. Code: " + response.code();
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<Restaurant> call, Throwable t) {
                String msg = "Lỗi kết nối API tải chi tiết nhà hàng: " + t.getMessage();
                Log.e(TAG, msg, t);
                callback.onError("Lỗi kết nối hoặc network: " + t.getMessage());
            }
        });
    }

    /**
     * Tải danh sách menu của nhà hàng từ API.
     */
    public void loadMenu(String restaurantId, MenuCallback callback) {
        Log.d(TAG, "Gọi API loadMenu, id = " + restaurantId);

        apiService.getRestaurantMenu(restaurantId).enqueue(new Callback<List<MenuItem>>() {
            @Override
            public void onResponse(Call<List<MenuItem>> call, Response<List<MenuItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Load menu OK, size = " + response.body().size());
                    callback.onMenuLoaded(response.body());
                } else {
                    String error = "Không tải được menu. Code: " + response.code();
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<List<MenuItem>> call, Throwable t) {
                String msg = "Lỗi kết nối API tải menu: " + t.getMessage();
                Log.e(TAG, msg, t);
                callback.onError("Lỗi kết nối hoặc network: " + t.getMessage());
            }
        });
    }
}
