package com.example.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CustomerOrdersAdapter
 *
 * YÊU CẦU LAYOUT: item_customer_order.xml với các id:
 * - tv_order_code, tv_items_brief, tv_total, tv_time
 * - layout_status_chip, iv_status, tv_status
 * - btn_cancel (nút hủy)
 *
 * Trạng thái hỗ trợ: pending/new/awaiting_confirm,
 * in_progress/processing/shipping,
 * completed/delivered, cancelled/failed.
 */
public class CustomerOrdersAdapter extends RecyclerView.Adapter<CustomerOrdersAdapter.VH> {

    public interface Listener {
        /** Hủy đơn; reason có thể null nếu không hỏi lý do. */
        void onCancel(@NonNull Map<String, Object> order, int position, @Nullable String reason);

        default void onItemClick(@NonNull Map<String, Object> order, int position) {
        }
    }

    private final Context ctx;
    private final List<Map<String, Object>> data;
    private final Listener listener;

    public CustomerOrdersAdapter(
            @NonNull Context ctx,
            @NonNull List<Map<String, Object>> data,
            @NonNull Listener listener) {
        this.ctx = ctx;
        this.data = data;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        Object code = firstNonNull(
                data.get(position).get("order_id"),
                data.get(position).get("code"),
                String.valueOf(position));
        try {
            return Long.parseLong(code.toString().replaceAll("\\D+", ""));
        } catch (Exception ignore) {
            return code.toString().hashCode();
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> m = data.get(position);

        // Code hiển thị
        String code = "#" + asString(firstNonNull(m.get("order_id"), m.get("code"), position + 1));
        h.tvCode.setText(code);

        // Tóm tắt món / tổng tiền / thời gian
        h.tvItemsBrief.setText(buildItemsBrief(m.get("items")));
        h.tvTotal.setText(formatVnd(firstNonNull(m.get("total"), "0")));
        h.tvTime.setText(asString(firstNonNull(m.get("created_at"), m.get("time"), "")));

        // Trạng thái
        String status = asString(m.get("status")).trim().toLowerCase(Locale.ROOT);
        if (TextUtils.isEmpty(status))
            status = "pending";
        applyStatusChip(h, status);

        // Reset action visibility
        h.btnCancel.setVisibility(View.GONE);

        // ✅ FIX: Chỉ cho phép hủy ở PENDING hoặc CONFIRMED (chờ xử lý)
        // Không cho hủy khi đã bắt đầu chuẩn bị hoặc đang vận chuyển
        if (isPending(status) || status.equalsIgnoreCase("confirmed")) {
            h.btnCancel.setVisibility(View.VISIBLE);
        }

        // Clicks
        h.itemView.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onItemClick(m, p);
        });

        h.btnCancel.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            // Nếu cần thu thập "reason", bạn có thể mở dialog ở Fragment.
            listener.onCancel(m, p, null);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // ================= Helpers (UI) =================

