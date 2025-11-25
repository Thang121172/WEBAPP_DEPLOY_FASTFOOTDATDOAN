package com.example.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.app.adapters.AdminUsersAdapter;
import com.example.app.network.AdminApi;
import com.example.app.network.BackendConfig;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUsersFragment extends Fragment {

    private static final String TAG = "AdminUsers";
    
    private AdminApi adminApi;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private AdminUsersAdapter adapter;
    private List<Map<String, Object>> users = new ArrayList<>();
    private ChipGroup chipGroupRole;
    private TextInputEditText etSearch;
    
    private String currentRoleFilter = null;
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);
        
        adminApi = BackendConfig.getRetrofit(requireContext()).create(AdminApi.class);
        
        recyclerView = view.findViewById(R.id.recycler_users);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvEmpty = view.findViewById(R.id.tv_empty);
        chipGroupRole = view.findViewById(R.id.chip_group_role);
        etSearch = view.findViewById(R.id.et_search);
        
        adapter = new AdminUsersAdapter(requireContext(), users, new AdminUsersAdapter.Callbacks() {
            @Override
            public void onToggleStatus(int userId, boolean isActive) {
                toggleUserStatus(userId, isActive);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadUsers);
        
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
        
        // Setup role filter chips
        chipGroupRole.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentRoleFilter = null;
            } else {
                int chipId = checkedIds.get(0);
                if (chipId == R.id.chip_all) {
                    currentRoleFilter = null;
                } else if (chipId == R.id.chip_user) {
                    currentRoleFilter = "USER";
                } else if (chipId == R.id.chip_merchant) {
                    currentRoleFilter = "MERCHANT";
                } else if (chipId == R.id.chip_shipper) {
                    currentRoleFilter = "SHIPPER";
                }
            }
            loadUsers();
        });
        
        // Setup search
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentSearch = v.getText().toString().trim();
                loadUsers();
                return true;
            }
            return false;
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // Delay search to avoid too many requests
                recyclerView.postDelayed(() -> {
                    if (etSearch.getText().toString().trim().equals(s.toString().trim())) {
                        currentSearch = s.toString().trim();
                        loadUsers();
                    }
                }, 500);
            }
        });
        
        loadUsers();
        
        return view;
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        
        String search = currentSearch.isEmpty() ? null : currentSearch;
        
        Call<Map<String, Object>> call = adminApi.getUsers(currentRoleFilter, search);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("users")) {
                        users.clear();
                        List<?> usersList = (List<?>) data.get("users");
                        if (usersList != null) {
                            for (Object item : usersList) {
                                if (item instanceof Map) {
                                    users.add((Map<String, Object>) item);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (users.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to load users: " + response.code());
                    Toast.makeText(requireContext(), "Không thể tải danh sách người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Error loading users", t);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleUserStatus(int userId, boolean isActive) {
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Boolean> body = new HashMap<>();
        body.put("is_active", isActive);
        
        Call<Map<String, Object>> call = adminApi.updateUserStatus(userId, body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful()) {
                    String message = isActive ? "Đã kích hoạt tài khoản" : "Đã khóa tài khoản";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload to update UI
                } else {
                    Log.e(TAG, "Failed to update user status: " + response.code());
                    Toast.makeText(requireContext(), "Không thể cập nhật trạng thái tài khoản", Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload to revert UI
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error updating user status", t);
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadUsers(); // Reload to revert UI
            }
        });
    }
}
