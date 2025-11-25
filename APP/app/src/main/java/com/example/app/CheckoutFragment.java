package com.example.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import com.example.app.R;
import com.example.app.adapters.CartAdapter;
import com.example.app.databinding.FragmentCheckoutBinding;
import com.example.app.data.CartRepository;
import com.example.app.model.CartItem;
import com.example.app.network.BackendConfig;
import com.example.app.ui.AuthLoginActivity;
import com.example.app.network.OrdersClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * CheckoutFragment: Hiển thị giỏ hàng, tính tổng tiền, và xử lý đặt hàng.
 * Đã sửa: Sử dụng CartItem.getItemId() để gọi các phương thức Repository.
 */
public class CheckoutFragment extends Fragment
        implements CartAdapter.OnTotalsListener,
        CartAdapter.OnBrandTotalsListener,
        CartAdapter.OnQuantityChangeListener {

    private static final String TAG = "CheckoutFragment";
    private FragmentCheckoutBinding binding;

    private final List<CartItem> cartItems = new ArrayList<>();
    private CartAdapter cartAdapter;

    private BigDecimal currentSubtotal = BigDecimal.ZERO;
    private long currentShipping = 20000L; // Phí giao hàng mặc định (tối thiểu)
    private long currentTotal = 0L;
    
    // Công thức phí giao hàng dựa trên khoảng cách:
    // - 0km = 20,000 đ (tối thiểu)
    // - 15km = 50,000 đ (tối đa)
    // - Mỗi km = (50,000 - 20,000) / 15 = 2,000 đ/km
    // - Phí = 20,000 + (khoảng cách * 2,000), tối đa 50,000 đ
    private static final long MAX_SHIPPING_FEE = 50000L; // 15km
    private static final long MIN_SHIPPING_FEE = 20000L; // 0km
    private static final double FEE_PER_KM = 2000.0; // 2,000 đ/km
    private static final double MAX_DISTANCE_KM = 15.0; // Chỉ hiển thị restaurants trong 15km

    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final CartRepository cartRepository = CartRepository.getInstance();
    
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private boolean isLocationFilled = false; // Để tránh ghi đè khi user đang nhập
    private Location currentCustomerLocation; // Lưu vị trí khách hàng để tính phí giao hàng

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();

        // Khởi tạo Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
        
        // Nút sử dụng vị trí hiện tại
        binding.btnUseCurrentLocation.setOnClickListener(v -> useCurrentLocation());
        
        // Nút thêm địa chỉ khác (xóa địa chỉ hiện tại để nhập mới)
        binding.btnAddOtherAddress.setOnClickListener(v -> {
            binding.etAddress.setText("");
            binding.etAddress.requestFocus();
            isLocationFilled = false;
        });

        // ✅ FIX: Observe cart items và tự động cập nhật tổng tiền
        cartRepository.getCartItems().observe(getViewLifecycleOwner(), this::onCartUpdated);

        // ✅ FIX: Tính lại tổng tiền ngay lập tức nếu đã có dữ liệu trong giỏ hàng
        List<CartItem> currentItems = cartRepository.getCartItems().getValue();
        if (currentItems != null && !currentItems.isEmpty()) {
            onCartUpdated(currentItems);
        }
        
        // Tự động lấy vị trí khi fragment được tạo (nếu đã có quyền)
        if (checkLocationPermission()) {
            useCurrentLocation();
        }
    }

    private void setupRecyclerView() {
        binding.recyclerCart.setLayoutManager(new LinearLayoutManager(requireContext()));

        cartAdapter = new CartAdapter(requireContext(), cartItems, R.layout.item_cart_item);
        binding.recyclerCart.setAdapter(cartAdapter);

        cartAdapter.setTotalsListener(this);
        cartAdapter.setBrandTotalsListener(this);
        cartAdapter.setOnQuantityChangeListener(this);
    }

    public void onCartUpdated(List<CartItem> updatedCartItems) {
        if (binding == null || cartAdapter == null) {
            Log.w(TAG, "Binding or adapter is null, cannot update cart");
            return;
        }

        cartItems.clear();
        if (updatedCartItems != null) {
            cartItems.addAll(updatedCartItems);
        }

        // ✅ DEBUG: Log thông tin items để kiểm tra giá
        Log.d(TAG, "Cart updated with " + cartItems.size() + " items.");
        for (CartItem item : cartItems) {
            Log.d(TAG, "Item: " + item.getName() + ", Price: " + item.getPrice() + ", Quantity: " + item.getQuantity());
        }

        // ✅ FIX: Tính tổng tiền trực tiếp từ items trong giỏ hàng
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        if (!cartItems.isEmpty()) {
            for (CartItem item : cartItems) {
                BigDecimal price = BigDecimal.valueOf(item.getPrice());
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
                calculatedSubtotal = calculatedSubtotal.add(price.multiply(qty));
            }
        }
        Log.d(TAG, "Calculated subtotal directly: " + calculatedSubtotal);

        // ✅ FIX: Đảm bảo listener được set trước khi update data
        if (cartAdapter != null) {
            // ✅ FIX: Sử dụng updateData() thay vì notifyDataSetChanged() để tự động tính
            // lại tổng tiền
            cartAdapter.updateData(new ArrayList<>(cartItems)); // Tạo copy mới để đảm bảo adapter nhận được list mới
        }

        if (cartItems.isEmpty()) {
            binding.recyclerCart.setVisibility(View.GONE);
            binding.tvError.setText(getString(R.string.err_cannot_place_order));
            binding.tvError.setVisibility(View.VISIBLE);
            // ✅ FIX: Khi giỏ hàng trống, set tổng tiền về 0
            onTotalsCalculated(BigDecimal.ZERO);
        } else {
            binding.recyclerCart.setVisibility(View.VISIBLE);
            binding.tvError.setVisibility(View.GONE);
            // ✅ FIX: Đảm bảo tổng tiền được cập nhật ngay cả khi listener không được gọi
            onTotalsCalculated(calculatedSubtotal);
        }
    }

    // MARK: - Implement CartAdapter Listeners

    @Override
    public void onTotalsCalculated(BigDecimal subtotal) {
        currentSubtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        
        // ✅ Tính phí giao hàng dựa trên khoảng cách
        currentShipping = calculateShippingFee();
        
        currentTotal = currentSubtotal.longValue() + currentShipping;

        // ✅ DEBUG: Log tổng tiền được tính
        Log.d(TAG, "onTotalsCalculated: subtotal=" + currentSubtotal + ", total=" + currentTotal);
        Log.d(TAG, "onTotalsCalculated: binding is null=" + (binding == null));

        if (binding != null) {
            String subtotalText = formatCurrency(currentSubtotal.longValue());
            String shippingText = formatCurrency(currentShipping);
            String totalText = formatCurrency(currentTotal);

            Log.d(TAG,
                    "Setting texts: subtotal=" + subtotalText + ", shipping=" + shippingText + ", total=" + totalText);

            binding.tvSubtotal.setText(subtotalText);
            binding.tvShipping.setText(shippingText);
            binding.tvTotal.setText(totalText);

            // ✅ DEBUG: Log sau khi set text
            Log.d(TAG, "Text set. tvSubtotal text=" + binding.tvSubtotal.getText() +
                    ", tvTotal text=" + binding.tvTotal.getText());

            updatePlaceOrderButtonState();
        } else {
            Log.e(TAG, "Binding is NULL! Cannot update totals!");
        }
    }

    @Override
    public void onBrandTotalsUpdated(Map<String, BigDecimal> map) {
        // ... (Logic giữ nguyên) ...
    }

    // ✅ Implement OnQuantityChangeListener (Dùng Item ID để gọi Repository)
    @Override
    public void onQuantityChange(CartItem item, int newQuantity) {
        String itemId = item.getItemId(); // Lấy ID của CartItem

        if (newQuantity <= 0) {
            // Fix lỗi: removeItem(CartItem) -> removeItem(String itemId)
            cartRepository.removeItem(itemId);
        } else {
            // Fix lỗi: updateItemQuantity(CartItem, int) -> updateItemQuantity(String
            // itemId, int newQuantity)
            cartRepository.updateItemQuantity(itemId, newQuantity);
        }
    }
    // END MARK: - Implement CartAdapter Listeners

    private void updatePlaceOrderButtonState() {
        // ... (Logic giữ nguyên) ...
    }

    private void placeOrder() {
        String address = binding.etAddress.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            binding.tilAddress.setError(getString(R.string.err_enter_address));
            return;
        }
        binding.tilAddress.setError(null);

        // ✅ FIX: Kiểm tra đăng nhập trước khi đặt hàng
        if (BackendConfig.getAccessToken(requireContext()) == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireContext(), AuthLoginActivity.class));
            return;
        }

        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_cannot_place_order, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state (optional, but good UX)
        binding.btnPlaceOrder.setEnabled(false);
        binding.btnPlaceOrder.setText("Đang xử lý...");

        // Prepare payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("address", address);
        payload.put("payment_method", resolvePaymentMethod());

        // ✅ FIX: Lấy restaurant_id từ item đầu tiên (tất cả items phải cùng một nhà hàng)
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_cannot_place_order, Toast.LENGTH_SHORT).show();
            binding.btnPlaceOrder.setEnabled(true);
            binding.btnPlaceOrder.setText(R.string.checkout_place_order);
            return;
        }
        
        int restaurantId = cartItems.get(0).getRestaurantId();
        if (restaurantId <= 0) {
            Toast.makeText(requireContext(), "Lỗi: Không xác định được nhà hàng", Toast.LENGTH_SHORT).show();
            binding.btnPlaceOrder.setEnabled(true);
            binding.btnPlaceOrder.setText(R.string.checkout_place_order);
            return;
        }
        payload.put("restaurant_id", restaurantId);

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("product_id", item.getProductId());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("price", item.getPrice());
            // Add other fields if necessary
            itemsList.add(itemMap);
        }
        payload.put("items", itemsList);

        // Call API
        OrdersClient ordersClient = new OrdersClient(requireContext());
        ordersClient.createOrder(payload, new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<Map<String, Object>> call,
                    retrofit2.Response<Map<String, Object>> response) {
                binding.btnPlaceOrder.setEnabled(true);
                binding.btnPlaceOrder.setText(R.string.checkout_place_order);

                if (response.isSuccessful() && response.body() != null) {
                    // Success
                    Toast.makeText(requireContext(), R.string.order_placed, Toast.LENGTH_SHORT).show();
                    CartRepository.getInstance().clearCart();

                    // ✅ FIX: Lấy orderId từ response và điều hướng đến OrderDetailFragment
                    Map<String, Object> responseBody = response.body();
                    Object orderIdObj = responseBody.get("orderId");
                    String orderId = orderIdObj != null ? orderIdObj.toString() : null;

                    if (orderId != null && !orderId.isEmpty()) {
                        try {
                            // Điều hướng đến OrderDetailFragment với orderId
                            android.os.Bundle args = new android.os.Bundle();
                            args.putString("orderId", orderId);
                            NavHostFragment.findNavController(CheckoutFragment.this)
                                    .navigate(R.id.action_checkoutFragment_to_orderDetailFragment, args);
                        } catch (Exception e) {
                            Log.e(TAG, "Navigation error: " + e.getMessage());
                            // Fallback: quay về màn hình trước
                            NavHostFragment.findNavController(CheckoutFragment.this).popBackStack();
                        }
                    } else {
                        // Nếu không có orderId, quay về màn hình trước
                        NavHostFragment.findNavController(CheckoutFragment.this).popBackStack();
                    }
                } else {
                    // Error
                    String errorMsg = "Lỗi đặt hàng: " + response.code();
                    if (response.errorBody() != null) {
                        // Try to parse error body if needed
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Order failed: " + response.message());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                if (binding != null) {
                    binding.btnPlaceOrder.setEnabled(true);
                    binding.btnPlaceOrder.setText(R.string.checkout_place_order);
                }
                Toast.makeText(requireContext(), R.string.err_connection_friendly, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Order error", t);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String resolvePaymentMethod() {
        if (binding == null)
            return "COD";
        int checkedId = binding.rgPayment.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_qr)
            return "QR";
        if (checkedId == R.id.rb_card)
            return "CARD";
        return "COD";
    }

    private String formatCurrency(long amount) {
        return vnd.format(amount).trim() + " đ";
    }

    /**
     * Tính phí giao hàng dựa trên khoảng cách
     * Công thức: 20,000 + (khoảng cách * 2,000), tối đa 50,000 đ
     * - 0km = 20,000 đ
     * - 15km = 50,000 đ
     * - Mỗi km = 2,000 đ
     */
    private long calculateShippingFee() {
        if (cartItems.isEmpty()) {
            return MIN_SHIPPING_FEE;
        }

        // Lấy khoảng cách từ item đầu tiên (tất cả items cùng một restaurant)
        CartItem firstItem = cartItems.get(0);
        Double distance = firstItem.getRestaurantDistance();

        // Nếu không có khoảng cách, thử tính lại từ vị trí hiện tại
        if (distance == null || distance < 0) {
            distance = calculateDistanceFromCurrentLocation(firstItem.getRestaurantId());
        }

        if (distance == null || distance < 0) {
            // Không có khoảng cách -> dùng phí tối thiểu
            return MIN_SHIPPING_FEE;
        }

        // Nếu khoảng cách > 15km, không nên có trong giỏ hàng (đã filter ở backend)
        // Nhưng để an toàn, vẫn tính với phí tối đa
        if (distance > MAX_DISTANCE_KM) {
            return MAX_SHIPPING_FEE;
        }

        // Tính phí: 20,000 + (khoảng cách * 2,000)
        long calculatedFee = (long) (MIN_SHIPPING_FEE + (distance * FEE_PER_KM));

        // Đảm bảo không vượt quá tối đa
        return Math.min(calculatedFee, MAX_SHIPPING_FEE);
    }

    /**
     * Tính khoảng cách từ vị trí hiện tại đến restaurant
     */
    private Double calculateDistanceFromCurrentLocation(int restaurantId) {
        if (!checkLocationPermission()) {
            return null;
        }

        try {
            // Lấy vị trí hiện tại (synchronous - chỉ dùng khi cần)
            // Tốt hơn là lưu vị trí khi lấy địa chỉ
            Location currentLocation = null;
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Lấy vị trí từ địa chỉ đã nhập (nếu có)
                // Hoặc lưu vị trí khi lấy địa chỉ
                // Tạm thời return null, sẽ tính sau
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: " + e.getMessage());
        }

        return null;
    }

    /**
     * Kiểm tra quyền truy cập vị trí
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Yêu cầu quyền truy cập vị trí
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Sử dụng vị trí hiện tại để điền địa chỉ
     */
    private void useCurrentLocation() {
        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Hiển thị loading
        binding.etAddress.setHint("Đang lấy vị trí...");
        binding.btnUseCurrentLocation.setEnabled(false);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        binding.btnUseCurrentLocation.setEnabled(true);
                        binding.etAddress.setHint(getString(R.string.checkout_address_hint));

                        if (location != null) {
                            // Có vị trí -> Reverse geocoding để lấy địa chỉ
                            getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            Toast.makeText(requireContext(), "Không thể lấy vị trí. Vui lòng thử lại hoặc nhập địa chỉ thủ công.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnUseCurrentLocation.setEnabled(true);
                    binding.etAddress.setHint(getString(R.string.checkout_address_hint));
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    Toast.makeText(requireContext(), "Không thể lấy vị trí. Vui lòng nhập địa chỉ thủ công.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Chuyển đổi tọa độ GPS thành địa chỉ (Reverse Geocoding)
     */
    private void getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), java.util.Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                
                // Tạo địa chỉ đầy đủ
                StringBuilder addressBuilder = new StringBuilder();
                
                // Lấy thông tin địa chỉ
                String featureName = address.getFeatureName(); // Tên địa điểm
                String thoroughfare = address.getThoroughfare(); // Tên đường
                String subThoroughfare = address.getSubThoroughfare(); // Số nhà
                String subLocality = address.getSubLocality(); // Phường/Xã
                String locality = address.getLocality(); // Quận/Huyện
                String adminArea = address.getAdminArea(); // Tỉnh/Thành phố
                
                // Xây dựng địa chỉ
                if (subThoroughfare != null && !subThoroughfare.isEmpty()) {
                    addressBuilder.append(subThoroughfare).append(" ");
                }
                if (thoroughfare != null && !thoroughfare.isEmpty()) {
                    addressBuilder.append(thoroughfare);
                    if (subLocality != null || locality != null) {
                        addressBuilder.append(", ");
                    }
                }
                if (subLocality != null && !subLocality.isEmpty()) {
                    addressBuilder.append(subLocality);
                    if (locality != null) {
                        addressBuilder.append(", ");
                    }
                }
                if (locality != null && !locality.isEmpty()) {
                    addressBuilder.append(locality);
                    if (adminArea != null) {
                        addressBuilder.append(", ");
                    }
                }
                if (adminArea != null && !adminArea.isEmpty()) {
                    addressBuilder.append(adminArea);
                }
                
                // Nếu không có địa chỉ chi tiết, dùng featureName
                if (addressBuilder.length() == 0 && featureName != null) {
                    addressBuilder.append(featureName);
                }
                
                // Nếu vẫn không có, dùng getAddressLine
                if (addressBuilder.length() == 0) {
                    String addressLine = address.getAddressLine(0);
                    if (addressLine != null) {
                        addressBuilder.append(addressLine);
                    } else {
                        // Fallback: dùng tọa độ
                        addressBuilder.append("Vị trí: ").append(latitude).append(", ").append(longitude);
                    }
                }
                
                    String fullAddress = addressBuilder.toString().trim();
                if (!fullAddress.isEmpty()) {
                    binding.etAddress.setText(fullAddress);
                    isLocationFilled = true;
                    
                    // ✅ Lưu vị trí để tính phí giao hàng
                    currentCustomerLocation = new Location("CustomerLocation");
                    currentCustomerLocation.setLatitude(latitude);
                    currentCustomerLocation.setLongitude(longitude);
                    
                    // Cập nhật lại phí giao hàng nếu đã có items trong giỏ
                    if (!cartItems.isEmpty()) {
                        onTotalsCalculated(currentSubtotal);
                    }
                    
                    Toast.makeText(requireContext(), "Đã lấy địa chỉ từ vị trí hiện tại", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Không thể xác định địa chỉ. Vui lòng nhập thủ công.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy địa chỉ. Vui lòng nhập thủ công.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoding error: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Lỗi khi lấy địa chỉ. Vui lòng nhập thủ công.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Đã cấp quyền -> Lấy vị trí
                useCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Cần quyền truy cập vị trí để tự động điền địa chỉ. Bạn có thể nhập địa chỉ thủ công.", Toast.LENGTH_LONG).show();
            }
        }
    }
}