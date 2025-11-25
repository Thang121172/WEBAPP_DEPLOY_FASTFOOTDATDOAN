package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app.databinding.FragmentShipperRevenueBinding;
import com.example.app.network.AuthClient;
import com.example.app.network.ShipperApi;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ShipperRevenueFragment
 * Hiển thị doanh thu và thống kê đơn hàng của shipper
 */
public class ShipperRevenueFragment extends Fragment {

    private FragmentShipperRevenueBinding binding;
    private ShipperApi shipperApi;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShipperRevenueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shipperApi = new AuthClient(requireContext())
                .getRetrofit()
                .create(ShipperApi.class);

        // Refresh button
        binding.btnRefresh.setOnClickListener(v -> loadRevenue());

        // Load data
        loadRevenue();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadRevenue() {
        shipperApi.getRevenue().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    updateUI(data);
                } else {
                    showError("Không tải được dữ liệu doanh thu");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void updateUI(Map<String, Object> data) {
        // Doanh thu hôm nay
        Object todayRevenueObj = data.get("today_revenue");
        double todayRevenue = todayRevenueObj != null ? parseDouble(todayRevenueObj) : 0.0;
        binding.tvTodayRevenue.setText(formatCurrency(todayRevenue));

        // Số đơn đã giao hôm nay
        Object todayOrdersObj = data.get("today_orders");
        int todayOrders = todayOrdersObj != null ? parseInt(todayOrdersObj) : 0;
        binding.tvTodayOrders.setText(String.valueOf(todayOrders));

        // Tổng doanh thu
        Object totalRevenueObj = data.get("total_revenue");
        double totalRevenue = totalRevenueObj != null ? parseDouble(totalRevenueObj) : 0.0;
        binding.tvTotalRevenue.setText(formatCurrency(totalRevenue));

        // Tổng số đơn đã giao
        Object totalOrdersObj = data.get("total_orders");
        int totalOrders = totalOrdersObj != null ? parseInt(totalOrdersObj) : 0;
        binding.tvTotalOrders.setText(String.valueOf(totalOrders));
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }

    private double parseDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private void showError(String msg) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
        }
    }
}

