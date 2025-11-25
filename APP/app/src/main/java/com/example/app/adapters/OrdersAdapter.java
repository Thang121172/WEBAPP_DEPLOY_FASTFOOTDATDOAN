package com.example.app.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat; // Thư viện quan trọng để fix lỗi tinting
import androidx.recyclerview.widget.RecyclerView;
import com.example.app.R;
import com.example.app.model.OrderSummary;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderSummary order);
    }

    private final List<OrderSummary> orders;
    private final OnOrderClickListener listener;

    public OrdersAdapter(List<OrderSummary> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (orders == null || position < 0 || position >= orders.size())
            return RecyclerView.NO_ID;
        OrderSummary o = orders.get(position);
        if (o == null || TextUtils.isEmpty(o.orderId))
            return position;
        try {
            return Long.parseLong(o.orderId.replaceAll("\\D+", ""));
        } catch (Exception ignore) {
            return o.orderId.hashCode();
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // *** ĐÃ CẬP NHẬT: Sử dụng item_order_summary.xml ***
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_summary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        OrderSummary o = orders.get(position);
        Context ctx = holder.itemView.getContext();

        // Mã đơn (#code)
        String codeText = (!TextUtils.isEmpty(o.orderId)) ? "#" + o.orderId : "#---";
        holder.tvOrderId.setText(codeText);

        // Trạng thái: icon + màu + text
        String rawStatus = o.status != null ? o.status : "-";
        applyStatus(ctx, holder, rawStatus);

        // Tổng tiền VNĐ
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        holder.tvTotal.setText(nf.format(o.total) + " đ");

        // Thời gian
        holder.tvTime.setText(o.createdAt != null ? o.createdAt : "");

        // Click item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onOrderClick(o);
        });
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    // ===== Helpers: map trạng thái sang icon/màu/text và áp dụng tinting ổn định =====
    private void applyStatus(Context ctx, VH holder, String raw) {
        String status = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);

        int colorId;
        int iconRes;
        String display;

        // Định nghĩa các resource color và drawable (Giả định các icon này đã tồn tại)
        switch (status) {
            case "delivered":
            case "completed":
            case "done":
            case "success":
            case "hoàn tất":
                iconRes = R.drawable.ic_check_circle;
                colorId = R.color.ff_status_success;
                display = "Đã giao";
                break;

            case "shipping":
            case "in_progress":
            case "preparing":
            case "on_the_way":
            case "processing":
            case "đang giao":
                iconRes = R.drawable.ic_time;
                colorId = R.color.ff_status_warning;
                display = "Đang giao";
                break;

            case "cancelled":
            case "canceled":
            case "rejected":
            case "failed":
            case "đã hủy":
                iconRes = R.drawable.ic_cancel;
                colorId = R.color.ff_status_error;
                display = "Đã hủy";
                break;

            default:
                iconRes = R.drawable.ic_info;
                colorId = R.color.ff_text_secondary;
                display = raw;
                break;
        }

        int color = ContextCompat.getColor(ctx, colorId);

        // 1. Lấy Drawable và Wrap để áp dụng màu tint ổn định trên mọi API
        Drawable drawable = ContextCompat.getDrawable(ctx, iconRes);
        if (drawable != null) {
            // Đảm bảo drawable có thể được tint (dùng .mutate() để không ảnh hưởng đến các drawable khác)
            Drawable wrappedDrawable = DrawableCompat.wrap(drawable).mutate();
            DrawableCompat.setTint(wrappedDrawable, color);

            // 2. Thiết lập drawableStart và bỏ các drawable khác (null, null, null)
            holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(wrappedDrawable, null, null, null);
        } else {
            // Nếu không tìm thấy drawable, đặt về null
            holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }

        // 3. Thiết lập text và màu chữ
        holder.tvStatus.setTextColor(color);
        holder.tvStatus.setText(display);
    }

    // ===== ViewHolder =====
    static class VH extends RecyclerView.ViewHolder {
        // Cập nhật tên biến để khớp với item_order_summary.xml
        TextView tvOrderId, tvStatus, tvTotal, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ ID từ item_order_summary.xml
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            tvTime = itemView.findViewById(R.id.tv_order_time);
        }
    }
}
