package com.example.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;
import com.example.app.model.CartItem;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * CartAdapter: Hiển thị và quản lý các item trong giỏ hàng.
 * Đã sửa:
 * 1. Đổi tên phương thức notifyDataSetChanged() bị lỗi (do là final) thành handleDataChangeAndNotifyTotals().
 * 2. Đảm bảo chuyển đổi price (double) sang BigDecimal để tính toán.
 * 3. FIX LỖI: Cập nhật các ID trong CartViewHolder để khớp với item_cart_item.xml.
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private final Context context;
    private final int itemLayoutId;
    private final NumberFormat currencyFormatter;

    // Listeners fields
    private OnTotalsListener totalsListener;
    private OnBrandTotalsListener brandTotalsListener;
    private OnQuantityChangeListener quantityChangeListener;

    // --- INTERFACES CẦN THIẾT ---
    public interface OnTotalsListener {
        void onTotalsCalculated(BigDecimal subtotal);
    }
    public interface OnBrandTotalsListener {
        void onBrandTotalsUpdated(Map<String, BigDecimal> brandSubtotals);
    }
    public interface OnQuantityChangeListener {
        void onQuantityChange(CartItem item, int newQuantity);
    }

    // --- CONSTRUCTOR & SETTERS ---
    public CartAdapter(Context context, List<CartItem> cartItems, int itemLayoutId) {
        this.context = context;
        this.cartItems = cartItems;
        this.itemLayoutId = itemLayoutId;
        this.currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public void setTotalsListener(OnTotalsListener listener) {
        this.totalsListener = listener;
    }

    public void setBrandTotalsListener(OnBrandTotalsListener listener) {
        this.brandTotalsListener = listener;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.quantityChangeListener = listener;
    }

    /**
     * Cập nhật danh sách items trong giỏ hàng
     */
    public void updateData(List<CartItem> newItems) {
        this.cartItems = newItems != null ? newItems : new ArrayList<>();
        handleDataChangeAndNotifyTotals();
    }

    // --- IMPLEMENTATION ---

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(this.itemLayoutId, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        final CartItem item = cartItems.get(position);

        // Sử dụng ID đúng từ layout XML
        holder.tvItemName.setText(item.getName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        // Chuyển đổi double sang BigDecimal để tính toán chính xác
        BigDecimal price = convertToBigDecimal(item.getPrice());
        int quantity = item.getQuantity();
        BigDecimal subTotal = price.multiply(new BigDecimal(quantity));

        holder.tvSubtotal.setText(String.format("%s đ", formatCurrency(subTotal)));

        // Xử lý sự kiện tăng/giảm số lượng
        holder.btnIncrease.setOnClickListener(v -> {
            if (quantityChangeListener != null) {
                quantityChangeListener.onQuantityChange(item, quantity + 1);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (quantityChangeListener != null && quantity > 0) {
                quantityChangeListener.onQuantityChange(item, quantity - 1);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    /**
     * Phương thức thay thế cho notifyDataSetChanged() bị lỗi.
     * Phương thức này thực hiện: 1. Thông báo cập nhật View, 2. Tính toán lại tổng tiền.
     */
    public void handleDataChangeAndNotifyTotals() {
        super.notifyDataSetChanged();

        if (getItemCount() > 0) {
            calculateAndNotifyTotals();
        } else {
            if (totalsListener != null) {
                totalsListener.onTotalsCalculated(BigDecimal.ZERO);
            }
            if (brandTotalsListener != null) {
                brandTotalsListener.onBrandTotalsUpdated(new HashMap<>());
            }
        }
    }

    private void calculateAndNotifyTotals() {
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        Map<String, BigDecimal> brandSubtotals = new HashMap<>();

        for (CartItem item : cartItems) {
            BigDecimal price = convertToBigDecimal(item.getPrice());
            BigDecimal itemTotal = price.multiply(new BigDecimal(item.getQuantity()));
            totalSubtotal = totalSubtotal.add(itemTotal);

            // Giả sử getBrandName() đã tồn tại trong CartItem
            String brandName = item.getBrandName();
            brandSubtotals.put(
                    brandName,
                    brandSubtotals.getOrDefault(brandName, BigDecimal.ZERO).add(itemTotal)
            );
        }

        if (totalsListener != null) {
            totalsListener.onTotalsCalculated(totalSubtotal);
        }

        if (brandTotalsListener != null) {
            brandSubtotals.clear(); // Xóa nếu không cần
            brandTotalsListener.onBrandTotalsUpdated(brandSubtotals);
        }
    }

    /**
     * Chuyển double sang BigDecimal để thực hiện tính toán chính xác.
     */
    private BigDecimal convertToBigDecimal(double price) {
        return BigDecimal.valueOf(price);
    }

    private String formatCurrency(BigDecimal amount) {
        return currencyFormatter.format(amount.longValue());
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        // Thay đổi các biến để khớp với ID trong layout
        final TextView tvItemName;
        final TextView tvQuantity;
        final TextView tvSubtotal;
        final MaterialButton btnDecrease; // Thay ImageButton thành MaterialButton
        final MaterialButton btnIncrease; // Thay ImageButton thành MaterialButton

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            // Sửa lại ánh xạ đúng với ID trong layout item_cart_item.xml
            tvItemName = itemView.findViewById(R.id.tv_item_title);
            tvQuantity = itemView.findViewById(R.id.tv_qty);
            tvSubtotal = itemView.findViewById(R.id.tv_item_price);
            btnDecrease = itemView.findViewById(R.id.btn_minus); // Sửa lại đúng với MaterialButton
            btnIncrease = itemView.findViewById(R.id.btn_plus); // Sửa lại đúng với MaterialButton
        }
    }
}