    private void applyStatusChip(@NonNull VH h, @NonNull String status) {
        int colorText;
        int iconRes;
        int bgColor;

        if (isCompleted(status)) {
            iconRes = R.drawable.ic_status_delivered;
            colorText = ContextCompat.getColor(ctx, R.color.ff_success);
            bgColor = withAlpha(colorText, 0.10f);
            h.tvStatus.setText("hoàn tất");
        } else if (status.equalsIgnoreCase("ready") || status.equalsIgnoreCase("handover")) {
            // ✅ FIX: READY status hiển thị màu xanh (success)
            iconRes = R.drawable.ic_status_delivered;
            colorText = ContextCompat.getColor(ctx, R.color.ff_success);
            bgColor = withAlpha(colorText, 0.10f);
            h.tvStatus.setText("sẵn sàng");
        } else if (isInProgress(status) || status.equalsIgnoreCase("cooking") || status.equalsIgnoreCase("confirmed")) {
            iconRes = R.drawable.ic_status_inprogress;
            colorText = ContextCompat.getColor(ctx, R.color.ff_warning);
            bgColor = withAlpha(colorText, 0.12f);
            h.tvStatus.setText("đang xử lý");
        } else if (isCancelled(status)) {
            iconRes = R.drawable.ic_status_cancelled;
            colorText = ContextCompat.getColor(ctx, R.color.ff_error);
            bgColor = withAlpha(colorText, 0.10f);
            h.tvStatus.setText("đã hủy");
        } else { // pending/new/awaiting_confirm
            iconRes = R.drawable.ic_status_inprogress;
            colorText = ContextCompat.getColor(ctx, R.color.ff_text_secondary);
            bgColor = withAlpha(colorText, 0.08f);
            h.tvStatus.setText("chờ xử lý");
        }

        h.ivStatus.setImageResource(iconRes);
        h.ivStatus.setColorFilter(colorText);
        h.tvStatus.setTextColor(colorText);

        if (h.layoutStatusChip.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) h.layoutStatusChip
                    .getBackground().mutate();
            gd.setColor(bgColor);
        }
    }

    private static boolean isPending(String s) {
        switch (s) {
            case "pending":
            case "new":
            case "awaiting_confirm":
                return true;
        }
        return false;
    }

    private static boolean isInProgress(String s) {
        switch (s) {
            case "in_progress":
            case "processing":
            case "shipping":
            case "cooking":
            case "confirmed":
            case "delivering":
                return true;
        }
        return false;
    }

    private static boolean isCompleted(String s) {
        switch (s) {
            case "completed":
            case "delivered":
            case "success":
                return true;
        }
        return false;
    }

    private static boolean isCancelled(String s) {
        switch (s) {
            case "cancelled":
            case "canceled":
            case "failed":
                return true;
        }
        return false;
    }

    // ================= Helpers (data) =================

    private static Object firstNonNull(Object... arr) {
        if (arr == null)
            return null;
        for (Object o : arr)
            if (o != null)
                return o;
        return null;
    }

    private static String asString(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String formatVnd(Object v) {
        if (v == null)
            return "0 đ";
        try {
            String s = v.toString();
            if (s.endsWith("đ") || s.endsWith("₫") || s.toLowerCase(Locale.ROOT).contains("vnd")) {
                return s;
            }
            double d = Double.parseDouble(s.replaceAll("[^0-9.]", ""));
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            return nf.format(d) + " đ";
        } catch (Exception ignore) {
            return String.valueOf(v);
        }
    }

    @SuppressWarnings("unchecked")
    private static String buildItemsBrief(Object itemsObj) {
        if (!(itemsObj instanceof List))
            return "";
        List<?> items = (List<?>) itemsObj;
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Object it : items) {
            if (!(it instanceof Map))
                continue;
            Map<String, Object> row = (Map<String, Object>) it;
            Object name = firstNonNull(row.get("name"), row.get("title"), "Món");
            Object q = firstNonNull(row.get("qty"), 1);
            if (shown > 0)
                sb.append(", ");
            sb.append(q).append("x ").append(name);
            shown++;
            if (shown >= 3)
                break;
        }
        if (items.size() > shown)
            sb.append("…");
        return sb.toString();
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * alpha);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ================= ViewHolder =================

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvItemsBrief, tvTotal, tvTime, tvStatus;
        ImageView ivStatus;
        View layoutStatusChip;
        View btnCancel;

        VH(@NonNull View v) {
            super(v);
            tvCode = v.findViewById(R.id.tv_order_code);
            tvItemsBrief = v.findViewById(R.id.tv_items_brief);
            tvTotal = v.findViewById(R.id.tv_total);
            tvTime = v.findViewById(R.id.tv_time);
            tvStatus = v.findViewById(R.id.tv_status);
            ivStatus = v.findViewById(R.id.iv_status);
            layoutStatusChip = v.findViewById(R.id.layout_status_chip);
            btnCancel = v.findViewById(R.id.btn_cancel);
        }
    }
}
