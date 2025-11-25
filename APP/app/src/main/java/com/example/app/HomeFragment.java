package com.example.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.util.Log;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.adapters.MenuAdapter;
import com.example.app.adapters.OrdersAdapter;
import com.example.app.model.MenuItem;
import com.example.app.model.OrderSummary;
import com.example.app.network.BackendConfig;
import com.example.app.network.MenuApi;
import com.example.app.network.AuthClient;
import com.example.app.network.OrdersApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment
 * - Xin quyền GPS
 * - Lấy last known location
 * - Gọi API nearby hoặc fallback featured
 * - Hiển thị đơn gần đây + menu gợi ý (không mock)
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // location
    private ActivityResultLauncher<String[]> requestLocationPermsLauncher;
    private boolean askedLocationYet = false;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;

    // UI refs
    private TextView tvWelcome;
    private TextView tvLocationStatus;
    private RecyclerView recyclerOrders;
    private RecyclerView recyclerMenu;
    private Button btnCheckout;
    private View btnLogout; // ✅ ĐÃ THÊM: Nút Đăng Xuất

    // data
    private final List<OrderSummary> orderItems = new ArrayList<>();
    private OrdersAdapter ordersAdapter;

    private final List<MenuItem> menuItems = new ArrayList<>();
    private MenuAdapter menuAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(requireActivity());

        // request permissions launcher
        requestLocationPermsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if ((fineGranted != null && fineGranted)
                            || (coarseGranted != null && coarseGranted)) {
                        getLastLocation();
                    } else {
                        Toast.makeText(
                                getContext(),
                                "Cần quyền vị trí để tìm cửa hàng gần bạn.",
                                Toast.LENGTH_LONG).show();
                        loadNearbyItems(null, null);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcome = v.findViewById(R.id.tv_welcome);
        tvLocationStatus = v.findViewById(R.id.tv_location_status);
        recyclerOrders = v.findViewById(R.id.recycler_orders);
        recyclerMenu = v.findViewById(R.id.recycler_menu);
        btnCheckout = v.findViewById(R.id.btn_checkout);
        btnLogout = v.findViewById(R.id.btn_logout); // ✅ ÁNH XẠ: Nút Đăng Xuất

        // greeting user
        AuthClient authClient = new AuthClient(requireContext());
        String role = authClient.getRole();
        String email = authClient.getEmail();

        if (tvWelcome != null) {
            String who = (email != null && email.length() > 0) ? email : "bạn";
            if (role != null && role.length() > 0) {
                tvWelcome.setText("Xin chào " + who + " (" + role + ")");
            } else {
                tvWelcome.setText("Xin chào " + who);
            }
        }

        // Recent orders list: HORIZONTAL
        ordersAdapter = new OrdersAdapter(orderItems, order -> {
            Toast.makeText(getContext(),
                    "Mở chi tiết đơn #" + order.orderId,
                    Toast.LENGTH_SHORT).show();
        });
        recyclerOrders.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerOrders.setAdapter(ordersAdapter);

        // Menu list
        menuAdapter = new MenuAdapter(menuItems, item -> {
            Toast.makeText(
                    getContext(),
                    "Chọn món: " + item.title,
                    Toast.LENGTH_SHORT).show();
        });
        recyclerMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMenu.setAdapter(menuAdapter);

        // Checkout demo (sẽ nối Orders API đặt hàng thật ở bước sau)
        btnCheckout.setOnClickListener(v1 -> placeTestOrder());

        // ✅ LOGIC ĐĂNG XUẤT
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v1 -> performLogout());
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!askedLocationYet) {
            askedLocationYet = true;
            checkLocationPermissionAndGetLocation();
        } else {
            // refresh đơn gần đây mỗi lần quay lại
            fetchRecentOrders();
        }
    }

    /**
     * ✅ PHƯƠNG THỨC ĐĂNG XUẤT: Xóa token và điều hướng về màn hình Login.
     */
    private void performLogout() {
        // 1. Xóa tất cả token/session
        BackendConfig.clearAccessToken(requireContext());
        BackendConfig.clearAllSession(requireContext());

        // 2. Reset Retrofit instance
        BackendConfig.resetRetrofit();

        // 3. Điều hướng về màn hình đăng nhập
        try {
            NavController nav = NavHostFragment.findNavController(HomeFragment.this);
            // Sử dụng ID của LoginFragment (đảm bảo ID này đúng trong nav_graph.xml)
            nav.navigate(R.id.loginFragment);

            Toast.makeText(getContext(), "Đã đăng xuất thành công.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Nav to login failed after logout: " + e.getMessage());
            Toast.makeText(getContext(), "Lỗi điều hướng sau khi đăng xuất.", Toast.LENGTH_SHORT).show();
        }
    }
    // ----------------------------

    private void checkLocationPermissionAndGetLocation() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            getLastLocation();
        } else {
            requestLocationPermsLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    // Luôn gọi đơn gần đây (không phụ thuộc GPS)
                    fetchRecentOrders();

                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        Log.d(TAG, "Vị trí: Lat=" + lat + ", Lon=" + lon);

                        if (tvLocationStatus != null) {
                            tvLocationStatus.setText(
                                    "Vị trí của bạn: " + lat + ", " + lon);
                        }

                        loadNearbyItems(lat, lon);
                    } else {
                        Toast.makeText(
                                getContext(),
                                "Không thể lấy vị trí hiện tại. Vui lòng bật GPS.",
                                Toast.LENGTH_SHORT).show();

                        if (tvLocationStatus != null) {
                            tvLocationStatus.setText(
                                    "Không lấy được vị trí. Đang tải menu nổi bật...");
                        }

                        loadNearbyItems(null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    fetchRecentOrders();
                    if (tvLocationStatus != null) {
                        tvLocationStatus.setText("Không lấy được vị trí. Đang tải menu nổi bật...");
                    }
                    loadNearbyItems(null, null);
                });
    }

    // ====================== Recent Orders (API thật) ======================
    private void fetchRecentOrders() {
        OrdersApi ordersApi = BackendConfig.getRetrofit(requireContext()).create(OrdersApi.class);
        ordersApi.getRecentOrders(10).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call,
                                   @NonNull Response<List<Map<String, Object>>> response) {
                if (!isAdded())
                    return;

                // Xử lý 401 (Unauthorized) nếu refresh token bị lỗi
                if (response.code() == 401) {
                    Toast.makeText(getContext(), "Phiên đăng nhập hết hạn. Đang đăng xuất...", Toast.LENGTH_LONG).show();
                    performLogout();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    applyRecentOrders(response.body());
                } else {
                    Log.w(TAG, "Không tải được đơn gần đây: HTTP " + response.code());
                    // Vẫn hiển thị UI bình thường nếu lỗi, chỉ log warning
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call,
                                  @NonNull Throwable t) {
                if (!isAdded())
                    return;
                Log.e(TAG, "Lỗi mạng khi tải đơn gần đây: " + t.getMessage());
            }
        });
    }

    private void applyRecentOrders(@NonNull List<Map<String, Object>> raw) {
        orderItems.clear();
        for (Map<String, Object> m : raw) {
            String code = str(m.get("order_id"));
            if (code == null || code.isEmpty())
                code = str(m.get("code"));
            if (code == null)
                code = "";

            String status = str(m.get("status"));
            if (status == null)
                status = "";

            long total = 0;
            Object totalObj = m.get("total");
            if (totalObj instanceof Number) {
                total = ((Number) totalObj).longValue();
            } else if (totalObj != null) {
                try {
                    String s = totalObj.toString().replaceAll("[^0-9]", "");
                    total = s.isEmpty() ? 0 : Long.parseLong(s);
                } catch (Exception ignore) {
                }
            }

            String time = str(m.get("created_at"));
            if (time == null)
                time = str(m.get("time"));
            if (time == null)
                time = "";

            orderItems.add(new OrderSummary(code, status, total, time));
        }
        ordersAdapter.notifyDataSetChanged();
    }

    // ====================== Menu (nearby / featured) ======================
    private void loadNearbyItems(@Nullable Double lat, @Nullable Double lon) {
        MenuApi menuApi = BackendConfig.getRetrofit(requireContext()).create(MenuApi.class);

        if (lat != null && lon != null) {
            if (tvLocationStatus != null)
                tvLocationStatus.setText("Đang tìm cửa hàng gần bạn...");

            menuApi.getNearbyStores(lat, lon)
                    .enqueue(new Callback<List<Map<String, Object>>>() {
                        @Override
                        public void onResponse(
                                Call<List<Map<String, Object>>> call,
                                Response<List<Map<String, Object>>> response) {
                            if (!isAdded())
                                return;

                            // ✅ KHẮC PHỤC LỖI: Xử lý response.body() == null khi DB trống/lỗi.
                            if (response.isSuccessful()) {
                                List<Map<String, Object>> data = response.body();
                                if (data == null) data = new ArrayList<>(); // Nếu body null, coi là list rỗng.

                                Log.d(TAG, "Nearby OK, count=" + data.size());
                                if (tvLocationStatus != null) {
                                    tvLocationStatus.setText(
                                            "Đã tìm thấy " + data.size() + " địa điểm gần bạn.");
                                }
                                applyMenuData(data);
                            } else {
                                // Nếu có lỗi HTTP (4xx, 5xx), chuyển sang tải Featured
                                Log.w(TAG, "Nearby API fail: HTTP " + response.code());
                                if (tvLocationStatus != null) {
                                    tvLocationStatus.setText("Lỗi server. Đang tải menu nổi bật...");
                                }
                                loadFeaturedItems(menuApi);
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<List<Map<String, Object>>> call,
                                Throwable t) {
                            if (!isAdded())
                                return;
                            Log.e(TAG, "Nearby API fail: " + t.getMessage());
                            if (tvLocationStatus != null) {
                                tvLocationStatus.setText("Lỗi mạng/server. Đang tải menu nổi bật...");
                            }
                            loadFeaturedItems(menuApi);
                        }
                    });

        } else {
            if (tvLocationStatus != null) {
                tvLocationStatus.setText("Không lấy được vị trí. Đang tải các món nổi bật...");
            }
            loadFeaturedItems(menuApi);
        }
    }

    private void loadFeaturedItems(MenuApi menuApi) {
        menuApi.getFeaturedItems()
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response) {
                        if (!isAdded())
                            return;

                        // ✅ KHẮC PHỤC LỖI: Xử lý response.body() == null khi DB trống/lỗi.
                        if (response.isSuccessful()) {
                            List<Map<String, Object>> data = response.body();
                            if (data == null) data = new ArrayList<>(); // Nếu body null, coi là list rỗng.

                            Log.d(TAG, "Featured OK, count=" + data.size());
                            if (tvLocationStatus != null) {
                                tvLocationStatus.setText("Menu nổi bật (" + data.size() + " món).");
                            }
                            applyMenuData(data);
                        } else {
                            Log.w(TAG, "Featured API fail: HTTP " + response.code());
                            if (tvLocationStatus != null) {
                                tvLocationStatus.setText("Không thể tải menu mặc định (Lỗi HTTP " + response.code() + ").");
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t) {
                        if (!isAdded())
                            return;
                        Log.e(TAG, "Featured API fail: " + t.getMessage());
                        if (tvLocationStatus != null) {
                            tvLocationStatus.setText("Lỗi: Không thể kết nối với server. (Kiểm tra IP/Cổng)");
                        }
                    }
                });
    }

    private void applyMenuData(List<Map<String, Object>> rawList) {
        menuItems.clear();
        for (Map<String, Object> row : rawList) {
            // id
            String id = null;
            if (row.get("id") != null)
                id = row.get("id").toString();
            else if (row.get("_id") != null)
                id = row.get("_id").toString();

            // title
            String title = null;
            if (row.get("title") != null)
                title = row.get("title").toString();
            else if (row.get("name") != null)
                title = row.get("name").toString();
            else if (row.get("item_name") != null)
                title = row.get("item_name").toString();
            if (title == null || title.isEmpty())
                title = "Món";

            // description
            String desc = null;
            if (row.get("description") != null)
                desc = row.get("description").toString();
            else if (row.get("desc") != null)
                desc = row.get("desc").toString();
            if (desc == null)
                desc = "";

            // price
            double price = 0.0;
            Object pObj = null;
            if (row.get("price") != null)
                pObj = row.get("price");
            else if (row.get("amount") != null)
                pObj = row.get("amount");
            else if (row.get("unit_price") != null)
                pObj = row.get("unit_price");

            if (pObj instanceof Number) {
                price = ((Number) pObj).doubleValue();
            } else if (pObj != null) {
                try {
                    String s = pObj.toString().replaceAll("[^0-9.]", "");
                    price = s.isEmpty() ? 0.0 : Double.parseDouble(s);
                } catch (Exception ignore) {
                }
            }

            menuItems.add(new MenuItem(id, title, desc, price, null));
        }
        menuAdapter.notifyDataSetChanged();
    }

    private void placeTestOrder() {
        Toast.makeText(
                getContext(),
                "Checkout demo (sẽ nối OrdersApi ở bước sau).",
                Toast.LENGTH_SHORT).show();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}