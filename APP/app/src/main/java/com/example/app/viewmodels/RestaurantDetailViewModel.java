package com.example.app.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.app.data.CartRepository;
import com.example.app.data.RestaurantRepository;
import com.example.app.model.MenuItem;
import com.example.app.model.Restaurant;

import java.math.BigDecimal;
import java.util.List;

/**
 * RestaurantDetailViewModel
 * - Quản lý trạng thái dữ liệu cho RestaurantDetailFragment.
 * - Tải chi tiết nhà hàng và danh sách Menu.
 */
public class RestaurantDetailViewModel extends ViewModel {

    private static final String TAG = "RestaurantDetailVM";

    private final RestaurantRepository restaurantRepository = RestaurantRepository.getInstance();
    private final CartRepository cartRepository = CartRepository.getInstance();

    private final MutableLiveData<Restaurant> _restaurantDetail = new MutableLiveData<>();
    public LiveData<Restaurant> getRestaurantDetail() {
        return _restaurantDetail;
    }

    private final MutableLiveData<List<MenuItem>> _menuList = new MutableLiveData<>();
    public LiveData<List<MenuItem>> getMenuList() {
        return _menuList;
    }

    // 👉 LiveData báo lỗi để Fragment hiển thị
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    /**
     * Tổng tiền giỏ hàng (BigDecimal) – convert từ LiveData<Double> trong CartRepository
     */
    public LiveData<BigDecimal> getCartTotal() {
        LiveData<Double> subtotalDouble = cartRepository.getCartSubtotal();
        return Transformations.map(subtotalDouble, totalDouble -> {
            if (totalDouble == null) return BigDecimal.ZERO;
            return BigDecimal.valueOf(totalDouble);
        });
    }

    /**
     * Tải chi tiết nhà hàng và menu.
     * @param restaurantId ID của nhà hàng cần tải.
     */
    public void loadRestaurantDetail(String restaurantId) {
        Log.d(TAG, "loadRestaurantDetail() với restaurantId = " + restaurantId);

        // Tải chi tiết nhà hàng
        restaurantRepository.loadRestaurantDetail(restaurantId, new RestaurantRepository.RestaurantDetailCallback() {
            @Override
            public void onRestaurantLoaded(Restaurant restaurant) {
                Log.d(TAG, "onRestaurantLoaded: " + (restaurant != null ? restaurant.getName() : "null"));
                _restaurantDetail.setValue(restaurant);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Lỗi loadRestaurantDetail: " + message);
                _errorMessage.setValue(message);
            }
        });

        // Tải danh sách menu
        restaurantRepository.loadMenu(restaurantId, new RestaurantRepository.MenuCallback() {
            @Override
            public void onMenuLoaded(List<MenuItem> menuItems) {
                Log.d(TAG, "onMenuLoaded: " + (menuItems != null ? menuItems.size() : 0) + " items");
                _menuList.setValue(menuItems);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Lỗi loadMenu: " + message);
                _errorMessage.setValue(message);
            }
        });
    }

    // ✅ HÀM MỚI: dùng cho nút "Thêm" trong RestaurantDetailFragment
    public void addMenuItemToCart(MenuItem item) {
        if (item == null) return;
        // Lấy khoảng cách từ restaurant detail nếu có
        Restaurant restaurant = _restaurantDetail.getValue();
        Double distance = (restaurant != null) ? restaurant.getDistance() : null;
        cartRepository.addToCart(item, distance);
    }
}
