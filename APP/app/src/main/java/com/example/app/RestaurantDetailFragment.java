package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // 👈 THÊM

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.adapters.MenuAdapter;
import com.example.app.model.MenuItem;
import com.example.app.viewmodels.RestaurantDetailViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * RestaurantDetailFragment
 * - Hiển thị chi tiết nhà hàng và danh sách menu món ăn.
 */
public class RestaurantDetailFragment extends Fragment {

    private static final String TAG = "RestaurantDetailFrag";

    // KEY trùng với StoreDiscoveryFragment + nav_graph
    public static final String ARG_RESTAURANT_ID = "restaurantId";

    private String restaurantId;
    private RestaurantDetailViewModel viewModel;
    private MenuAdapter menuAdapter;

    // UI Components
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivBanner;
    private Toolbar toolbar;
    private TextView tvInfo;
    private RecyclerView recyclerViewMenu;
    private MaterialButton btnViewCart;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            // thử đọc cả 2 key cho chắc
            restaurantId = args.getString(ARG_RESTAURANT_ID);
            if (restaurantId == null) {
                restaurantId = args.getString("restaurant_id");
            }
            Log.d(TAG, "Nhận Restaurant ID từ arguments: " + restaurantId);
        } else {
            Log.e(TAG, "getArguments() == null trong onCreate");
        }

        // Init ViewModel sớm
        viewModel = new ViewModelProvider(this).get(RestaurantDetailViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_restaurant_detail, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View Components
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar);
        ivBanner = view.findViewById(R.id.iv_restaurant_banner);
        toolbar = view.findViewById(R.id.toolbar);
        tvInfo = view.findViewById(R.id.tv_restaurant_info);
        recyclerViewMenu = view.findViewById(R.id.recycler_menu);
        btnViewCart = view.findViewById(R.id.btn_view_cart_floating);

        // Title ban đầu
        collapsingToolbar.setTitle("Đang tải...");

        // RecyclerView cho Menu
        recyclerViewMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        menuAdapter = new MenuAdapter(
                new ArrayList<MenuItem>(),
                item -> {
                    // Click vào cả item (nếu sau này muốn mở chi tiết món)
                    Log.d(TAG, "Clicked menu item: " + item.getTitle());
                },
                item -> {
                    // 👇 BẤM NÚT "THÊM" -> CỘNG VÀO GIỎ HÀNG
                    Log.d(TAG, "AddToCart clicked: " + item.getTitle());
                    viewModel.addMenuItemToCart(item);
                    Toast.makeText(
                            requireContext(),
                            "Đã thêm: " + item.getTitle(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
        recyclerViewMenu.setAdapter(menuAdapter);

        // Luôn observe ViewModel
        observeViewModel();

        // Nếu có restaurantId -> gọi API
        if (restaurantId != null && !restaurantId.isEmpty()) {
            Log.d(TAG, "Gọi viewModel.loadRestaurantDetail với id = " + restaurantId);
            viewModel.loadRestaurantDetail(restaurantId);
        } else {
            Log.e(TAG, "restaurantId null hoặc rỗng, không thể gọi API");
            collapsingToolbar.setTitle("Thiếu ID cửa hàng");
            tvInfo.setText("Không nhận được mã cửa hàng. Vui lòng quay lại và chọn lại.");
        }

        // Nút Giỏ hàng
        btnViewCart.setOnClickListener(v -> {
            Log.d(TAG, "Chuyển sang Giỏ hàng.");
            Intent intent = new Intent(requireContext(), CartActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        if (viewModel == null) return;

        // Chi tiết nhà hàng
        viewModel.getRestaurantDetail().observe(getViewLifecycleOwner(), restaurant -> {
            if (restaurant != null) {
                collapsingToolbar.setTitle(restaurant.getName());
                tvInfo.setText(String.format(
                        "Địa chỉ: %s | Đánh giá: %.1f",
                        restaurant.getAddress(),
                        restaurant.getRating()
                ));
            } else {
                Log.w(TAG, "Restaurant detail null.");
                collapsingToolbar.setTitle("Không tải được cửa hàng");
                tvInfo.setText("Không thể tải thông tin cửa hàng.");
            }
        });

        // Danh sách menu
        viewModel.getMenuList().observe(getViewLifecycleOwner(), menuList -> {
            if (menuAdapter != null && menuList != null) {
                menuAdapter.updateData(menuList);
                Log.d(TAG, "Đã tải " + menuList.size() + " món ăn.");
            } else {
                Log.w(TAG, "Menu list null hoặc adapter null.");
            }
        });

        // Tổng tiền giỏ hàng (update nút nổi)
        viewModel.getCartTotal().observe(getViewLifecycleOwner(), total -> {
            if (total != null && total.longValue() > 0) {
                btnViewCart.setText(
                        String.format("Xem Giỏ Hàng (%s đ)", formatCurrency(total.longValue()))
                );
                btnViewCart.setVisibility(View.VISIBLE);
            } else {
                btnViewCart.setVisibility(View.GONE);
            }
        });

        // Lỗi từ ViewModel
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Log.e(TAG, "Error từ ViewModel: " + message);
                collapsingToolbar.setTitle("Lỗi tải dữ liệu");
                tvInfo.setText(message);
            }
        });
    }

    private String formatCurrency(long amount) {
        NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return vnd.format(amount).trim();
    }
}
