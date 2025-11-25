package com.example.app.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app.model.CartItem;
import com.example.app.model.MenuItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * CartRepository: Quản lý dữ liệu giỏ hàng (Singleton Pattern).
 */
public class CartRepository {

    private static volatile CartRepository instance;

    // LiveData danh sách item trong giỏ
    private final MutableLiveData<List<CartItem>> _cartItems =
            new MutableLiveData<>(new ArrayList<>());

    // LiveData tổng tiền giỏ hàng
    private final MutableLiveData<Double> _cartSubtotal =
            new MutableLiveData<>(0.0);

    private CartRepository() {
    }

    public static CartRepository getInstance() {
        if (instance == null) {
            synchronized (CartRepository.class) {
                if (instance == null) {
                    instance = new CartRepository();
                }
            }
        }
        return instance;
    }

    public LiveData<List<CartItem>> getCartItems() {
        return _cartItems;
    }

    public LiveData<Double> getCartSubtotal() {
        return _cartSubtotal;
    }

    /**
     * ➤ Thêm món vào giỏ hàng (MenuItem → CartItem)
     */
    public void addToCart(MenuItem menuItem) {
        addToCart(menuItem, null);
    }

    /**
     * ➤ Thêm món vào giỏ hàng với khoảng cách
     */
    public void addToCart(MenuItem menuItem, Double restaurantDistance) {
        if (menuItem == null) return;
        
        String productId = menuItem.getId();
        if (productId == null || productId.trim().isEmpty()) {
            // Nếu không có ID, dùng title làm key (fallback)
            productId = menuItem.getTitle() != null ? menuItem.getTitle() : "";
        } else {
            productId = productId.trim();
        }

        List<CartItem> currentItems = _cartItems.getValue();
        if (currentItems == null) currentItems = new ArrayList<>();

        // kiểm tra xem đã có món này trong giỏ hay chưa (so sánh productId)
        CartItem existing = null;
        for (CartItem ci : currentItems) {
            String ciProductId = ci.productId != null ? ci.productId.trim() : "";
            if (productId.equals(ciProductId)) {
                existing = ci;
                break;
            }
        }

        if (existing == null) {
            // Tạo id duy nhất cho cartItem
            String cartItemId = java.util.UUID.randomUUID().toString();

            // Constructor với 9 tham số (có thêm restaurantDistance):
            CartItem newItem = new CartItem(
                    cartItemId,               // cartItemId
                    productId,                // productId
                    menuItem.getTitle(),      // title
                    menuItem.getPrice(),      // price
                    1,                        // quantity
                    menuItem.getImageUrl(),   // imageUrl
                    null,                     // options
                    menuItem.getRestaurantId(), // restaurantId
                    restaurantDistance        // restaurantDistance
            );

            currentItems.add(newItem);

        } else {
            // nếu đã có → tăng số lượng
            existing.setQuantity(existing.getQuantity() + 1);
            // Cập nhật distance nếu chưa có
            if (existing.restaurantDistance == null && restaurantDistance != null) {
                existing.restaurantDistance = restaurantDistance;
            }
        }

        // cập nhật LiveData
        _cartItems.setValue(currentItems);
        recalculateSubtotal(currentItems);
    }


    /**
     * ➤ Cập nhật số lượng item trong giỏ
     */
    public void updateItemQuantity(String cartItemId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(cartItemId);
            return;
        }

        List<CartItem> items = _cartItems.getValue();
        if (items == null) return;

        for (CartItem item : items) {
            if (item.cartItemId.equals(cartItemId)) {
                item.setQuantity(newQuantity);
                break;
            }
        }

        _cartItems.setValue(items);
        recalculateSubtotal(items);
    }

    /**
     * ➤ Xóa 1 item trong giỏ
     */
    public void removeItem(String cartItemId) {
        List<CartItem> items = _cartItems.getValue();
        if (items == null) return;

        boolean removed = items.removeIf(item -> item.cartItemId.equals(cartItemId));

        if (removed) {
            _cartItems.setValue(items);
            recalculateSubtotal(items);
        }
    }

    /**
     * ➤ Tính lại tổng tiền
     */
    private void recalculateSubtotal(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            BigDecimal price = BigDecimal.valueOf(item.getPrice());
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            total = total.add(price.multiply(qty));
        }

        _cartSubtotal.setValue(total.doubleValue());
    }

    /**
     * ➤ Xóa toàn bộ giỏ hàng
     */
    public void clearCart() {
        _cartItems.setValue(new ArrayList<>());
        _cartSubtotal.setValue(0.0);
    }
}
