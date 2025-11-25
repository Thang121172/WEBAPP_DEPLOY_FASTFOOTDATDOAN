package com.example.app.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.VH> {
    private List<Map<String, Object>> items;

    public OrderItemAdapter(@NonNull List<Map<String, Object>> items) {
        this.items = items;
        setHasStableIds(true);
    }

    /** Cho phép cập nhật danh sách nếu cần */
    public void submitList(@NonNull List<Map<String, Object>> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (items == null || position < 0 || position >= items.size())
            return RecyclerView.NO_ID;

        Object id = firstNonNull(items.get(position).get("id"), items.get(position).get("_id"));
        if (id == null)
            return items.get(position).hashCode();

        try {
            return Long.parseLong(id.toString().replaceAll("\\D+", ""));
        } catch (Exception ignore) {
            return id.toString().hashCode();
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, Object> it = items.get(position);

        // --- Tên món linh hoạt ---
        // ✅ FIX: Ưu tiên product_name từ backend
        String title = firstNonEmpty(
                asString(it.get("product_name")),  // ✅ Thêm product_name vào đầu
                asString(it.get("name")),
                asString(it.get("title")),
                asString(it.get("item_name")),
                "-");
        holder.title.setText(title);

        // --- Số lượng ---
        int qty = parseQty(firstNonNull(it.get("qty"), it.get("quantity")));
        holder.qty.setText("x" + qty);

        // --- Giá: ưu tiên line_total nếu backend trả; nếu không tính unit * qty ---
        Object lineTotalRaw = firstNonNull(it.get("line_total"), it.get("total"));
        String priceText;
        if (lineTotalRaw != null) {
            priceText = formatCurrency(lineTotalRaw);
        } else {
            BigDecimal unit = parseMoney(firstNonNull(it.get("price"), it.get("unit_price"), it.get("amount")));
            BigDecimal total = unit.multiply(BigDecimal.valueOf(qty)).setScale(0, RoundingMode.HALF_UP);
            priceText = formatCurrency(total);
        }
        holder.price.setText(priceText);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    // ===== Helpers =====

    private static Object firstNonNull(Object... arr) {
        if (arr == null)
            return null;
        for (Object o : arr)
            if (o != null)
                return o;
        return null;
    }

    private static String asString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static String firstNonEmpty(String... arr) {
        if (arr == null)
            return null;
        for (String s : arr) {
            if (!TextUtils.isEmpty(s))
                return s;
        }
        return null;
    }

    private static int parseQty(Object q) {
        if (q == null)
            return 1;
        try {
            String s = q.toString().trim();
            if (TextUtils.isEmpty(s))
                return 1;
            return Math.max(1, Integer.parseInt(s));
        } catch (Exception ignore) {
            return 1;
        }
    }

    private static BigDecimal parseMoney(Object v) {
        if (v == null)
            return BigDecimal.ZERO;
        try {
            // Lọc mọi ký tự không phải số hoặc dấu chấm
            String s = v.toString().replaceAll("[^0-9.]", "");
            if (TextUtils.isEmpty(s))
                return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception ignore) {
            return BigDecimal.ZERO;
        }
    }

    /** Nhận cả BigDecimal / Number / String (kể cả đã có "đ", "₫", "VND"). */
    private static String formatCurrency(Object v) {
        try {
            if (v instanceof BigDecimal) {
                NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                return nf.format(((BigDecimal) v).setScale(0, RoundingMode.HALF_UP)) + " đ";
            }
            String s = v.toString();
            if (s.endsWith("đ") || s.endsWith("₫") || s.toLowerCase(Locale.ROOT).contains("vnd")) {
                return s; // đã có đơn vị ⇒ giữ nguyên
            }
            double d = Double.parseDouble(s.replaceAll("[^0-9.]", ""));
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            return nf.format(Math.round(d)) + " đ";
        } catch (Exception ignore) {
            return String.valueOf(v);
        }
    }

    // ===== ViewHolder =====
    static class VH extends RecyclerView.ViewHolder {
        TextView title, qty, price;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_item_title);
            qty = v.findViewById(R.id.tv_item_qty);
            price = v.findViewById(R.id.tv_item_price);
        }
    }
}
