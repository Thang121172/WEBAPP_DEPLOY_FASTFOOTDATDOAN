package com.example.app.ui.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app.R;
import com.example.app.adapters.RestaurantAdapter;
import com.example.app.api.RestaurantApiService;
import com.example.app.api.RetrofitClient;
import com.example.app.databinding.FragmentStoreDiscoveryBinding;
import com.example.app.model.Restaurant;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * StoreDiscoveryFragment
 * Tải và hiển thị danh sách các Quán ăn (Restaurant) bằng cách gọi API Retrofit.
 */
public class StoreDiscoveryFragment extends Fragment {

    private FragmentStoreDiscoveryBinding binding;
    private RestaurantAdapter restaurantAdapter;
    private final List<Restaurant> restaurantList = new ArrayList<>();
    private RestaurantApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Khởi tạo View Binding
        binding = FragmentStoreDiscoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo Retrofit Service
        apiService = RetrofitClient.getRestaurantService();

        // Khởi tạo Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Khởi tạo Adapter + callback khi click vào 1 quán
        restaurantAdapter = new RestaurantAdapter(
                requireContext(),
                restaurantList,
                restaurant -> {
                    // Điều hướng sang màn chi tiết quán ăn
                    NavController navController = NavHostFragment.findNavController(this);

                    Bundle args = new Bundle();
                    // restaurantId là String -> dùng putString
                    args.putString("restaurantId", restaurant.getId());

                    // Điều hướng trực tiếp tới destination RestaurantDetailFragment
                    navController.navigate(R.id.restaurantDetailFragment, args);
                }
        );

        // Cấu hình RecyclerView
        binding.recyclerViewStores.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewStores.setAdapter(restaurantAdapter);

        // Tải dữ liệu lần đầu
        fetchData();

        // Xử lý nút Thử lại (btnRetry)
        if (binding.btnRetry != null) {
            binding.btnRetry.setOnClickListener(v -> fetchData());
        }
    }

    /**
     * Bắt đầu quá trình tải dữ liệu bằng cách gọi API.
     * Tự động lấy vị trí GPS và đề xuất cửa hàng gần nhất.
     */
    private void fetchData() {
        showLoadingState();
        
        // Kiểm tra quyền truy cập vị trí
        if (checkLocationPermission()) {
            // Có quyền -> Lấy vị trí và tải cửa hàng gần nhất
            getCurrentLocationAndLoadRestaurants();
        } else {
            // Chưa có quyền -> Yêu cầu quyền
            requestLocationPermission();
        }
    }

    /**
     * Kiểm tra quyền truy cập vị trí
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Yêu cầu quyền truy cập vị trí
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Lấy vị trí hiện tại và tải cửa hàng gần nhất
     */
    private void getCurrentLocationAndLoadRestaurants() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Không có quyền -> Tải tất cả cửa hàng
            loadRestaurantsFromApi();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Có vị trí -> Tải cửa hàng gần nhất
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();
                            Log.d("StoreDiscovery", "Location: " + lat + ", " + lng);
                            loadNearbyRestaurantsFromApi(lat, lng);
                        } else {
                            // Không có vị trí -> Tải tất cả cửa hàng
                            Log.w("StoreDiscovery", "Location is null, loading all restaurants");
                            loadRestaurantsFromApi();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("StoreDiscovery", "Failed to get location: " + e.getMessage());
                    // Lỗi lấy vị trí -> Tải tất cả cửa hàng
                    loadRestaurantsFromApi();
                });
    }

    /**
     * Tải cửa hàng gần nhất dựa trên vị trí
     */
    private void loadNearbyRestaurantsFromApi(double lat, double lng) {
        Call<List<Restaurant>> call = apiService.getNearbyRestaurants(lat, lng);

        call.enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<Restaurant>> call,
                    @NonNull Response<List<Restaurant>> response
            ) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> fetchedData = response.body();

                    if (fetchedData.isEmpty()) {
                        showEmptyState("Không tìm thấy quán ăn nào gần bạn. Hãy thử lại sau.");
                    } else {
                        restaurantAdapter.updateData(fetchedData);
                        showContentState();
                    }
                } else {
                    // Nếu API nearby thất bại, thử tải tất cả
                    Log.w("StoreDiscovery", "Nearby API failed, loading all restaurants");
                    loadRestaurantsFromApi();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<Restaurant>> call,
                    @NonNull Throwable t
            ) {
                if (binding == null) return;
                Log.e("StoreDiscovery", "Nearby API failure: " + t.getMessage());
                // Nếu API nearby thất bại, thử tải tất cả
                loadRestaurantsFromApi();
            }
        });
    }

    /**
     * Thực hiện cuộc gọi API sử dụng Retrofit.
     */
    private void loadRestaurantsFromApi() {
        Call<List<Restaurant>> call = apiService.getRestaurants();

        call.enqueue(new Callback<List<Restaurant>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<Restaurant>> call,
                    @NonNull Response<List<Restaurant>> response
            ) {
                if (binding == null) return; // Bảo vệ khỏi lỗi onDestroyView

                if (response.isSuccessful() && response.body() != null) {
                    List<Restaurant> fetchedData = response.body();

                    if (fetchedData.isEmpty()) {
                        showEmptyState("Không tìm thấy quán ăn nào. Hãy thử lại sau.");
                    } else {
                        restaurantAdapter.updateData(fetchedData);
                        showContentState();
                    }
                } else {
                    showErrorState("Lỗi HTTP " + response.code() + ": Không thể tải dữ liệu.");
                    Log.e("StoreDiscovery", "API Response Error: "
                            + response.code() + ", Message: " + response.message());
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<Restaurant>> call,
                    @NonNull Throwable t
            ) {
                if (binding == null) return;
                showErrorState("Lỗi kết nối mạng: Vui lòng đảm bảo server đang chạy trên cổng 8000.");
                Log.e("StoreDiscovery", "API Connection Failure: " + t.getMessage(), t);
            }
        });
    }

    // --- CÁC HÀM HIỂN THỊ TRẠNG THÁI ---

    private void showLoadingState() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerViewStores.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
        if (binding.tvErrorMessage != null) binding.tvErrorMessage.setVisibility(View.GONE);
        if (binding.btnRetry != null) binding.btnRetry.setVisibility(View.GONE);
    }

    private void showContentState() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewStores.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
        if (binding.tvErrorMessage != null) binding.tvErrorMessage.setVisibility(View.GONE);
        if (binding.btnRetry != null) binding.btnRetry.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewStores.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
        if (binding.tvErrorMessage != null) {
            binding.tvErrorMessage.setVisibility(View.VISIBLE);
            binding.tvErrorMessage.setText(message);
        }
        if (binding.btnRetry != null) binding.btnRetry.setVisibility(View.VISIBLE);
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void showEmptyState(String message) {
        showErrorState(message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền -> Lấy vị trí và tải cửa hàng
                getCurrentLocationAndLoadRestaurants();
            } else {
                // Từ chối quyền -> Tải tất cả cửa hàng
                Toast.makeText(requireContext(), "Không có quyền truy cập vị trí. Hiển thị tất cả cửa hàng.", Toast.LENGTH_SHORT).show();
                loadRestaurantsFromApi();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
