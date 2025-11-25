package com.example.app;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app.adapters.ShipperOrdersAdapter;
import com.example.app.databinding.FragmentShipperDashboardBinding;
import com.example.app.network.AuthClient;
import com.example.app.network.ShipperApi;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;

/**
 * ShipperDashboardFragment
 *
 * Tabs:
 * - available : đơn chưa ai nhận (trong khu vực)
 * - assigned : đơn đã gán cho tôi (chờ lấy tại quán)
 * - delivering: đang giao
 * - completed : đã hoàn tất
 *
 * Bổ sung:
 * - Xin quyền vị trí (+ POST_NOTIFICATIONS trên API 33+) và khởi chạy
 * LocationUpdateService (foreground)
 * với ACTION_START/STOP + EXTRA_INTERVAL_MS.
 * - Snackbar khi API lỗi kèm Retry.
 */
public class ShipperDashboardFragment extends Fragment {

    private static final long LOCATION_PUSH_INTERVAL_MS = 15_000L; // 15s

    private FragmentShipperDashboardBinding binding;

    private ShipperApi shipperApi;

    // Buckets
    private final List<Map<String, Object>> listAvailable = new ArrayList<>();
    private final List<Map<String, Object>> listDelivering = new ArrayList<>();
    private final List<Map<String, Object>> listCompleted = new ArrayList<>();

    // Adapters
    private ShipperOrdersAdapter adpAvailable;
    private ShipperOrdersAdapter adpDelivering;
    private ShipperOrdersAdapter adpCompleted;

    // --- Permission launcher for location + notifications (API 33+) ---
    private ActivityResultLauncher<String[]> locationPermsLauncher;

    // Avoid duplicate service starts
    private boolean locationServiceStarted = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentShipperDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shipperApi = new AuthClient(requireContext())
                .getRetrofit()
                .create(ShipperApi.class);

