package com.example.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;

import com.example.app.adapters.OrderHistoryAdapter;
import com.example.app.adapters.OrderItemAdapter;
import com.example.app.databinding.FragmentOrderDetailBinding;
import com.example.app.network.BackendConfig;
import com.example.app.network.OrderTracker;
import com.example.app.network.OrdersClient;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailFragment extends Fragment {
    private FragmentOrderDetailBinding binding;
    private OrderTracker tracker;
    private String backendBase;
    private Double lastShipperLat = null;
    private Double lastShipperLng = null;
    private String deliveryAddress = null; // Địa chỉ giao hàng cho shipper

    // Throttle UI update cho vị trí shipper
    private static final long MIN_LOC_UPDATE_INTERVAL_MS = 1000L; // 1s/lần
    private long lastLocUiUpdateMs = 0L;
    private String lastShipperUiText = null;
    
    // ✅ FIX: Chuyển thành instance variables để có thể truy cập từ updateOrderInfo
    private List<Map<String, Object>> itemsList;
    private List<Map<String, Object>> historyList;
    private OrderItemAdapter itemAdapter;
    
    // Camera/Photo
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri photoUri;
    private OrderHistoryAdapter historyAdapter;
    private OrdersClient ordersClient;
    private String currentOrderId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ordersClient = new OrdersClient(requireContext());

        // Lấy base ROOT (không có /api) cho OrderTracker (socket)
        backendBase = BackendConfig.getRetrofitRoot(requireContext()).baseUrl().toString();

        currentOrderId = null;
        if (getArguments() != null) {
            currentOrderId = getArguments().getString("orderId");
        }
        if (currentOrderId == null) {
            if (binding != null) {
                binding.tvStatus.setText(getString(R.string.no_order));
            }
            return;
        }
        
        final String orderId = currentOrderId; // Final reference for lambda

        // ===== Kết nối realtime =====
        tracker = new OrderTracker(requireContext(), backendBase);
        tracker.setAuthToken(BackendConfig.getAccessToken(requireContext()));
        tracker.connect();
        tracker.joinOrder(orderId);

        // ✅ FIX: Nhận cập nhật trạng thái đơn từ merchant (statusUpdate event)
        tracker.on("statusUpdate", args -> {
            if (args != null && args.length > 0) {
                Object o = args[0];
                try {
                    Log.i("OrderDetail", "statusUpdate: " + o);
                    if (!isAdded())
                        return;
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null)
                            return;
                        try {
                            if (o instanceof Map) {
                                Object status = ((Map<?, ?>) o).get("status");
                                if (status != null) {
                                    applyStatus(requireContext(), status.toString());
                                    // Refresh lại order để lấy thông tin mới nhất (bao gồm shipper)
                                    final String currentUserRole = new com.example.app.network.AuthClient(requireContext()).getRole();
                                    ordersClient.getOrder(orderId, new Callback<Map<String, Object>>() {
                                        @Override
                                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                            if (response.isSuccessful() && response.body() != null && isAdded()) {
                                                updateOrderInfo(response.body(), currentUserRole);
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                            Log.e("OrderDetail", "Failed to refresh order", t);
                                        }
                                    });
                                    return;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    });
                } catch (Exception e) {
                    Log.e("OrderDetail", "parse statusUpdate", e);
                }
            }
        });
        
        // Nhận cập nhật trạng thái đơn (orderUpdate event - tương thích)
        tracker.onOrderUpdate(args -> {
            if (args != null && args.length > 0) {
                Object o = args[0];
                try {
                    Log.i("OrderDetail", "order:update: " + o);
                    if (!isAdded())
                        return;
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null)
                            return;
                        try {
                            if (o instanceof Map) {
                                Object status = ((Map<?, ?>) o).get("status");
                                if (status != null) {
                                    applyStatus(requireContext(), status.toString());
                                    return;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        // Fallback: hiển thị thô nếu không parse được
                        binding.tvStatus.setText(o.toString());
                    });
                } catch (Exception e) {
                    Log.e("OrderDetail", "parse order:update", e);
                }
            }
        });

        // Nhận vị trí shipper (có throttle UI)
        tracker.onShipperLocation(args -> {
            if (args != null && args.length > 0) {
                Object payload = args[0];
                try {
                    String txt = payload.toString();
                    try {
                        if (payload instanceof Map) {
                            Object lat = ((Map<?, ?>) payload).get("lat");
                            Object lng = ((Map<?, ?>) payload).get("lng");
                            if (lat != null && lng != null) {
                                lastShipperLat = Double.parseDouble(lat.toString());
                                lastShipperLng = Double.parseDouble(lng.toString());
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    long now = System.currentTimeMillis();
                    boolean contentChanged = (lastShipperUiText == null) || !txt.equals(lastShipperUiText);
                    if (contentChanged || (now - lastLocUiUpdateMs) >= MIN_LOC_UPDATE_INTERVAL_MS) {
                        lastShipperUiText = txt;
                        lastLocUiUpdateMs = now;

                        if (!isAdded())
                            return;
                        final String finalTxt = txt;
                        requireActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.tvShipper.setText(getString(R.string.shipper_label) + " " + finalTxt);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("OrderDetail", "shipper loc parse", e);
                }
            }
        });

        // Chuẩn bị adapters
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        itemsList = new ArrayList<>();
        historyList = new ArrayList<>();
        itemAdapter = new OrderItemAdapter(itemsList);
        historyAdapter = new OrderHistoryAdapter(historyList);
        binding.rvItems.setAdapter(itemAdapter);
        binding.rvHistory.setAdapter(historyAdapter);

        // ✅ FIX: Set text button theo role (sẽ cập nhật lại sau khi load order)
        binding.btnOpenMaps.setText(getString(R.string.open_in_maps));
        binding.btnAcceptOrder.setVisibility(View.GONE);
        
        // ✅ FIX: Set click listener cho nút "Nhận đơn"
        binding.btnAcceptOrder.setOnClickListener(v -> {
            final String userRole = new com.example.app.network.AuthClient(requireContext()).getRole();
            if ("SHIPPER".equalsIgnoreCase(userRole)) {
                acceptOrder();
            }
        });
        
        // ✅ FIX: Set click listener cho nút "Xác nhận đã giao" (chụp ảnh)
        binding.btnConfirmDelivered.setOnClickListener(v -> {
            final String userRole = new com.example.app.network.AuthClient(requireContext()).getRole();
            if ("SHIPPER".equalsIgnoreCase(userRole)) {
                takePhotoForDelivery();
            }
        });
        
        // ✅ FIX: Setup camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && photoUri != null) {
                    confirmDeliveredWithPhoto(photoUri);
                } else {
                    Toast.makeText(requireContext(), "Chụp ảnh thất bại", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // ✅ FIX: Gọi API lấy chi tiết đơn - phân biệt role
        final String userRole = new com.example.app.network.AuthClient(requireContext()).getRole();
        if ("SHIPPER".equalsIgnoreCase(userRole)) {
            // Shipper: dùng ShipperApi
            com.example.app.network.ShipperApi shipperApi = new com.example.app.network.AuthClient(requireContext())
                    .getRetrofit()
                    .create(com.example.app.network.ShipperApi.class);
            shipperApi.getShipperOrder(orderId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (!isAdded())
                        return;

                    Log.d("OrderDetail", "Shipper API response code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("OrderDetail", "Shipper API success, body: " + response.body());
                        updateOrderInfo(response.body(), userRole);
                    } else {
                        String errorMsg = "Không tải được đơn hàng";
                        if (response.errorBody() != null) {
                            try {
                                String errorStr = response.errorBody().string();
                                Log.e("OrderDetail", "Shipper API error: " + errorStr);
                                errorMsg += " (HTTP " + response.code() + ")";
                            } catch (Exception e) {
                                Log.e("OrderDetail", "Error reading error body", e);
                            }
                        }
                        binding.tvStatus.setText(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    if (!isAdded())
                        return;
                    Log.e("OrderDetail", "Shipper API failure", t);
                    binding.tvStatus.setText("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "Unknown"));
                }
            });
        } else {
            // Customer/Merchant: dùng OrdersClient
            ordersClient.getOrder(orderId, new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded())
                    return;

                if (response.isSuccessful() && response.body() != null) {
                    updateOrderInfo(response.body(), userRole);
                } else {
                    binding.tvStatus.setText(getString(R.string.err_cannot_fetch_order));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (!isAdded())
                    return;
                binding.tvStatus.setText(getString(R.string.network_error));
            }
            });
        }

        // ✅ FIX: Mở Maps - phân biệt role
        binding.btnOpenMaps.setOnClickListener(v -> {
            
            if ("SHIPPER".equalsIgnoreCase(userRole)) {
                // Shipper: mở navigation đến địa chỉ giao hàng
                if (deliveryAddress == null || deliveryAddress.isEmpty()) {
                    Toast.makeText(getContext(), "Không có địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    // Mở Google Maps với navigation đến địa chỉ
                    String uri = "google.navigation:q=" + android.net.Uri.encode(deliveryAddress, "UTF-8");
                    android.content.Intent intent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        // Fallback: mở map với địa chỉ
                        uri = "geo:0,0?q=" + android.net.Uri.encode(deliveryAddress, "UTF-8");
                        intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không thể mở bản đồ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                // Customer: mở map để xem shipper location
                if (lastShipperLat == null || lastShipperLng == null) {
                    Toast.makeText(getContext(), "Chưa có vị trí shipper", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String uri = String.format(
                            Locale.US,
                            "geo:%f,%f?q=%f,%f(Shipper)",
                            lastShipperLat, lastShipperLng, lastShipperLat, lastShipperLng);
                    android.content.Intent intent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(uri));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(
                            getContext(),
                            getString(R.string.cannot_open_maps) + " " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tracker != null)
            tracker.disconnect();
        binding = null;
    }

    // ================= Helpers =================
    
    private void updateOrderInfo(Map<?, ?> body, String userRole) {
        Object ord = body.get("order");
        Object items = body.get("items");
        Object history = body.get("history");
        Object shipperObj = body.get("shipper"); // ✅ FIX: Đọc shipper từ body, không phải orderMap

        try {
            if (ord instanceof Map) {
                Map<?, ?> orderMap = (Map<?, ?>) ord;
                Object status = orderMap.get("status");
                Object total = orderMap.get("total");
                Object totalAmount = orderMap.get("total_amount"); // ✅ FIX: Thử lấy total_amount nếu total = 0
                if (total == null || (total instanceof Number && ((Number) total).doubleValue() == 0)) {
                    total = totalAmount; // Dùng total_amount nếu total = 0 hoặc null
                }
                Log.d("OrderDetail", "Total from API: total=" + total + ", total_amount=" + totalAmount);
                Object addr = orderMap.get("address");

                if (status != null) {
                    applyStatus(requireContext(), status.toString());
                }

                // ✅ FIX: Hiển thị mã đơn hàng
                Object orderIdObj = orderMap.get("order_id");
                if (orderIdObj != null) {
                    binding.tvOrderId.setText("Mã đơn: #" + orderIdObj.toString());
                } else {
                    binding.tvOrderId.setText("Mã đơn: -");
                }
                
                String totalStr = (total != null) ? safeFormatCurrency(total) : "-";
                String addrStr = (addr != null && !addr.toString().trim().isEmpty() && !addr.toString().equals("-")) 
                        ? addr.toString() : "Chưa có địa chỉ";
                // ✅ FIX: Lưu địa chỉ để shipper navigate (chỉ lưu nếu có địa chỉ thực sự)
                if (addr != null && !addr.toString().trim().isEmpty() && !addr.toString().equals("-")) {
                    deliveryAddress = addr.toString();
                } else {
                    deliveryAddress = null;
                }
                binding.tvOrder.setText(
                        "Tổng: " + totalStr + "\n" +
                                "Địa chỉ: " + addrStr);
                
                // ✅ FIX: Cập nhật button theo role và trạng thái đơn
                Object shipperId = orderMap.get("shipper_id");
                Object orderStatus = orderMap.get("status");
                boolean isAssigned = shipperId != null;
                String statusStr = orderStatus != null ? orderStatus.toString().toUpperCase() : "";
                
                if ("SHIPPER".equalsIgnoreCase(userRole)) {
                    // Shipper: hiển thị nút "Nhận đơn" nếu đơn chưa được nhận
                    if (!isAssigned && (statusStr.equals("READY") || statusStr.equals("CONFIRMED") || statusStr.equals("COOKING"))) {
                        binding.btnAcceptOrder.setVisibility(View.VISIBLE);
                        binding.btnConfirmDelivered.setVisibility(View.GONE);
                        binding.btnOpenMaps.setVisibility(View.GONE);
                    } else if (isAssigned && statusStr.equals("SHIPPING")) {
                        // ✅ FIX: Hiển thị nút "Xác nhận đã giao" khi đang giao
                        binding.btnAcceptOrder.setVisibility(View.GONE);
                        binding.btnConfirmDelivered.setVisibility(View.VISIBLE);
                        binding.btnOpenMaps.setText("Dò đường đi giao");
                        binding.btnOpenMaps.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnAcceptOrder.setVisibility(View.GONE);
                        binding.btnConfirmDelivered.setVisibility(View.GONE);
                        binding.btnOpenMaps.setText("Dò đường đi giao");
                        binding.btnOpenMaps.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Customer: chỉ hiển thị button nếu có shipper
                    binding.btnAcceptOrder.setVisibility(View.GONE);
                    binding.btnConfirmDelivered.setVisibility(View.GONE);
                    binding.btnOpenMaps.setText(getString(R.string.open_in_maps));
                    if (shipperObj instanceof Map) {
                        binding.btnOpenMaps.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnOpenMaps.setVisibility(View.GONE);
                    }
                }
                
                // ✅ FIX: Hiển thị thông tin shipper
                if (shipperObj instanceof Map) {
                    Map<?, ?> shipper = (Map<?, ?>) shipperObj;
                    Object shipperName = shipper.get("shipper_name");
                    Object shipperPhone = shipper.get("shipper_phone");
                    Object vehiclePlate = shipper.get("vehicle_plate");
                    Object shipperLat = shipper.get("shipper_lat");
                    Object shipperLng = shipper.get("shipper_lng");
                    
                    if (shipperName != null || shipperPhone != null || vehiclePlate != null) {
                        binding.layoutShipperInfo.setVisibility(View.VISIBLE);
                        binding.tvShipper.setVisibility(View.GONE);
                        
                        if (shipperName != null) {
                            binding.tvShipperName.setVisibility(View.VISIBLE);
                            binding.tvShipperName.setText("Tên shipper: " + shipperName.toString());
                        } else {
                            binding.tvShipperName.setVisibility(View.GONE);
                        }
                        
                        if (shipperPhone != null) {
                            binding.tvShipperPhone.setVisibility(View.VISIBLE);
                            binding.tvShipperPhone.setText("SĐT: " + shipperPhone.toString());
                        } else {
                            binding.tvShipperPhone.setVisibility(View.GONE);
                        }
                        
                        if (vehiclePlate != null) {
                            binding.tvShipperVehicle.setVisibility(View.VISIBLE);
                            binding.tvShipperVehicle.setText("Biển số xe: " + vehiclePlate.toString());
                        } else {
                            binding.tvShipperVehicle.setVisibility(View.GONE);
                        }
                        
                        // Lưu vị trí shipper để mở map
                        if (shipperLat != null && shipperLng != null) {
                            try {
                                lastShipperLat = Double.parseDouble(shipperLat.toString());
                                lastShipperLng = Double.parseDouble(shipperLng.toString());
                            } catch (Exception e) {
                                Log.e("OrderDetail", "Parse shipper location", e);
                            }
                        }
                    } else {
                        binding.layoutShipperInfo.setVisibility(View.GONE);
                        binding.tvShipper.setVisibility(View.VISIBLE);
                        binding.tvShipper.setText(getString(R.string.shipper_label) + " Chưa có");
                    }
                } else {
                    binding.layoutShipperInfo.setVisibility(View.GONE);
                    binding.tvShipper.setVisibility(View.VISIBLE);
                    binding.tvShipper.setText(getString(R.string.shipper_label) + " Chưa có");
                }
            } else if (ord != null) {
                binding.tvOrder.setText(ord.toString());
            }

            // Items
            itemsList.clear();
            if (items instanceof List) {
                for (Object it : (List<?>) items) {
                    if (it instanceof Map)
                        itemsList.add((Map<String, Object>) it);
                }
            }
            itemAdapter.notifyDataSetChanged();

            // History
            historyList.clear();
            if (history instanceof List) {
                for (Object h : (List<?>) history) {
                    if (h instanceof Map)
                        historyList.add((Map<String, Object>) h);
                }
            }
            historyAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            Log.e("OrderDetail", "parsing order body", e);
            binding.tvStatus.setText(getString(R.string.err_cannot_show_order_detail));
        }
    }

    private void applyStatus(Context ctx, String raw) {
        if (binding == null)
            return;

        String status = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        int color;
        int iconRes;
        String display;

        switch (status) {
            case "delivered":
            case "completed":
            case "done":
            case "success":
                iconRes = R.drawable.ic_status_delivered;
                color = ContextCompat.getColor(ctx, R.color.ff_success);
                display = "Đã giao";
                break;

            case "ready":
            case "handover":
                // ✅ FIX: READY status hiển thị màu xanh (success)
                iconRes = R.drawable.ic_status_delivered;
                color = ContextCompat.getColor(ctx, R.color.ff_success);
                display = "Sẵn sàng";
                break;

            case "shipping":
            case "in_progress":
            case "preparing":
            case "on_the_way":
            case "processing":
            case "cooking":
            case "confirmed":
            case "delivering":
                iconRes = R.drawable.ic_status_inprogress;
                color = ContextCompat.getColor(ctx, R.color.ff_warning);
                display = "Đang xử lý";
                break;

            case "cancelled":
            case "canceled":
            case "rejected":
            case "failed":
                iconRes = R.drawable.ic_status_cancelled;
                color = ContextCompat.getColor(ctx, R.color.ff_error);
                display = "Đã hủy";
                break;

            default:
                iconRes = R.drawable.ic_status_inprogress;
                color = ContextCompat.getColor(ctx, R.color.ff_text_secondary);
                display = raw;
                break;
        }

        binding.ivStatus.setImageResource(iconRes);
        binding.ivStatus.setColorFilter(color);
        binding.tvStatus.setTextColor(color);
        binding.tvStatus.setText(display);
    }

    private String safeFormatCurrency(Object v) {
        try {
            String s = v.toString();
            if (s.endsWith("đ") || s.endsWith("₫") || s.toLowerCase(Locale.ROOT).contains("vnd")) {
                return s;
            }
            double value = Double.parseDouble(s.replaceAll("[^0-9.]", ""));
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            return nf.format(value) + " đ";
        } catch (Exception ignored) {
            return String.valueOf(v);
        }
    }
    
    // ✅ FIX: Nhận đơn hàng (shipper accept order)
    private void acceptOrder() {
        Bundle args = getArguments();
        if (args == null) {
            Toast.makeText(requireContext(), "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        String orderId = args.getString("orderId");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.example.app.network.ShipperApi shipperApi = new com.example.app.network.AuthClient(requireContext())
                .getRetrofit()
                .create(com.example.app.network.ShipperApi.class);
        
        binding.btnAcceptOrder.setEnabled(false);
        shipperApi.acceptOrder(orderId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                binding.btnAcceptOrder.setEnabled(true);
                if (!isAdded()) return;
                
                if (response.isSuccessful()) {
                    android.util.Log.d("OrderDetail", "Order accepted successfully, going back to dashboard");
                    Toast.makeText(requireContext(), "Đã nhận đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    // ✅ FIX: Quay lại dashboard - ShipperDashboardFragment sẽ tự refresh trong onResume
                    requireActivity().onBackPressed();
                } else {
                    String errorMsg = "Nhận đơn thất bại";
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            Log.e("OrderDetail", "Accept order error: " + errorStr);
                            if (errorStr.contains("order_already_assigned")) {
                                errorMsg = "Đơn hàng đã được nhận bởi shipper khác";
                            } else if (errorStr.contains("order_not_ready")) {
                                errorMsg = "Đơn hàng chưa sẵn sàng để nhận";
                            } else if (errorStr.contains("shipper_not_available")) {
                                errorMsg = "Shipper không available";
                            } else if (errorStr.contains("not_a_shipper")) {
                                errorMsg = "Bạn không phải shipper";
                            } else {
                                errorMsg = "Nhận đơn thất bại: " + errorStr;
                            }
                        } catch (Exception e) {
                            Log.e("OrderDetail", "Error reading error body", e);
                        }
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                binding.btnAcceptOrder.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void takePhotoForDelivery() {
        try {
            // Tạo file để lưu ảnh
            File photoFile = new File(requireContext().getCacheDir(), "delivery_photo_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                photoFile
            );
            
            // Mở camera
            cameraLauncher.launch(photoUri);
        } catch (Exception e) {
            Log.e("OrderDetail", "Error taking photo", e);
            Toast.makeText(requireContext(), "Không thể mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void confirmDeliveredWithPhoto(Uri photoUri) {
        Bundle args = getArguments();
        if (args == null) {
            Toast.makeText(requireContext(), "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        String orderId = args.getString("orderId");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ FIX: Update status DELIVERED
        com.example.app.network.ShipperApi shipperApi = new com.example.app.network.AuthClient(requireContext())
                .getRetrofit()
                .create(com.example.app.network.ShipperApi.class);
        
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("status", "DELIVERED");
        // TODO: Upload ảnh lên server nếu cần
        
        binding.btnConfirmDelivered.setEnabled(false);
        shipperApi.updateOrderStatus(orderId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                binding.btnConfirmDelivered.setEnabled(true);
                if (!isAdded()) return;
                
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Đã xác nhận giao hàng thành công", Toast.LENGTH_SHORT).show();
                    // Quay lại dashboard để refresh
                    requireActivity().onBackPressed();
                } else {
                    String errorMsg = "Xác nhận giao hàng thất bại";
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            if (errorStr.contains("order_not_assigned")) {
                                errorMsg = "Đơn hàng không thuộc về bạn";
                            }
                        } catch (Exception e) {
                            Log.e("OrderDetail", "Error reading error body", e);
                        }
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                binding.btnConfirmDelivered.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"), Toast.LENGTH_LONG).show();
            }
        });
    }
}
