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
import androidx.navigation.Navigation;

import com.example.app.network.AdminApi;
import com.example.app.network.BackendConfig;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminHomeFragment extends Fragment {

    private static final String TAG = "AdminHomeFragment";
    
    private AdminApi adminApi;
    private ProgressBar progressBar;
    private TextView tvTotalOrders, tvCancelRequests, tvRevenue, tvUsers, tvPendingCancelCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminApi = BackendConfig.getRetrofit(requireContext()).create(AdminApi.class);

        // Initialize views
        progressBar = view.findViewById(R.id.progress_bar);
        tvTotalOrders = view.findViewById(R.id.tv_total_orders_value);
        tvCancelRequests = view.findViewById(R.id.tv_cancel_requests_value);
        tvRevenue = view.findViewById(R.id.tv_revenue_value);
        tvUsers = view.findViewById(R.id.tv_users_value);
        tvPendingCancelCount = view.findViewById(R.id.tv_pending_cancel_count);

        // Setup click listeners
        view.findViewById(R.id.card_manage_orders).setOnClickListener(v -> {
            // Navigate to manage orders screen
            AdminOrdersFragment fragment = new AdminOrdersFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null)
                .commit();
        });

        view.findViewById(R.id.card_cancel_requests_list).setOnClickListener(v -> {
            // Navigate to cancel requests screen
            AdminCancelRequestsFragment fragment = new AdminCancelRequestsFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null)
                .commit();
        });

        view.findViewById(R.id.card_manage_users).setOnClickListener(v -> {
            // Navigate to manage users screen
            AdminUsersFragment fragment = new AdminUsersFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null)
                .commit();
        });

        view.findViewById(R.id.card_view_stats).setOnClickListener(v -> {
            // Navigate to detailed stats screen
            AdminStatsFragment fragment = new AdminStatsFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null)
                .commit();
        });

        // Load dashboard data
        loadDashboardData();
    }

    private void loadDashboardData() {
        progressBar.setVisibility(View.VISIBLE);

        // Load detailed stats
        Call<Map<String, Object>> call = adminApi.getDetailedStats();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    updateDashboard(response.body());
                } else {
                    Log.e(TAG, "Failed to load stats: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading stats", t);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Load cancel requests count
        Call<Map<String, Object>> cancelCall = adminApi.getCancelRequests();
        cancelCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("orders")) {
                        java.util.List<?> orders = (java.util.List<?>) data.get("orders");
                        int count = orders != null ? orders.size() : 0;
                        tvCancelRequests.setText(String.valueOf(count));
                        tvPendingCancelCount.setText(String.valueOf(count));
                        
                        if (count > 0) {
                            tvPendingCancelCount.setVisibility(View.VISIBLE);
                        } else {
                            tvPendingCancelCount.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error loading cancel requests", t);
            }
        });
    }

    private void updateDashboard(Map<String, Object> stats) {
        try {
            // Update orders
            Map<String, Object> orders = (Map<String, Object>) stats.get("orders");
            if (orders != null) {
                Object total = orders.get("total");
                if (total != null) {
                    tvTotalOrders.setText(String.valueOf(total));
                }
            }

            // Update revenue
            Map<String, Object> revenue = (Map<String, Object>) stats.get("revenue");
            if (revenue != null) {
                Object totalRev = revenue.get("total");
                if (totalRev != null) {
                    double totalValue = ((Number) totalRev).doubleValue();
                    NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
                    tvRevenue.setText(formatter.format(totalValue) + "₫");
                }
            }

            // Update users
            Map<String, Object> users = (Map<String, Object>) stats.get("users");
            if (users != null) {
                Object totalUsers = users.get("total");
                if (totalUsers != null) {
                    tvUsers.setText(String.valueOf(totalUsers));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing dashboard stats", e);
        }
    }
}
