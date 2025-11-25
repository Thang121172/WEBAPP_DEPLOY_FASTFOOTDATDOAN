package com.example.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app.adapters.MerchantOrdersAdapter;
import com.example.app.databinding.FragmentMerchantHomeBinding;
import com.example.app.network.AuthClient;
import com.example.app.network.MerchantApi;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MerchantHomeFragment
 *
 * Tabs:
 * - new : đơn mới (chưa xử lý)
 * - in_progress : đang chuẩn bị
 * - completed : đã sẵn sàng/đã giao cho shipper
 *
 * Hành động:
 * - Accept (-> preparing)
 * - Ready (-> ready -> sang completed)
 * - Reject/Cancel (-> cancelled)
 */
public class MerchantHomeFragment extends Fragment {

    private FragmentMerchantHomeBinding binding;

    private MerchantApi merchantApi;

    // Buckets
    private final List<Map<String, Object>> listNew = new ArrayList<>();
    private final List<Map<String, Object>> listInProgress = new ArrayList<>();
    private final List<Map<String, Object>> listReady = new ArrayList<>();
    private final List<Map<String, Object>> listCompleted = new ArrayList<>();

    // Adapters
    private MerchantOrdersAdapter adpNew;
    private MerchantOrdersAdapter adpInProgress;
    private MerchantOrdersAdapter adpReady;
    private MerchantOrdersAdapter adpCompleted;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMerchantHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        merchantApi = new AuthClient(requireContext())
                .getRetrofit()
                .create(MerchantApi.class);

        setupTabs();

