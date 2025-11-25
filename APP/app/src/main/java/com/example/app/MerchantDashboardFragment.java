package com.example.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app.adapters.MerchantOrdersAdapter;
import com.example.app.databinding.FragmentMerchantDashboardBinding;
import com.example.app.network.AuthClient;
import com.example.app.network.MerchantApi;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MerchantDashboardFragment
 *
 * - 3 tab: "Chờ xác nhận", "Đang chuẩn bị", "Sẵn sàng"
 * - Kéo để làm mới (SwipeRefreshLayout)
 * - Nút Reload
 * - Gọi API: /merchant/orders?status=pending|preparing|ready
 *
 * YÊU CẦU LAYOUT (fragment_merchant_dashboard.xml) đã có các id:
 * - tab_status (TabLayout)
 * - swipe_refresh (SwipeRefreshLayout)
 * - recycler_pending, recycler_preparing, recycler_ready (RecyclerView)
 * - layout_empty (View) (tuỳ chọn)
 * - btn_reload (View) (tuỳ chọn)
 */
public class MerchantDashboardFragment extends Fragment {

    private FragmentMerchantDashboardBinding binding;

    private MerchantApi merchantApi;

    private final List<Map<String, Object>> listPending = new ArrayList<>();
    private final List<Map<String, Object>> listPreparing = new ArrayList<>();
    private final List<Map<String, Object>> listReady = new ArrayList<>();

    private MerchantOrdersAdapter adapterPending;
    private MerchantOrdersAdapter adapterPreparing;
    private MerchantOrdersAdapter adapterReady;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMerchantDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrofit (dùng chung từ AuthClient để tự gắn Authorization header)
        merchantApi = new AuthClient(requireContext())
                .getRetrofit()
                .create(MerchantApi.class);

        // Setup Tabs
        setupTabs();

        // RecyclerViews
        binding.recyclerPending.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPreparing.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReady.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapterPending = new MerchantOrdersAdapter(requireContext(), listPending, callbacks("pending"));
        adapterPreparing = new MerchantOrdersAdapter(requireContext(), listPreparing, callbacks("preparing"));
        adapterReady = new MerchantOrdersAdapter(requireContext(), listReady, callbacks("ready"));

        binding.recyclerPending.setAdapter(adapterPending);
        binding.recyclerPreparing.setAdapter(adapterPreparing);
        binding.recyclerReady.setAdapter(adapterReady);

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener(this::refreshAll);

        // Nút reload (nếu có trong layout)
        if (binding.btnReload != null) {
            binding.btnReload.setOnClickListener(v -> refreshAll());
            ViewCompat.setTooltipText(binding.btnReload, "Tải lại");
        }

