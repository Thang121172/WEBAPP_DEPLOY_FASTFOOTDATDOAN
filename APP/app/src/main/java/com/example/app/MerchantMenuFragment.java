package com.example.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app.adapters.MerchantMenuAdapter;
import com.example.app.databinding.FragmentMerchantMenuBinding;
import com.example.app.network.AuthClient;
import com.example.app.network.MerchantApi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MerchantMenuFragment
 * Quản lý món ăn: xem, thêm, sửa, xóa
 */
public class MerchantMenuFragment extends Fragment {

    private FragmentMerchantMenuBinding binding;
    private MerchantApi merchantApi;
    private MerchantMenuAdapter adapter;
    private final List<Map<String, Object>> menuItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMerchantMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        merchantApi = new AuthClient(requireContext())
                .getRetrofit()
                .create(MerchantApi.class);

        // Setup RecyclerView
        binding.recyclerMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MerchantMenuAdapter(requireContext(), menuItems, new MerchantMenuAdapter.Listener() {
            @Override
            public void onEdit(Map<String, Object> item, int position) {
                showEditDialog(item, position);
            }

            @Override
            public void onDelete(Map<String, Object> item, int position) {
                showDeleteConfirm(item, position);
            }
        });
        binding.recyclerMenu.setAdapter(adapter);

        // Refresh
        binding.swipeRefresh.setOnRefreshListener(this::loadMenu);

        // Add button
        binding.btnAddItem.setOnClickListener(v -> showAddDialog());

