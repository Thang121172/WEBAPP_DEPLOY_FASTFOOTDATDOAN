package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.app.adapters.AdminOrdersAdapter;
import com.example.app.network.AdminApi;
import com.example.app.network.BackendConfig;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminOrdersFragment extends Fragment {

    private static final String TAG = "AdminOrders";
    
    private AdminApi adminApi;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private AdminOrdersAdapter adapter;
    private List<Map<String, Object>> allOrders = new ArrayList<>();
    private List<Map<String, Object>> filteredOrders = new ArrayList<>();
    private String currentStatus = null; // null = all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_orders, container, false);
        
        adminApi = BackendConfig.getRetrofit(requireContext()).create(AdminApi.class);
        
        recyclerView = view.findViewById(R.id.recycler_orders);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tabLayout = view.findViewById(R.id.tab_layout);
        
        adapter = new AdminOrdersAdapter(filteredOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadOrders);
        
        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ xử lý"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã giao"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã hủy"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentStatus = null; // All
                        break;
                    case 1:
                        currentStatus = "PENDING"; // Pending (includes PENDING and CONFIRMED)
                        break;
                    case 2:
                        currentStatus = "DELIVERED"; // Delivered
                        break;
                    case 3:
                        currentStatus = "CANCELED"; // Canceled
                        break;
                }
                // Filter orders on client side without reloading
                filterOrders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Reload when tab is reselected
                loadOrders();
            }
        });
        
        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null && getActivity() != null) {
            ((androidx.appcompat.app.AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
        
        loadOrders();
        
        return view;
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        
        // Always load all orders, then filter on client side
        Call<Map<String, Object>> call = adminApi.getOrders(null);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("orders")) {
                        allOrders.clear();
                        List<?> ordersList = (List<?>) data.get("orders");
                        if (ordersList != null) {
                            for (Object item : ordersList) {
                                if (item instanceof Map) {
                                    allOrders.add((Map<String, Object>) item);
                                }
                            }
                        }
                        filterOrders();
                    }
                } else {
                    Log.e(TAG, "Failed to load orders: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải danh sách đơn hàng", Toast.LENGTH_SHORT).show();
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Error loading orders", t);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private void filterOrders() {
        filteredOrders.clear();
        
        if (currentStatus == null) {
            // Show all orders
            filteredOrders.addAll(allOrders);
        } else {
            // Filter by status
            for (Map<String, Object> order : allOrders) {
                Object statusObj = order.get("status");
                String status = statusObj != null ? statusObj.toString() : "";
                
                if (currentStatus.equals("PENDING")) {
                    // Pending includes PENDING and CONFIRMED
                    if ("PENDING".equals(status) || "CONFIRMED".equals(status)) {
                        filteredOrders.add(order);
                    }
                } else if (currentStatus.equals(status)) {
                    filteredOrders.add(order);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (filteredOrders.isEmpty()) {
            showEmpty();
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        if (currentStatus == null) {
            tvEmpty.setText("Không có đơn hàng nào");
        } else if (currentStatus.equals("PENDING")) {
            tvEmpty.setText("Không có đơn hàng chờ xử lý");
        } else if (currentStatus.equals("DELIVERED")) {
            tvEmpty.setText("Không có đơn hàng đã giao");
        } else if (currentStatus.equals("CANCELED")) {
            tvEmpty.setText("Không có đơn hàng đã hủy");
        }
    }
}

