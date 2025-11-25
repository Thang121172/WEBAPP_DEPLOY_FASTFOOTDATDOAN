package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.app.ui.customer.StoreDiscoveryFragment;
import com.example.app.databinding.FragmentCustomerHomeBinding;

// 👇 THÊM
import com.example.app.data.CartRepository;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * CustomerHomeFragment (Chức năng: Khám phá Quán ăn & Điều hướng)
 *
 * Nhiệm vụ: Tải StoreDiscoveryFragment vào giao diện chính và xử lý điều hướng.
 */
public class CustomerHomeFragment extends Fragment {

    private FragmentCustomerHomeBinding binding;

    // 👇 THÊM: dùng chung CartRepository (singleton)
    private CartRepository cartRepository;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Tải Fragment con (Store Discovery)
        loadStoreDiscoveryFragment();

        // 2. Khởi tạo CartRepository
        cartRepository = CartRepository.getInstance();

        // ✅ FIX: Thêm nút "Đơn hàng của bạn"
        final MaterialButton myOrdersButton = binding.btnMyOrders;
        if (myOrdersButton != null) {
            myOrdersButton.setOnClickListener(v -> {
                Log.d("CustomerHomeFragment", "Chuyển đến màn hình Đơn hàng của bạn.");
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_customerHomeFragment_to_customerOrdersFragment);
            });
        }

        // 3. Lấy reference tới nút "Xem Giỏ Hàng"
        //    (giả sử ID trong XML là btn_view_cart_checkout – trùng với code cũ)
        final MaterialButton checkoutButton = binding.btnViewCartCheckout;

        if (checkoutButton != null) {

            // 3.1 Observe tổng tiền giỏ hàng -> update text + visibility
            cartRepository.getCartSubtotal().observe(
                    getViewLifecycleOwner(),
                    totalDouble -> {
                        double total = (totalDouble != null) ? totalDouble : 0.0;
                        long totalLong = (long) total;

                        if (totalLong > 0) {
                            String text = "Xem Giỏ Hàng (" +
                                    formatCurrency(totalLong) + " đ)";
                            checkoutButton.setText(text);
                            checkoutButton.setVisibility(View.VISIBLE);
                        } else {
                            checkoutButton.setText("Xem Giỏ Hàng (0đ)");
                            // tuỳ UX: ẩn khi 0đ hoặc vẫn hiện
                            checkoutButton.setVisibility(View.GONE);
                        }
                    }
            );

            // 3.2 Click -> điều hướng tới màn thanh toán
            checkoutButton.setOnClickListener(v -> {
                Log.d("CustomerHomeFragment", "Chuyển đến màn hình Thanh toán.");
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_customerHomeFragment_to_checkoutFragment);
            });

        } else {
            Log.e("CustomerHomeFragment",
                    "LỖI LOGIC: Không tìm thấy nút Checkout/Giỏ hàng trong fragment_customer_home.xml");
        }
    }

    /**
     * Tải StoreDiscoveryFragment vào FragmentContainerView.
     */
    private void loadStoreDiscoveryFragment() {
        final int containerId = R.id.fragment_container_store_discovery;

        if (getChildFragmentManager().findFragmentById(containerId) == null) {

            if (binding.getRoot().findViewById(containerId) == null) {
                Log.e("CustomerHomeFragment",
                        "LỖI: Không tìm thấy ID container R.id.fragment_container_store_discovery.");
                return;
            }

            Fragment discoveryFragment = new StoreDiscoveryFragment();

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(containerId, discoveryFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }

    // 👇 THÊM: format tiền giống bên RestaurantDetailFragment
    private String formatCurrency(long amount) {
        NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return vnd.format(amount).trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