        // Load data
        binding.swipeRefresh.setRefreshing(true);
        loadMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadMenu() {
        merchantApi.getMerchantMenu().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call,
                                   @NonNull Response<List<Map<String, Object>>> response) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    menuItems.clear();
                    menuItems.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                } else {
                    showError("Không tải được danh sách món ăn");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_menu_item, null);
        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        EditText etPrice = dialogView.findViewById(R.id.et_price);
        EditText etImageUrl = dialogView.findViewById(R.id.et_image_url);
        ImageView imgPreview = dialogView.findViewById(R.id.img_preview);
        MaterialButton btnSelectImage = dialogView.findViewById(R.id.btn_select_image);

        // Setup image selection
        Uri[] selectedImageUri = {null}; // Mảng để lưu Uri (dùng array để có thể modify trong lambda)
        
        ActivityResultLauncher<Intent> dialogImagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri[0] = uri;
                            // Hiển thị preview
                            imgPreview.setVisibility(View.VISIBLE);
                            Glide.with(requireContext()).load(uri).into(imgPreview);
                            
                            // Upload ảnh
                            uploadImage(uri, (uploadedUrl) -> {
                                if (uploadedUrl != null) {
                                    etImageUrl.setText(uploadedUrl);
                                    Toast.makeText(requireContext(), "Upload ảnh thành công", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
        );

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            dialogImagePicker.launch(intent);
        });

        // Load preview nếu có URL sẵn
        String existingUrl = etImageUrl.getText().toString().trim();
        if (!TextUtils.isEmpty(existingUrl)) {
            imgPreview.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(existingUrl).into(imgPreview);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Thêm món ăn")
                .setView(dialogView)
                .setPositiveButton("Thêm", null) // Set null để override sau
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String imageUrl = etImageUrl.getText().toString().trim();

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
                    Toast.makeText(requireContext(), "Vui lòng nhập tên và giá", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double price = Double.parseDouble(priceStr);
                    if (price < 0) {
                        Toast.makeText(requireContext(), "Giá phải >= 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    if (!TextUtils.isEmpty(description)) body.put("description", description);
                    body.put("price", price);
                    if (!TextUtils.isEmpty(imageUrl)) body.put("image_url", imageUrl);

                    addMenuItem(body);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showEditDialog(Map<String, Object> item, int position) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_menu_item, null);
        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        EditText etPrice = dialogView.findViewById(R.id.et_price);
        EditText etImageUrl = dialogView.findViewById(R.id.et_image_url);
        ImageView imgPreview = dialogView.findViewById(R.id.img_preview);
        MaterialButton btnSelectImage = dialogView.findViewById(R.id.btn_select_image);

        // Fill existing data
        String existingImageUrl = getString(item.get("image_url"));
        etName.setText(getString(item.get("name")));
        etDescription.setText(getString(item.get("description")));
        etPrice.setText(getString(item.get("price")));
        etImageUrl.setText(existingImageUrl);

        // Hiển thị preview nếu có ảnh
        if (!TextUtils.isEmpty(existingImageUrl)) {
            imgPreview.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(existingImageUrl).into(imgPreview);
        }

        Object idObj = item.get("id");
        String id = idObj != null ? idObj.toString() : null;

        // Setup image selection
        Uri[] selectedImageUri = {null};
        
        ActivityResultLauncher<Intent> dialogImagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri[0] = uri;
                            // Hiển thị preview
                            imgPreview.setVisibility(View.VISIBLE);
                            Glide.with(requireContext()).load(uri).into(imgPreview);
                            
                            // Upload ảnh
                            uploadImage(uri, (uploadedUrl) -> {
                                if (uploadedUrl != null) {
                                    etImageUrl.setText(uploadedUrl);
                                    Toast.makeText(requireContext(), "Upload ảnh thành công", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
        );

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            dialogImagePicker.launch(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Sửa món ăn")
                .setView(dialogView)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String imageUrl = etImageUrl.getText().toString().trim();

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
                    Toast.makeText(requireContext(), "Vui lòng nhập tên và giá", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double price = Double.parseDouble(priceStr);
                    if (price < 0) {
                        Toast.makeText(requireContext(), "Giá phải >= 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    body.put("description", description);
                    body.put("price", price);
                    body.put("image_url", imageUrl);

                    updateMenuItem(id, body, position);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showDeleteConfirm(Map<String, Object> item, int position) {
        Object idObj = item.get("id");
        String id = idObj != null ? idObj.toString() : null;
        String name = getString(item.get("name"));

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa món ăn")
                .setMessage("Bạn có chắc muốn xóa \"" + name + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteMenuItem(id, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addMenuItem(Map<String, Object> body) {
        merchantApi.addMenuItem(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Thêm món thành công", Toast.LENGTH_SHORT).show();
                    loadMenu();
                } else {
                    // ✅ FIX: Hiển thị lỗi chi tiết từ backend
                    String errorMsg = "Thêm món thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("MerchantMenuFragment", "Add menu error: " + errorBody);
                            if (errorBody.contains("message")) {
                                // Parse JSON error nếu có
                                errorMsg = "Thêm món thất bại: " + errorBody;
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MerchantMenuFragment", "Error parsing error body", e);
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                android.util.Log.e("MerchantMenuFragment", "Add menu failure", t);
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void updateMenuItem(String id, Map<String, Object> body, int position) {
        merchantApi.updateMenuItem(id, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    loadMenu();
                } else {
                    showError("Cập nhật thất bại");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void deleteMenuItem(String id, int position) {
        merchantApi.deleteMenuItem(id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Xóa thành công", Toast.LENGTH_SHORT).show();
                    loadMenu();
                } else {
                    showError("Xóa thất bại");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showError("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "?"));
            }
        });
    }

    private void updateEmptyState() {
        if (binding != null) {
            binding.layoutEmpty.setVisibility(menuItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String msg) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
        }
    }

    private static String getString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    // Upload ảnh lên server
    private void uploadImage(Uri imageUri, ImageUploadCallback callback) {
        try {
            // Convert Uri to File
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onComplete(null);
                return;
            }

            // Tạo file tạm
            File tempFile = new File(requireContext().getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            // Tạo RequestBody từ file
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", tempFile.getName(), requestFile);

            // Upload
            merchantApi.uploadImage(imagePart).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call,
                                       @NonNull Response<Map<String, Object>> response) {
                    // Xóa file tạm
                    tempFile.delete();
                    
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> result = response.body();
                        String url = (String) result.get("url");
                        callback.onComplete(url);
                    } else {
                        callback.onComplete(null);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    // Xóa file tạm
                    tempFile.delete();
                    android.util.Log.e("MerchantMenuFragment", "Upload image failure", t);
                    callback.onComplete(null);
                }
            });

        } catch (Exception e) {
            android.util.Log.e("MerchantMenuFragment", "Error preparing image for upload", e);
            callback.onComplete(null);
        }
    }

    // Callback interface cho upload
    interface ImageUploadCallback {
        void onComplete(String imageUrl);
    }
}