        // RecyclerViews
        binding.recyclerNew.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerInProgress.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReady.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));

        adpNew = new MerchantOrdersAdapter(requireContext(), listNew, callbacks("new"));
        adpInProgress = new MerchantOrdersAdapter(requireContext(), listInProgress, callbacks("in_progress"));
        adpReady = new MerchantOrdersAdapter(requireContext(), listReady, callbacks("ready"));
        adpCompleted = new MerchantOrdersAdapter(requireContext(), listCompleted, callbacks("completed"));

        binding.recyclerNew.setAdapter(adpNew);
        binding.recyclerInProgress.setAdapter(adpInProgress);
        binding.recyclerReady.setAdapter(adpReady);
        binding.recyclerCompleted.setAdapter(adpCompleted);

        // Refresh
        binding.swipeRefresh.setOnRefreshListener(this::refreshAll);

        // Optional reload button
        if (binding.btnReload != null) {
            binding.btnReload.setOnClickListener(v -> refreshAll());
            ViewCompat.setTooltipText(binding.btnReload, "Tải lại");
        }

        // ✅ FIX: Thêm nút "Thêm món ăn"
        if (binding.btnAddMenu != null) {
            binding.btnAddMenu.setOnClickListener(v -> {
                androidx.navigation.NavController navController = 
                    androidx.navigation.fragment.NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_merchantHomeFragment_to_merchantMenuFragment);
            });
        }

        // ✅ FIX: Thêm nút "Doanh thu"
        if (binding.btnRevenue != null) {
            binding.btnRevenue.setOnClickListener(v -> {
                androidx.navigation.NavController navController = 
                    androidx.navigation.fragment.NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_merchantHomeFragment_to_merchantRevenueFragment);
            });
        }

        // First load
        binding.swipeRefresh.setRefreshing(true);
        refreshAll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ================= Tabs =================

    private void setupTabs() {
        TabLayout tl = binding.tabStatus;
        if (tl.getTabCount() == 0) {
            tl.addTab(tl.newTab().setText("Mới").setTag("new"));
            tl.addTab(tl.newTab().setText("Đang làm").setTag("in_progress"));
            tl.addTab(tl.newTab().setText("Sẵn sàng").setTag("ready"));
            tl.addTab(tl.newTab().setText("Hoàn tất").setTag("completed"));
        }

        showOnly("new");

        tl.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tag = tab.getTag() != null ? tab.getTag().toString() : "new";
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
        int n = View.GONE, p = View.GONE, r = View.GONE, c = View.GONE;
        switch (which) {
            case "in_progress":
                p = View.VISIBLE;
                break;
            case "ready":
                r = View.VISIBLE;
                break;
            case "completed":
                c = View.VISIBLE;
                break;
            case "new":
            default:
                n = View.VISIBLE;
                break;
        }
        binding.recyclerNew.setVisibility(n);
        binding.recyclerInProgress.setVisibility(p);
        binding.recyclerReady.setVisibility(r);
        binding.recyclerCompleted.setVisibility(c);

        updateEmpty(which);
    }

    private void updateEmpty(@NonNull String which) {
        if (binding.layoutEmpty == null)
            return;
        boolean empty;
        switch (which) {
            case "new":
                empty = listNew.isEmpty();
                break;
            case "in_progress":
                empty = listInProgress.isEmpty();
                break;
            case "ready":
                empty = listReady.isEmpty();
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
        fetchBucket("new", listNew, adpNew);
        fetchBucket("in_progress", listInProgress, adpInProgress);
        fetchBucket("ready", listReady, adpReady);
        fetchBucket("completed", listCompleted, adpCompleted);
    }

    private void fetchBucket(
            @NonNull String status,
            @NonNull List<Map<String, Object>> target,
            @NonNull MerchantOrdersAdapter adapter) {

        merchantApi.getMerchantOrders(status).enqueue(new Callback<List<Map<String, Object>>>() {
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

    private MerchantOrdersAdapter.Listener callbacks(@NonNull String bucket) {
        return new MerchantOrdersAdapter.Listener() {
            @Override
            public void onAccept(@NonNull Map<String, Object> order, int position) {
                // Accept = chuyển sang preparing
                String id = extractId(order);
                if (TextUtils.isEmpty(id))
                    return;

                Map<String, Object> body = new HashMap<>();
                body.put("status", "preparing");

                merchantApi.updateOrderStatus(id, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;
                        if (response.isSuccessful()) {
                            if ("new".equals(bucket)) {
                                moveItem(listNew, position, listInProgress);
                                adpNew.notifyItemRemoved(position);
                                adpInProgress.notifyItemInserted(listInProgress.size() - 1);
                                showOnly("in_progress");
                            }
                        } else {
                            showErrorSnack("Nhận đơn thất bại.", () -> onAccept(order, position));
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        showErrorSnack("Lỗi mạng khi nhận đơn: " + (t.getMessage() != null ? t.getMessage() : "?"),
                                () -> onAccept(order, position));
                    }
                });
            }

            @Override
            public void onReady(@NonNull Map<String, Object> order, int position) {
                // Mark ready = chuyển sang tab "Sẵn sàng" (READY)
                String id = extractId(order);
                if (TextUtils.isEmpty(id))
                    return;

                Map<String, Object> body = new HashMap<>();
                body.put("status", "ready");

                merchantApi.updateOrderStatus(id, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;
                        if (response.isSuccessful()) {
                            if ("in_progress".equals(bucket)) {
                                // Chuyển đơn sang tab "Sẵn sàng" (READY)
                                moveItem(listInProgress, position, listReady);
                                adpInProgress.notifyItemRemoved(position);
                                adpReady.notifyItemInserted(listReady.size() - 1);
                                showOnly("ready");
                            }
                        } else {
                            showErrorSnack("Cập nhật 'Sẵn sàng' thất bại.",
                                    () -> onReady(order, position));
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        showErrorSnack("Lỗi mạng khi cập nhật: " + (t.getMessage() != null ? t.getMessage() : "?"),
                                () -> onReady(order, position));
                    }
                });
            }

            @Override
            public void onComplete(@NonNull Map<String, Object> order, int position) {
                // Complete = chuyển sang DELIVERED (hoàn tất)
                String id = extractId(order);
                if (TextUtils.isEmpty(id))
                    return;

                Map<String, Object> body = new HashMap<>();
                body.put("status", "delivered");

                merchantApi.updateOrderStatus(id, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;
                        if (response.isSuccessful()) {
                            // Refresh tất cả các tab để cập nhật
                            refreshAll();
                            // Chuyển sang tab "Hoàn tất" để user thấy đơn mới
                            showOnly("completed");
                            // Hiển thị thông báo thành công
                            if (binding != null) {
                                android.view.View rootView = getView();
                                if (rootView != null) {
                                    com.google.android.material.snackbar.Snackbar.make(
                                            rootView,
                                            "Đã đánh dấu đơn hàng là hoàn tất",
                                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        } else {
                            showErrorSnack("Đánh dấu 'Hoàn tất' thất bại.",
                                    () -> onComplete(order, position));
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        showErrorSnack("Lỗi mạng khi cập nhật: " + (t.getMessage() != null ? t.getMessage() : "?"),
                                () -> onComplete(order, position));
                    }
                });
            }

            @Override
            public void onReject(@NonNull Map<String, Object> order, int position) {
                // Reject/Cancel
                String id = extractId(order);
                if (TextUtils.isEmpty(id))
                    return;

                Map<String, Object> body = new HashMap<>();
                body.put("status", "cancelled");

                merchantApi.updateOrderStatus(id, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response) {
                        if (!isAdded())
                            return;
                        if (response.isSuccessful()) {
                            removeAt(bucket, position);
                            showOnly(bucket);
                        } else {
                            showErrorSnack("Huỷ đơn thất bại.", () -> onReject(order, position));
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t) {
                        if (!isAdded())
                            return;
                        showErrorSnack("Lỗi mạng khi huỷ đơn: " + (t.getMessage() != null ? t.getMessage() : "?"),
                                () -> onReject(order, position));
                    }
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
                    androidx.navigation.fragment.NavHostFragment.findNavController(MerchantHomeFragment.this)
                            .navigate(R.id.action_merchantHomeFragment_to_orderDetailFragment, args);
                }
            }
        };
    }

    // ================= Helpers =================

    private static void moveItem(List<Map<String, Object>> from, int pos, List<Map<String, Object>> to) {
        if (pos < 0 || pos >= from.size())
            return;
        Map<String, Object> item = from.remove(pos);
        to.add(item);
    }

    private void removeAt(@NonNull String bucket, int pos) {
        switch (bucket) {
            case "new":
                removeAndNotify(listNew, pos, adpNew);
                break;
            case "in_progress":
                removeAndNotify(listInProgress, pos, adpInProgress);
                break;
            case "ready":
                removeAndNotify(listReady, pos, adpReady);
                break;
            case "completed":
                removeAndNotify(listCompleted, pos, adpCompleted);
                break;
        }
    }

    private static void removeAndNotify(List<Map<String, Object>> list, int pos, MerchantOrdersAdapter adp) {
        if (pos < 0 || pos >= list.size())
            return;
        list.remove(pos);
        adp.notifyItemRemoved(pos);
        adp.notifyItemRangeChanged(pos, Math.max(0, list.size() - pos));
    }

    @Nullable
    private static String extractId(@NonNull Map<String, Object> order) {
        Object id = order.get("order_id");
        if (id == null)
            id = order.get("code");
        return id != null ? id.toString() : null;
    }

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
