package com.example.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app.databinding.FragmentShipperProfileBinding;
import com.example.app.network.AuthClient;
import com.example.app.network.ShipperApi;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ShipperProfileFragment
 * Cho phép shipper cập nhật thông tin: phone, vehicle_plate
 */
public class ShipperProfileFragment extends Fragment {

    private FragmentShipperProfileBinding binding;
    private ShipperApi shipperApi;
    private boolean isEditing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShipperProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shipperApi = new AuthClient(requireContext())
                .getRetrofit()
                .create(ShipperApi.class);

        // Load profile
        loadProfile();

        // Edit button
        if (binding.btnEdit != null) {
            binding.btnEdit.setOnClickListener(v -> enableEditing());
        }

        // Save button
        binding.btnSave.setOnClickListener(v -> saveProfile());
        
        // Ban đầu khóa tất cả các trường
        setFieldsEnabled(false);
        isEditing = false;
        if (binding.btnEdit != null) {
            binding.btnEdit.setVisibility(View.VISIBLE);
        }
        binding.btnSave.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadProfile() {
        shipperApi.getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> profile = response.body();
                    updateUI(profile);
                } else {
                    showError("Không tải được thông tin hồ sơ");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void updateUI(Map<String, Object> profile) {
        Object username = profile.get("username");
        Object email = profile.get("email");
        Object phone = profile.get("phone");
        Object vehiclePlate = profile.get("vehicle_plate");

        if (username != null) {
            binding.etUsername.setText(username.toString());
        }
        if (email != null) {
            binding.etEmail.setText(email.toString());
        }
        if (phone != null) {
            binding.etPhone.setText(phone.toString());
        }
        if (vehiclePlate != null) {
            binding.etVehiclePlate.setText(vehiclePlate.toString());
        }
        
        // Sau khi load xong, khóa tất cả các trường và hiển thị nút "Chỉnh sửa"
        setFieldsEnabled(false);
        isEditing = false;
        if (binding.btnEdit != null) {
            binding.btnEdit.setVisibility(View.VISIBLE);
        }
        binding.btnSave.setVisibility(View.GONE);
    }

    private void saveProfile() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String vehiclePlate = binding.etVehiclePlate.getText().toString().trim();

        Map<String, Object> body = new HashMap<>();
        if (!TextUtils.isEmpty(username)) {
            body.put("username", username);
        }
        if (!TextUtils.isEmpty(email)) {
            body.put("email", email);
        }
        if (!TextUtils.isEmpty(phone)) {
            body.put("phone", phone);
        }
        if (!TextUtils.isEmpty(vehiclePlate)) {
            body.put("vehicle_plate", vehiclePlate);
        }

        if (body.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập thông tin cần cập nhật", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);
        shipperApi.updateProfile(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                binding.btnSave.setEnabled(true);
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Đã cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    // ✅ FIX: Khóa tất cả các trường sau khi lưu thành công
                    setFieldsEnabled(false);
                    isEditing = false;
                    if (binding.btnEdit != null) {
                        binding.btnEdit.setVisibility(View.VISIBLE);
                    }
                    binding.btnSave.setVisibility(View.GONE);
                    // Reload profile để đảm bảo hiển thị đúng dữ liệu mới
                    loadProfile();
                } else {
                    String errorMsg = "Cập nhật thất bại";
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            if (errorStr.contains("username_already_exists")) {
                                errorMsg = "Tên đăng nhập đã tồn tại";
                            } else if (errorStr.contains("email_already_exists")) {
                                errorMsg = "Email đã tồn tại";
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                binding.btnSave.setEnabled(true);
                if (!isAdded()) return;
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void enableEditing() {
        isEditing = true;
        setFieldsEnabled(true);
        if (binding.btnEdit != null) {
            binding.btnEdit.setVisibility(View.GONE);
        }
        binding.btnSave.setVisibility(View.VISIBLE);
    }

    private void setFieldsEnabled(boolean enabled) {
        // Username và Email luôn bị khóa (không cho sửa)
        binding.etUsername.setEnabled(false); // Luôn khóa
        binding.etEmail.setEnabled(false); // Luôn khóa
        // Phone và vehicle_plate có thể bật/tắt tùy theo enabled parameter
        binding.etPhone.setEnabled(enabled);
        binding.etVehiclePlate.setEnabled(enabled);
    }

    private void showError(String msg) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
        }
    }
}