        // Lần đầu: load tất cả
        binding.swipeRefresh.setRefreshing(true);
        refreshAll();
    }

    private void setupTabs() {
        TabLayout tl = binding.tabStatus;
        if (tl.getTabCount() == 0) {
            tl.addTab(tl.newTab().setText("Chờ xác nhận").setTag("pending"));
            tl.addTab(tl.newTab().setText("Đang chuẩn bị").setTag("preparing"));
            tl.addTab(tl.newTab().setText("Sẵn sàng").setTag("ready"));
        }

        // Mặc định hiện tab 0
        showOnly("pending");

        tl.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tag = (tab.getTag() != null) ? tab.getTag().toString() : "";
                if (TextUtils.isEmpty(tag))
                    tag = "pending";
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
        int p = View.GONE, pr = View.GONE, r = View.GONE;
        switch (which) {
            case "preparing":
                pr = View.VISIBLE;
                break;
            case "ready":
                r = View.VISIBLE;
                break;
            case "pending":
            default:
                p = View.VISIBLE;
                break;
        }
        binding.recyclerPending.setVisibility(p);
        binding.recyclerPreparing.setVisibility(pr);
        binding.recyclerReady.setVisibility(r);

        updateEmptyState(which);
    }

    private void updateEmptyState(@NonNull String which) {
        if (binding.layoutEmpty == null)
            return;

        boolean empty = false;
        switch (which) {
            case "pending":
                empty = listPending.isEmpty();
                break;
            case "preparing":
                empty = listPreparing.isEmpty();
                break;
            case "ready":
                empty = listReady.isEmpty();
                break;
        }
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void refreshAll() {
        // gọi 3 request song song
        fetchOrders("pending", listPending, adapterPending);
        fetchOrders("preparing", listPreparing, adapterPreparing);
        fetchOrders("ready", listReady, adapterReady);
    }

    private void fetchOrders(
            @NonNull String status,
            @NonNull List<Map<String, Object>> target,
            @NonNull MerchantOrdersAdapter adapter) {
        merchantApi.getMerchantOrders(status)
                .enqueue(new Callback<List<Map<String, Object>>>() {
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

                        } else {
                            // Fallback: không clear để user vẫn xem data cũ
                        }

                        stopRefreshingIfDone();
                        updateEmptyState(status);
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<Map<String, Object>>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        // Fallback im lặng, giữ data cũ
                        stopRefreshingIfDone();
                        updateEmptyState(status);
                    }
                });
    }

    // Dừng icon loading khi 3 call đều trả về (đơn giản hoá: khi call nào trả về
    // cũng gọi, Safe)
    private void stopRefreshingIfDone() {
        if (binding != null && binding.swipeRefresh.isRefreshing()) {
            // nhỏ gọn: dừng ngay khi có 1 call trả về để UI mượt, hoặc
            // bạn có thể đếm 3 request xong hẳn rồi dừng.
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private MerchantOrdersAdapter.Listener callbacks(@NonNull String bucket) {
        return new MerchantOrdersAdapter.Listener() {
            @Override
            public void onAccept(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, "preparing", () -> {
                    // ✅ FIX: refreshAll() đã được gọi trong updateStatus, không cần di chuyển thủ công
                    // Chỉ cần chuyển tab để user thấy order mới
                    String actualStatus = order.get("status") != null ? order.get("status").toString() : "";
                    String targetBucket = mapBackendStatusToBucket(actualStatus);
                    showOnly(targetBucket);
                });
            }

            @Override
            public void onReject(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, "cancelled", () -> {
                    // ✅ FIX: refreshAll() đã được gọi trong updateStatus, order sẽ tự động bị xóa khỏi danh sách
                    // Chỉ cần cập nhật empty state
                    updateEmptyState(bucket);
                });
            }

            @Override
            public void onReady(@NonNull Map<String, Object> order, int position) {
                updateStatus(order, "ready", () -> {
                    // ✅ FIX: refreshAll() đã được gọi trong updateStatus, không cần di chuyển thủ công
                    // Chỉ cần chuyển tab để user thấy order mới
                    String actualStatus = order.get("status") != null ? order.get("status").toString() : "";
                    String targetBucket = mapBackendStatusToBucket(actualStatus);
                    showOnly(targetBucket);
                });
            }

            @Override
            public void onItemClick(@NonNull Map<String, Object> order, int position) {
                // ✅ FIX: Navigate đến OrderDetailFragment để xem chi tiết đơn hàng
                Object orderIdObj = order.get("order_id");
                String orderId = orderIdObj != null ? orderIdObj.toString() : null;
                if (orderId != null) {
                    Bundle args = new Bundle();
                    args.putString("orderId", orderId);
                    androidx.navigation.fragment.NavHostFragment.findNavController(MerchantDashboardFragment.this)
                            .navigate(R.id.action_merchantHomeFragment_to_orderDetailFragment, args);
                }
            }
        };
    }

    private void updateStatus(@NonNull Map<String, Object> order, @NonNull String nextStatus, @NonNull Runnable onOk) {
        Object orderId = order.get("order_id");
        if (orderId == null)
            orderId = order.get("code");
        String id = orderId != null ? orderId.toString() : null;
        if (TextUtils.isEmpty(id))
            return;

        Map<String, Object> body = new HashMap<>();
        body.put("status", nextStatus);

        merchantApi.updateOrderStatus(id, body)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;

                        if (response.isSuccessful() && response.body() != null) {
                            // ✅ FIX: Đọc status thực tế từ response (backend trả về CONFIRMED, COOKING, READY, etc.)
                            Map<String, Object> responseBody = response.body();
                            Object actualStatus = responseBody.get("status");
                            String statusStr = actualStatus != null ? actualStatus.toString().toUpperCase() : "";
                            
                            // ✅ FIX: Map status từ backend sang bucket frontend
                            String targetBucket = mapBackendStatusToBucket(statusStr);
                            
                            // ✅ FIX: Cập nhật status trong order object với status thực tế từ backend
                            order.put("status", statusStr);
                            
                            // ✅ FIX: Di chuyển order sang bucket mới ngay lập tức để UI cập nhật
                            moveOrderToBucket(order, targetBucket);
                            
                            // ✅ FIX: Không refresh toàn bộ để tránh load lại đơn hàng cũ
                            // moveOrderToBucket() đã cập nhật UI rồi, không cần refresh
                            
                            // Chạy callback để cập nhật UI (nếu cần)
                            onOk.run();
                        } else {
                            // TODO: hiện Toast báo lỗi nếu cần
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        // TODO: hiện Toast báo lỗi nếu cần
                    }
                });
    }
    
    /**
     * Di chuyển order sang bucket mới dựa trên status và cập nhật UI ngay lập tức
     */
    private void moveOrderToBucket(@NonNull Map<String, Object> order, @NonNull String targetBucket) {
        Object orderId = order.get("order_id");
        if (orderId == null) orderId = order.get("code");
        if (orderId == null) return;
        
        String idStr = orderId.toString();
        
        // Tìm và xóa order khỏi bucket cũ
        int oldPosition = -1;
        String oldBucket = null;
        
        // Tìm trong listPending
        for (int i = 0; i < listPending.size(); i++) {
            Map<String, Object> o = listPending.get(i);
            Object oid = o.get("order_id");
            if (oid == null) oid = o.get("code");
            if (oid != null && oid.toString().equals(idStr)) {
                oldPosition = i;
                oldBucket = "pending";
                break;
            }
        }
        
        // Nếu không tìm thấy trong pending, tìm trong preparing
        if (oldPosition == -1) {
            for (int i = 0; i < listPreparing.size(); i++) {
                Map<String, Object> o = listPreparing.get(i);
                Object oid = o.get("order_id");
                if (oid == null) oid = o.get("code");
                if (oid != null && oid.toString().equals(idStr)) {
                    oldPosition = i;
                    oldBucket = "preparing";
                    break;
                }
            }
        }
        
        // Nếu không tìm thấy trong preparing, tìm trong ready
        if (oldPosition == -1) {
            for (int i = 0; i < listReady.size(); i++) {
                Map<String, Object> o = listReady.get(i);
                Object oid = o.get("order_id");
                if (oid == null) oid = o.get("code");
                if (oid != null && oid.toString().equals(idStr)) {
                    oldPosition = i;
                    oldBucket = "ready";
                    break;
                }
            }
        }
        
        // Nếu tìm thấy order trong bucket cũ và bucket mới khác bucket cũ
        if (oldPosition >= 0 && oldBucket != null && !oldBucket.equals(targetBucket)) {
            // Xóa khỏi bucket cũ
            switch (oldBucket) {
                case "pending":
                    listPending.remove(oldPosition);
                    adapterPending.notifyItemRemoved(oldPosition);
                    adapterPending.notifyItemRangeChanged(oldPosition, listPending.size() - oldPosition);
                    break;
                case "preparing":
                    listPreparing.remove(oldPosition);
                    adapterPreparing.notifyItemRemoved(oldPosition);
                    adapterPreparing.notifyItemRangeChanged(oldPosition, listPreparing.size() - oldPosition);
                    break;
                case "ready":
                    listReady.remove(oldPosition);
                    adapterReady.notifyItemRemoved(oldPosition);
                    adapterReady.notifyItemRangeChanged(oldPosition, listReady.size() - oldPosition);
                    break;
            }
            
            // Thêm vào bucket mới
            int newPosition = -1;
            switch (targetBucket) {
                case "pending":
                    listPending.add(order);
                    newPosition = listPending.size() - 1;
                    adapterPending.notifyItemInserted(newPosition);
                    break;
                case "preparing":
                    listPreparing.add(order);
                    newPosition = listPreparing.size() - 1;
                    adapterPreparing.notifyItemInserted(newPosition);
                    break;
                case "ready":
                    listReady.add(order);
                    newPosition = listReady.size() - 1;
                    adapterReady.notifyItemInserted(newPosition);
                    break;
            }
            
            // Cập nhật empty state
            updateEmptyState(oldBucket);
            updateEmptyState(targetBucket);
        } else if (oldPosition >= 0 && oldBucket != null && oldBucket.equals(targetBucket)) {
            // Nếu order vẫn ở cùng bucket, chỉ cần notify item changed
            switch (targetBucket) {
                case "pending":
                    adapterPending.notifyItemChanged(oldPosition);
                    break;
                case "preparing":
                    adapterPreparing.notifyItemChanged(oldPosition);
                    break;
                case "ready":
                    adapterReady.notifyItemChanged(oldPosition);
                    break;
            }
        }
    }
    
    /**
     * Map status từ backend (CONFIRMED, COOKING, READY, etc.) sang bucket frontend (pending, preparing, ready)
     */
    private String mapBackendStatusToBucket(@NonNull String backendStatus) {
        String statusUpper = backendStatus.toUpperCase();
        if (statusUpper.equals("PENDING") || statusUpper.equals("CONFIRMED")) {
            return "pending"; // Chờ xác nhận
        } else if (statusUpper.equals("COOKING") || statusUpper.equals("PREPARING")) {
            return "preparing"; // Đang chuẩn bị
        } else if (statusUpper.equals("READY") || statusUpper.equals("HANDOVER")) {
            return "ready"; // Sẵn sàng
        }
        return "pending"; // Default
    }

    private static void removeAt(List<Map<String, Object>> list, int pos, MerchantOrdersAdapter adp) {
        if (pos < 0 || pos >= list.size())
            return;
        list.remove(pos);
        adp.notifyItemRemoved(pos);
        adp.notifyItemRangeChanged(pos, Math.max(0, list.size() - pos));
    }

    private static void safeMoveItem(List<Map<String, Object>> from, int pos, List<Map<String, Object>> to) {
        if (pos < 0 || pos >= from.size())
            return;
        Map<String, Object> item = from.remove(pos);
        to.add(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