        // Prepare permission launcher
        locationPermsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    // POST_NOTIFICATIONS có thể bị từ chối — không chặn luồng
                    if (fine || coarse) {
                        startLocationServiceSafely();
                    } else {
                        showErrorSnack(
                                "Bạn đã từ chối quyền vị trí. Không thể cập nhật vị trí giao hàng.",
                                this::requestLocationPermissions);
                    }
                });

        setupTabs();

        // RecyclerViews
        binding.recyclerAvailable.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerDelivering.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));

        adpAvailable = new ShipperOrdersAdapter(requireContext(), listAvailable, callbacks("available"));
        adpDelivering = new ShipperOrdersAdapter(requireContext(), listDelivering, callbacks("delivering"));
        adpCompleted = new ShipperOrdersAdapter(requireContext(), listCompleted, callbacks("completed"));

        binding.recyclerAvailable.setAdapter(adpAvailable);
        binding.recyclerDelivering.setAdapter(adpDelivering);
        binding.recyclerCompleted.setAdapter(adpCompleted);

        // Refresh
        binding.swipeRefresh.setOnRefreshListener(this::refreshAll);

        // ✅ FIX: Nút "Hồ sơ"
        if (binding.btnProfile != null) {
            binding.btnProfile.setOnClickListener(v -> {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
                navController.navigate(R.id.action_shipperDashboardFragment_to_shipperProfileFragment);
            });
        }

        // ✅ FIX: Nút "Doanh thu"
        if (binding.btnRevenue != null) {
            binding.btnRevenue.setOnClickListener(v -> {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
                navController.navigate(R.id.action_shipperDashboardFragment_to_shipperRevenueFragment);
            });
        }

        // First load
        binding.swipeRefresh.setRefreshing(true);
        refreshAll();

        // Ask for permissions → start foreground service
        requestLocationPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Nếu trước đó đã cấp quyền mà service bị kill, đảm bảo bật lại
        startLocationServiceSafely();
        // ✅ FIX: Refresh lại danh sách khi quay lại từ OrderDetailFragment (sau khi nhận đơn)
        android.util.Log.d("ShipperDashboard", "onResume: Refreshing all buckets");
        refreshAll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Tuỳ business: dừng khi rời màn (giữ lại nếu bạn muốn theo dõi xuyên app)
        stopLocationServiceSafely();
        binding = null;
    }

    // ================= Tabs =================

    private void setupTabs() {
        TabLayout tl = binding.tabStatus;
        if (tl.getTabCount() == 0) {
            tl.addTab(tl.newTab().setText("Mới").setTag("available")); // Đơn hàng mới, có thể nhận
            tl.addTab(tl.newTab().setText("Đang giao").setTag("delivering")); // Đơn hàng đang giao
            tl.addTab(tl.newTab().setText("Xong").setTag("completed")); // Đơn hàng đã hoàn tất
        }

        // default show
        showOnly("available");

        tl.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tag = tab.getTag() != null ? tab.getTag().toString() : "available";
                showOnly(tag);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void showOnly(@NonNull String which) {
        int a = View.GONE, d = View.GONE, c = View.GONE;
        switch (which) {
            case "delivering":
                d = View.VISIBLE;
                break;
            case "completed":
                c = View.VISIBLE;
                break;
            case "available":
            default:
                a = View.VISIBLE;
                break;
        }
        binding.recyclerAvailable.setVisibility(a);
        binding.recyclerDelivering.setVisibility(d);
        binding.recyclerCompleted.setVisibility(c);

        updateEmpty(which);
    }

    private void updateEmpty(@NonNull String which) {
        if (binding.layoutEmpty == null)
            return;
        boolean empty;
        switch (which) {
            case "available":
                empty = listAvailable.isEmpty();
                break;
            case "delivering":
                empty = listDelivering.isEmpty();
                break;
            case "completed":
                empty = listCompleted.isEmpty();
                break;
            default:
                empty = true;
        }
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ================= Data =================

    private void refreshAll() {
        fetchBucket("available", listAvailable, adpAvailable);
        fetchBucket("delivering", listDelivering, adpDelivering);
        fetchBucket("completed", listCompleted, adpCompleted);
    }

    private void fetchBucket(
            @NonNull String status,
            @NonNull List<Map<String, Object>> target,
            @NonNull ShipperOrdersAdapter adapter) {

        android.util.Log.d("ShipperDashboard", "Fetching bucket: " + status);
        shipperApi.getShipperOrders(status).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<Map<String, Object>>> call,
                    @NonNull Response<List<Map<String, Object>>> response) {
                if (!isAdded())
                    return;

                if (response.isSuccessful() && response.body() != null) {
                    target.clear();
                    target.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    android.util.Log.d("ShipperDashboard", "Bucket " + status + " loaded: " + response.body().size() + " items");
                } else {
                    android.util.Log.e("ShipperDashboard", "Failed to load bucket " + status + ": " + response.code());
                    showErrorSnack("Không tải được danh sách (" + status + ").",
                            () -> fetchBucket(status, target, adapter));
                }
                stopRefreshingSoon();
                updateEmpty(status);
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<Map<String, Object>>> call,
                    @NonNull Throwable t) {
                if (!isAdded())
                    return;
                showErrorSnack(
                        "Lỗi mạng khi tải (" + status + "): " + (t.getMessage() != null ? t.getMessage() : "?"),
                        () -> fetchBucket(status, target, adapter));
                stopRefreshingSoon();
                updateEmpty(status);
            }
        });
    }

    private void stopRefreshingSoon() {
        if (binding != null && binding.swipeRefresh.isRefreshing()) {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private ShipperOrdersAdapter.Listener callbacks(@NonNull String bucket) {
        return new ShipperOrdersAdapter.Listener() {
            @Override
            public void onAccept(@NonNull Map<String, Object> order, int position) {
                String id = extractId(order);
                if (TextUtils.isEmpty(id))
                    return;

                shipperApi.acceptOrder(id).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;
                        if (response.isSuccessful()) {
                            if ("available".equals(bucket)) {
                                // ✅ FIX: Refresh lại bucket "delivering" để lấy đơn mới từ server
                                listAvailable.remove(position);
                                adpAvailable.notifyItemRemoved(position);
                                // Fetch lại bucket "delivering" từ server
                                fetchBucket("delivering", listDelivering, adpDelivering);
                                showOnly("delivering");
                            }
                            Toast.makeText(requireContext(), "Đã nhận đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            // ✅ FIX: Parse error body để hiển thị lỗi cụ thể
                            String errorMsg = "Nhận đơn thất bại";
                            if (response.errorBody() != null) {
                                try {
                                    String errorStr = response.errorBody().string();
                                    android.util.Log.e("ShipperDashboard", "Accept order error: " + errorStr);
                                    if (errorStr.contains("order_already_assigned")) {
                                        errorMsg = "Đơn hàng đã được nhận bởi shipper khác";
                                    } else if (errorStr.contains("order_not_ready")) {
                                        errorMsg = "Đơn hàng chưa sẵn sàng để nhận";
                                    } else if (errorStr.contains("shipper_not_available")) {
                                        errorMsg = "Shipper không available";
                                    } else if (errorStr.contains("not_a_shipper")) {
                                        errorMsg = "Bạn không phải shipper";
                                    } else if (errorStr.contains("order_not_found")) {
                                        errorMsg = "Không tìm thấy đơn hàng";
                                    } else {
                                        errorMsg = "Nhận đơn thất bại: " + errorStr;
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("ShipperDashboard", "Error reading error body", e);
                                }
                            }
                            showErrorSnack(errorMsg, () -> onAccept(order, position));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        showErrorSnack("Lỗi mạng khi nhận đơn: " + (t.getMessage() != null ? t.getMessage() : "?"),
                                () -> onAccept(order, position));
                    }
                });
            }

            @Override
            public void onArrived(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, position, bucket, "arrived_store");
            }

            @Override
            public void onPicked(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, position, bucket, "picked_up");
            }

            @Override
            public void onOnTheWay(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, position, bucket, "on_the_way");
            }

            @Override
            public void onDelivered(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, position, bucket, "delivered");
            }

            @Override
            public void onFailed(@NonNull Map<String, Object> order, int position, String reason) {
                // ✅ FIX: Nút "Thất bại" = hủy đơn, set status = CANCELED (không phải "failed")
                Map<String, Object> body = new HashMap<>();
                body.put("status", "canceled"); // Backend chỉ hỗ trợ CANCELED, không có "failed"
                if (!TextUtils.isEmpty(reason))
                    body.put("reason", reason);

                String id = extractId(order);
                if (TextUtils.isEmpty(id))
                    return;

                shipperApi.updateOrderStatus(id, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;
                        if (response.isSuccessful()) {
                            // failed → remove khỏi bucket hiện tại
                            removeAt(bucket, position);
                            showOnly(bucket);
                        } else {
                            showErrorSnack("Hủy đơn thất bại.",
                                    () -> onFailed(order, position, reason));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        showErrorSnack("Lỗi mạng khi cập nhật failed: "
                                + (t.getMessage() != null ? t.getMessage() : "?"),
                                () -> onFailed(order, position, reason));
                    }
                });
            }

            @Override
            public void onItemClick(@NonNull Map<String, Object> order, int position) {
                // ✅ FIX: Mở OrderDetailFragment để xem chi tiết đơn hàng
                Object orderIdObj = order.get("order_id");
                String orderId = orderIdObj != null ? orderIdObj.toString() : null;
                if (orderId != null) {
                    Bundle args = new Bundle();
                    args.putString("orderId", orderId);
                    androidx.navigation.fragment.NavHostFragment.findNavController(ShipperDashboardFragment.this)
                            .navigate(R.id.action_shipperDashboardFragment_to_orderDetailFragment, args);
                }
            }
        };
    }

    private void updateStatus(@NonNull Map<String, Object> order, int position,
            @NonNull String bucket, @NonNull String next) {
        String id = extractId(order);
        if (TextUtils.isEmpty(id))
            return;

        Map<String, Object> body = new HashMap<>();
        body.put("status", next);

        shipperApi.updateOrderStatus(id, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    @NonNull Call<Map<String, Object>> call,
                    @NonNull Response<Map<String, Object>> response) {
                if (!isAdded())
                    return;

                if (!response.isSuccessful()) {
                    showErrorSnack("Cập nhật trạng thái thất bại.",
                            () -> updateStatus(order, position, bucket, next));
                    return;
                }

                order.put("status", next);

                switch (next) {
                    case "delivered":
                        if ("delivering".equals(bucket)) {
                            moveItem(listDelivering, position, listCompleted);
                            adpDelivering.notifyItemRemoved(position);
                            adpCompleted.notifyItemInserted(listCompleted.size() - 1);
                            showOnly("completed");
                        } else if ("completed".equals(bucket)) {
                            adpCompleted.notifyItemChanged(position);
                        }
                        break;
                    default:
                        if ("delivering".equals(bucket)) {
                            adpDelivering.notifyItemChanged(position);
                        }
                        break;
                }

                updateEmpty(bucket);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded())
                    return;
                showErrorSnack("Lỗi mạng khi cập nhật: " + (t.getMessage() != null ? t.getMessage() : "?"),
                        () -> updateStatus(order, position, bucket, next));
            }
        });
    }

    private void removeAt(@NonNull String bucket, int pos) {
        switch (bucket) {
            case "available":
                removeAndNotify(listAvailable, pos, adpAvailable);
                break;
            case "delivering":
                removeAndNotify(listDelivering, pos, adpDelivering);
                break;
            case "completed":
                removeAndNotify(listCompleted, pos, adpCompleted);
                break;
        }
    }

    private static void removeAndNotify(List<Map<String, Object>> list, int pos, ShipperOrdersAdapter adp) {
        if (pos < 0 || pos >= list.size())
            return;
        list.remove(pos);
        adp.notifyItemRemoved(pos);
        adp.notifyItemRangeChanged(pos, Math.max(0, list.size() - pos));
    }

    private static void moveItem(List<Map<String, Object>> from, int pos, List<Map<String, Object>> to) {
        if (pos < 0 || pos >= from.size())
            return;
        Map<String, Object> item = from.remove(pos);
        to.add(item);
    }

    @Nullable
    private static String extractId(@NonNull Map<String, Object> order) {
        Object id = order.get("order_id");
        if (id == null)
            id = order.get("code");
        return id != null ? id.toString() : null;
    }

    // ================= Location permission + service =================

    private void requestLocationPermissions() {
        if (locationPermsLauncher == null)
            return;

        // Kiểm tra xem quyền đã được cấp chưa
        boolean hasFineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        
        if (hasFineLocation || hasCoarseLocation) {
            // Đã có quyền, khởi chạy service ngay
            startLocationServiceSafely();
            return;
        }

        // Chưa có quyền, luôn hiển thị dialog giải thích trước khi xin quyền
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Cần quyền vị trí")
                .setMessage("Ứng dụng cần quyền truy cập vị trí để:\n" +
                        "• Cập nhật vị trí của bạn cho khách hàng\n" +
                        "• Theo dõi tiến trình giao hàng\n" +
                        "• Tối ưu hóa tuyến đường giao hàng\n\n" +
                        "Vui lòng cấp quyền để tiếp tục sử dụng tính năng shipper.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    // Xin quyền sau khi user đồng ý
                    launchPermissionRequest();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    showErrorSnack(
                            "Bạn cần cấp quyền vị trí để sử dụng tính năng shipper.",
                            this::requestLocationPermissions);
                })
                .setCancelable(false)
                .show();
    }
    
    private void launchPermissionRequest() {
        if (locationPermsLauncher == null)
            return;
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermsLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            locationPermsLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationServiceSafely() {
        if (!isAdded() || locationServiceStarted)
            return;
        try {
            Intent it = new Intent(requireContext(), LocationUpdateService.class);
            it.setAction(LocationUpdateService.ACTION_START);
            it.putExtra(LocationUpdateService.EXTRA_INTERVAL_MS, LOCATION_PUSH_INTERVAL_MS);
            ContextCompat.startForegroundService(requireContext(), it);
            locationServiceStarted = true;
        } catch (Throwable t) {
            showErrorSnack("Không thể khởi chạy cập nhật vị trí: " + t.getMessage(),
                    this::requestLocationPermissions);
        }
    }

    private void stopLocationServiceSafely() {
        if (!isAdded() || !locationServiceStarted)
            return;
        try {
            Intent it = new Intent(requireContext(), LocationUpdateService.class);
            it.setAction(LocationUpdateService.ACTION_STOP);
            requireContext().startService(it);
        } catch (Throwable ignore) {
            // ignore
        } finally {
            locationServiceStarted = false;
        }
    }

    // ================= UX helpers =================

    private void showErrorSnack(@NonNull String msg, @Nullable Runnable action) {
        if (binding == null)
            return;
        Snackbar sb = Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG);
        if (action != null) {
            sb.setAction("Thử lại", v -> action.run());
        }
        sb.show();
    }
}
