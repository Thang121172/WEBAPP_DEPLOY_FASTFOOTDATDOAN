package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.app.adapters.AdminCancelRequestsAdapter;
import com.example.app.network.AdminApi;
import com.example.app.network.BackendConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCancelRequestsFragment extends Fragment {

    private static final String TAG = "AdminCancelRequests";
    
    private AdminApi adminApi;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private AdminCancelRequestsAdapter adapter;
    private List<Map<String, Object>> orders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_cancel_requests, container, false);
        
        adminApi = BackendConfig.getRetrofit(requireContext()).create(AdminApi.class);
        
        recyclerView = view.findViewById(R.id.recycler_cancel_requests);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        
        adapter = new AdminCancelRequestsAdapter(orders, new AdminCancelRequestsAdapter.Callbacks() {
            @Override
            public void onApprove(int orderId) {
                approveCancel(orderId);
            }

            @Override
            public void onReject(int orderId) {
                rejectCancel(orderId);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadCancelRequests);
        
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
        
        loadCancelRequests();
        
        return view;
    }

    private void loadCancelRequests() {
        progressBar.setVisibility(View.VISIBLE);
        
        Call<Map<String, Object>> call = adminApi.getCancelRequests();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("orders")) {
                        orders.clear();
                        List<?> ordersList = (List<?>) data.get("orders");
                        if (ordersList != null) {
                            for (Object item : ordersList) {
                                if (item instanceof Map) {
                                    orders.add((Map<String, Object>) item);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (orders.isEmpty()) {
                            Toast.makeText(requireContext(), "Không có yêu cầu hủy đơn nào", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to load cancel requests: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Error loading cancel requests", t);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveCancel(int orderId) {
        progressBar.setVisibility(View.VISIBLE);
        
        Call<Map<String, Object>> call = adminApi.approveCancel(orderId);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Đã duyệt hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    loadCancelRequests();
                } else {
                    Log.e(TAG, "Failed to approve cancel: " + response.code());
                    Toast.makeText(requireContext(), "Không thể duyệt hủy đơn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error approving cancel", t);
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectCancel(int orderId) {
        Map<String, String> body = new HashMap<>();
        body.put("reason", "Admin từ chối yêu cầu hủy đơn");
        
        progressBar.setVisibility(View.VISIBLE);
        
        Call<Map<String, Object>> call = adminApi.rejectCancel(orderId, body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Đã từ chối yêu cầu hủy", Toast.LENGTH_SHORT).show();
                    loadCancelRequests();
                } else {
                    Log.e(TAG, "Failed to reject cancel: " + response.code());
                    Toast.makeText(requireContext(), "Không thể từ chối yêu cầu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error rejecting cancel", t);
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

