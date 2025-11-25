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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.app.network.AdminApi;
import com.example.app.network.BackendConfig;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatsFragment extends Fragment {

    private static final String TAG = "AdminStats";
    
    private AdminApi adminApi;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    
    // Revenue views
    private TextView tvTotalRevenue, tvTodayRevenue, tvCompletedOrders;
    
    // Order stats views
    private TextView tvOrdersPending, tvOrdersConfirmed, tvOrdersDelivering, tvOrdersDelivered, tvOrdersCanceled;
    
    // User stats views
    private TextView tvUsersTotal, tvUsersCustomer, tvUsersMerchant, tvUsersShipper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_stats, container, false);
        
        adminApi = BackendConfig.getRetrofit(requireContext()).create(AdminApi.class);
        
        // Initialize views
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Revenue views
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvTodayRevenue = view.findViewById(R.id.tv_today_revenue);
        tvCompletedOrders = view.findViewById(R.id.tv_completed_orders);
        
        // Order stats views
        tvOrdersPending = view.findViewById(R.id.tv_orders_pending);
        tvOrdersConfirmed = view.findViewById(R.id.tv_orders_confirmed);
        tvOrdersDelivering = view.findViewById(R.id.tv_orders_delivering);
        tvOrdersDelivered = view.findViewById(R.id.tv_orders_delivered);
        tvOrdersCanceled = view.findViewById(R.id.tv_orders_canceled);
        
        // User stats views
        tvUsersTotal = view.findViewById(R.id.tv_users_total);
        tvUsersCustomer = view.findViewById(R.id.tv_users_customer);
        tvUsersMerchant = view.findViewById(R.id.tv_users_merchant);
        tvUsersShipper = view.findViewById(R.id.tv_users_shipper);
        
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
        
        loadDetailedStats();
        
        return view;
    }

    private void loadDetailedStats() {
        progressBar.setVisibility(View.VISIBLE);
        
        Call<Map<String, Object>> call = adminApi.getDetailedStats();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    updateStatsUI(response.body());
                } else {
                    Log.e(TAG, "Failed to load detailed stats: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải thống kê", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading detailed stats", t);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatsUI(Map<String, Object> stats) {
        try {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            
            // Revenue stats
            Map<String, Object> revenue = (Map<String, Object>) stats.get("revenue");
            if (revenue != null) {
                Object totalRev = revenue.get("total");
                if (totalRev != null) {
                    double totalValue = ((Number) totalRev).doubleValue();
                    tvTotalRevenue.setText(numberFormat.format(totalValue) + "₫");
                }
                
                Object todayRev = revenue.get("today");
                if (todayRev != null) {
                    double todayValue = ((Number) todayRev).doubleValue();
                    tvTodayRevenue.setText(numberFormat.format(todayValue) + "₫");
                }
                
                Object completed = revenue.get("completedOrders");
                if (completed != null) {
                    tvCompletedOrders.setText(String.valueOf(completed));
                }
            }
            
            // Order stats
            Map<String, Object> orders = (Map<String, Object>) stats.get("orders");
            if (orders != null) {
                tvOrdersPending.setText(String.valueOf(getValueOrDefault(orders, "pending", 0)));
                tvOrdersConfirmed.setText(String.valueOf(getValueOrDefault(orders, "confirmed", 0)));
                tvOrdersDelivering.setText(String.valueOf(getValueOrDefault(orders, "delivering", 0)));
                tvOrdersDelivered.setText(String.valueOf(getValueOrDefault(orders, "delivered", 0)));
                tvOrdersCanceled.setText(String.valueOf(getValueOrDefault(orders, "canceled", 0)));
            }
            
            // User stats
            Map<String, Object> users = (Map<String, Object>) stats.get("users");
            if (users != null) {
                tvUsersTotal.setText(String.valueOf(getValueOrDefault(users, "total", 0)));
                tvUsersCustomer.setText(String.valueOf(getValueOrDefault(users, "user", 0)));
                tvUsersMerchant.setText(String.valueOf(getValueOrDefault(users, "merchant", 0)));
                tvUsersShipper.setText(String.valueOf(getValueOrDefault(users, "shipper", 0)));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing stats data", e);
            Toast.makeText(requireContext(), "Lỗi hiển thị thống kê", Toast.LENGTH_SHORT).show();
        }
    }
    
    private int getValueOrDefault(Map<String, Object> map, String key, int defaultValue) {
        try {
            Object value = map.get(key);
            if (value != null) {
                return ((Number) value).intValue();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting value for key: " + key, e);
        }
        return defaultValue;
    }
}
