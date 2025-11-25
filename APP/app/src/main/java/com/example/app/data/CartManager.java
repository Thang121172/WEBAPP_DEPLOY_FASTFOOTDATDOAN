package com.example.app.data;

import com.example.app.model.CartItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Singleton Quản lý Giỏ hàng (Cart Manager).
 * Lớp này chịu trách nhiệm giao tiếp với nguồn dữ liệu (PostgreSQL/PGAdmin).
 * Đã loại bỏ hoàn toàn In-Memory List và Mock Data, chuyển sang mô hình kết nối DB.
 */
public class CartManager {

    private static CartManager instance;

    // Phí ship cố định (BUSINESS LOGIC CONSTANT)
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("25000");

    // Sử dụng interface để lắng nghe sự thay đổi của giỏ hàng (Vẫn giữ cho UI)
    public interface CartUpdateListener {
        void onCartUpdated();
    }
    private CartUpdateListener listener;

    private CartManager() {
        // TODO: Khởi tạo các kết nối CSDL tại đây nếu cần (ví dụ: Connection Pool, JDBC setup)
    }

    /**
     * Phương thức công khai để lấy thể hiện duy nhất của CartManager (Singleton).
     */
    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    // --- Cấu hình Listener ---
    public void setListener(CartUpdateListener listener) {
        this.listener = listener;
    }

    private void notifyCartUpdate() {
        if (listener != null) {
            listener.onCartUpdated();
        }
    }

    // --- Logic Quản lý Giỏ hàng (PGAdmin/PostgreSQL Placeholder) ---

    /**
     * Lấy danh sách các mặt hàng trong giỏ (Chỉ đọc).
     * @return Danh sách CartItem, được lấy từ PostgreSQL.
     */
    public List<CartItem> getItems() {
        // TODO: PGAdmin/PostgreSQL -
        // Triển khai logic gọi CSDL (Bất đồng bộ)
        // để FETCH dữ liệu từ bảng giỏ hàng (Ví dụ: SELECT * FROM cart_items WHERE user_id = ?).

        // Tạm thời trả về danh sách rỗng để tránh lỗi biên dịch/crash.
        return Collections.emptyList();
    }

    /**
     * Thêm một CartItem mới hoặc cập nhật số lượng của mục đã tồn tại.
     * @param newItem Món hàng cần thêm.
     */
    public void addItem(CartItem newItem) {
        // TODO: PGAdmin/PostgreSQL -
        // 1. Truy vấn CSDL để kiểm tra sự tồn tại.
        // 2. Nếu tồn tại: Thực hiện UPDATE quantity.
        // 3. Nếu chưa tồn tại: Thực hiện INSERT newItem.

        // Giả sử logic DB thành công
        notifyCartUpdate();
    }

    /**
     * Xóa một mặt hàng khỏi giỏ hàng bằng ID duy nhất của nó (cartItemId).
     */
    public void removeItem(String cartItemId) {
        // TODO: PGAdmin/PostgreSQL - Thực hiện DELETE FROM cart_table WHERE cartItemId = ?

        // Giả sử logic DB thành công
        notifyCartUpdate();
    }

    /**
     * Cập nhật số lượng của một mặt hàng trong giỏ.
     * Nếu quantity <= 0, món hàng sẽ bị xóa.
     */
    public void updateQuantity(String cartItemId, int quantity) {
        if (quantity <= 0) {
            removeItem(cartItemId);
            return;
        }

        // TODO: PGAdmin/PostgreSQL - Thực hiện UPDATE quantity = ? WHERE cartItemId = ?

        // Giả sử logic DB thành công
        notifyCartUpdate();
    }

    /**
     * Xóa sạch giỏ hàng.
     */
    public void clearCart() {
        // TODO: PGAdmin/PostgreSQL - Thực hiện DELETE FROM cart_table

        // Giả sử logic DB thành công
        notifyCartUpdate();
    }

    /**
     * Kiểm tra xem giỏ hàng có rỗng không.
     */
    public boolean isEmpty() {
        // TODO: PGAdmin/PostgreSQL - Thực hiện truy vấn COUNT(*) hoặc dùng getItems()
        return getItems().isEmpty();
    }


    // --- Logic Tính toán ---

    /**
     * Tính tổng phụ (Subtotal) của tất cả mặt hàng.
     */
    public BigDecimal calculateSubtotal() {
        // Lấy dữ liệu mới nhất từ DB
        List<CartItem> currentItems = getItems();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : currentItems) {
            // ✅ ĐÃ SỬA: Chuyển đổi giá trị double từ getLineTotal() sang BigDecimal
            // để giải quyết lỗi biên dịch.
            subtotal = subtotal.add(BigDecimal.valueOf(item.getLineTotal()));
        }

        // Làm tròn về 0 chữ số thập phân
        return subtotal.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Lấy phí vận chuyển.
     */
    public BigDecimal getShippingFee() {
        return SHIPPING_FEE;
    }

    /**
     * Tính tổng thanh toán (Subtotal + Shipping Fee).
     */
    public BigDecimal calculateTotal() {
        return calculateSubtotal().add(getShippingFee()).setScale(0, RoundingMode.HALF_UP);
    }
}
