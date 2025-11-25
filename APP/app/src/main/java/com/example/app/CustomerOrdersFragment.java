package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.app.adapters.CustomerOrdersAdapter;
import com.example.app.databinding.FragmentCustomerOrdersBinding;
import com.example.app.network.BackendConfig;
import com.example.app.network.OrderTracker;
import com.example.app.network.OrdersApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CustomerOrdersFragment: Hiển thị danh sách đơn hàng của khách hàng
 */
public class CustomerOrdersFragment extends Fragment {

    private static final String TAG = "CustomerOrdersFragment";
    private FragmentCustomerOrdersBinding binding;
    private CustomerOrdersAdapter adapter;
    private final List<Map<String, Object>> orders = new ArrayList<>();
    private OrderTracker orderTracker;
    private String backendBase;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCustomerOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });

        // Setup RecyclerView
        adapter = new CustomerOrdersAdapter(requireContext(), orders, new CustomerOrdersAdapter.Listener() {
            @Override
            public void onItemClick(@NonNull Map<String, Object> order, int position) {
                // Điều hướng đến chi tiết đơn hàng
                Object orderIdObj = order.get("order_id");
                String orderId = orderIdObj != null ? orderIdObj.toString() : null;
                if (orderId != null) {
                    Bundle args = new Bundle();
                    args.putString("orderId", orderId);
                    NavHostFragment.findNavController(CustomerOrdersFragment.this)
                            .navigate(R.id.action_customerOrdersFragment_to_orderDetailFragment, args);
                }
            }

            @Override
            public void onCancel(@NonNull Map<String, Object> order, int position, @Nullable String reason) {
                // ✅ FIX: Hiển thị dialog xác nhận trước khi hủy
                Object orderIdObj = order.get("order_id");
                String orderId = orderIdObj != null ? orderIdObj.toString() : null;
                if (orderId == null) {
                    Toast.makeText(requireContext(), "Không tìm thấy ID đơn hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Hiển thị dialog xác nhận
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Xác nhận hủy đơn")
                        .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + orderId + "?")
                        .setPositiveButton("Hủy đơn", (dialog, which) -> {
                            cancelOrder(orderId, reason, position);
                        })
                        .setNegativeButton("Không", null)
                        .show();
            }
        });

        binding.recyclerOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerOrders.setAdapter(adapter);

        // Setup SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener(this::loadOrders);

        // ✅ FIX: Setup socket để nhận update real-time từ merchant
        backendBase = BackendConfig.getRetrofitRoot(requireContext()).baseUrl().toString();
        orderTracker = new OrderTracker(requireContext(), backendBase);
        orderTracker.setAuthToken(BackendConfig.getAccessToken(requireContext()));
        orderTracker.connect();
        
        // ✅ FIX: Identify user để join vào user room khi socket connect
        String token = BackendConfig.getAccessToken(requireContext());
        final String userId = extractUserIdFromToken(token);
        final String role = new com.example.app.network.AuthClient(requireContext()).getRole();
        if (userId != null) {
            // Lắng nghe event connect để identify user
            orderTracker.on(io.socket.client.Socket.EVENT_CONNECT, args -> {
                if (userId != null) {
                    orderTracker.identify(userId, role);
                }
            });
        }
        
        // Lắng nghe update cho tất cả đơn hàng của user
        orderTracker.on("orderUpdate", args -> {
            if (args != null && args.length > 0) {
                Object o = args[0];
                try {
                    Log.i(TAG, "Received orderUpdate: " + o);
                    if (!isAdded())
                        return;
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null)
                            return;
                        try {
                            if (o instanceof Map) {
                                Object orderIdObj = ((Map<?, ?>) o).get("orderId");
                                Object statusObj = ((Map<?, ?>) o).get("status");
                                if (orderIdObj != null && statusObj != null) {
                                    String orderId = orderIdObj.toString();
                                    String newStatus = statusObj.toString();
                                    updateOrderStatus(orderId, newStatus);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing orderUpdate", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in orderUpdate callback", e);
                }
            }
        });
        
        // Lắng nghe statusUpdate (tương thích)
        orderTracker.on("statusUpdate", args -> {
            if (args != null && args.length > 0) {
                Object o = args[0];
                try {
                    Log.i(TAG, "Received statusUpdate: " + o);
                    if (!isAdded())
                        return;
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null)
                            return;
                        try {
                            if (o instanceof Map) {
                                Object orderIdObj = ((Map<?, ?>) o).get("orderId");
                                Object statusObj = ((Map<?, ?>) o).get("status");
                                if (orderIdObj != null && statusObj != null) {
                                    String orderId = orderIdObj.toString();
                                    String newStatus = statusObj.toString();
                                    updateOrderStatus(orderId, newStatus);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing statusUpdate", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in statusUpdate callback", e);
                }
            }
        });

        // Load orders
        loadOrders();
    }
    
    // ✅ FIX: Cập nhật status của order trong danh sách khi nhận socket event
    private void updateOrderStatus(String orderId, String newStatus) {
        for (int i = 0; i < orders.size(); i++) {
            Map<String, Object> order = orders.get(i);
            Object oid = order.get("order_id");
            if (oid != null && oid.toString().equals(orderId)) {
                order.put("status", newStatus);
                adapter.notifyItemChanged(i);
                Log.i(TAG, "Updated order " + orderId + " status to " + newStatus);
                return;
            }
        }
        // Nếu không tìm thấy, có thể order mới được tạo, reload toàn bộ
        Log.i(TAG, "Order " + orderId + " not found in list, reloading...");
        loadOrders();
    }

    private void loadOrders() {
        binding.swipeRefresh.setRefreshing(true);
        binding.tvEmpty.setVisibility(View.GONE);

        OrdersApi ordersApi = BackendConfig.getRetrofit(requireContext()).create(OrdersApi.class);
        ordersApi.listOrders(null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call,
                                   @NonNull Response<List<Map<String, Object>>> response) {
                binding.swipeRefresh.setRefreshing(false);

                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    orders.clear();
                    orders.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (orders.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Failed to load orders: " + response.code());
                    Toast.makeText(requireContext(), "Không tải được danh sách đơn hàng", Toast.LENGTH_SHORT).show();
                    if (orders.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                if (!isAdded()) return;

                Log.e(TAG, "Error loading orders", t);
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                if (orders.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void cancelOrder(String orderId, @Nullable String reason, int position) {
        Log.d(TAG, "Attempting to cancel order ID: " + orderId);
        
        // Log order details for debugging
        if (position >= 0 && position < orders.size()) {
            Map<String, Object> order = orders.get(position);
            Log.d(TAG, "Order details: " + order.toString());
            Log.d(TAG, "Order status: " + order.get("status"));
        }
        
        OrdersApi ordersApi = BackendConfig.getRetrofit(requireContext()).create(OrdersApi.class);
        
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        if (reason != null && !reason.trim().isEmpty()) {
            body.put("reason", reason);
        }

        ordersApi.cancelOrder(orderId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                Log.d(TAG, "Cancel order response code: " + response.code());
                Log.d(TAG, "Cancel order response isSuccessful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    Boolean success = (Boolean) result.get("success");
                    
                    if (Boolean.TRUE.equals(success)) {
                        Toast.makeText(requireContext(), "Đơn hàng đã được hủy thành công", Toast.LENGTH_SHORT).show();
                        // Reload danh sách đơn hàng
                        loadOrders();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(requireContext(), 
                                message != null ? message : "Không thể hủy đơn hàng", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Parse error message
                    String errorMessage = "Không thể hủy đơn hàng";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Cancel order error: " + errorBody);
                            // Try to parse JSON error
                            if (errorBody.contains("cannot_cancel_order")) {
                                // Backend sẽ trả về message chi tiết
                                if (errorBody.contains("đã bắt đầu chuẩn bị") || errorBody.contains("đang vận chuyển")) {
                                    errorMessage = "Không thể hủy đơn hàng khi đã bắt đầu chuẩn bị hoặc đang vận chuyển";
                                } else {
                                    errorMessage = "Không thể hủy đơn hàng ở trạng thái này";
                                }
                            } else if (errorBody.contains("order_not_found")) {
                                errorMessage = "Không tìm thấy đơn hàng";
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Error canceling order", t);
                Toast.makeText(requireContext(), "Lỗi kết nối khi hủy đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ FIX: Extract userId từ JWT token
    private String extractUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            // JWT format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            // Decode payload (base64)
            String payload = parts[1];
            // Add padding if needed
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            byte[] decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
            String json = new String(decoded, "UTF-8");
            // Parse JSON
            org.json.JSONObject obj = new org.json.JSONObject(json);
            return obj.optString("id", obj.optString("user_id", null));
        } catch (Exception e) {
            Log.e(TAG, "Error extracting userId from token", e);
            return null;
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ✅ FIX: Disconnect socket khi fragment bị destroy
        if (orderTracker != null) {
            orderTracker.disconnect();
            orderTracker = null;
        }
        binding = null;
    }
}

