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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MerchantOrdersAdapter
 * - Hiển thị danh sách đơn ở Merchant Dashboard.
 * - Nguồn dữ liệu: List<Map<String,Object>> (key gợi ý: order_id/code, status,
 * total, created_at, items)
 * - YÊU CẦU item_merchant_order.xml có:
 * tv_order_code, tv_items_brief, tv_total, tv_time, tv_status,
 * iv_status, layout_status_chip, btn_accept, btn_reject, btn_ready
 */
public class MerchantOrdersAdapter extends RecyclerView.Adapter<MerchantOrdersAdapter.VH> {

    public interface Listener {
        void onAccept(@NonNull Map<String, Object> order, int position);

        void onReject(@NonNull Map<String, Object> order, int position);

        void onReady(@NonNull Map<String, Object> order, int position);

        default void onComplete(@NonNull Map<String, Object> order, int position) {
        }

        default void onItemClick(@NonNull Map<String, Object> order, int position) {
        }
    }

    private final Context ctx;
    private final List<Map<String, Object>> data;
    private final Listener listener;

    public MerchantOrdersAdapter(@NonNull Context ctx,
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
                .inflate(R.layout.item_merchant_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> m = data.get(position);

        // Code
        Object code = firstNonNull(m.get("order_id"), m.get("code"), "—");
        h.tvCode.setText("#" + code);

        // Items brief
        h.tvItemsBrief.setText(buildItemsBrief(m.get("items")));

        // Total
        h.tvTotal.setText(formatVnd(firstNonNull(m.get("total"), "0")));

        // Time
        h.tvTime.setText(asString(firstNonNull(m.get("created_at"), m.get("time"), "")));

        // Status chip
        String rawStatus = asString(firstNonNull(m.get("status"), "pending")).trim().toLowerCase(Locale.ROOT);
        applyStatusChip(h, rawStatus);

        // Actions
        h.btnAccept.setVisibility(View.GONE);
        h.btnReject.setVisibility(View.GONE);
        h.btnReady.setVisibility(View.GONE);
        h.btnComplete.setVisibility(View.GONE);

        switch (rawStatus) {
            case "pending":
            case "new":
            case "awaiting_confirm":
            case "confirmed": // ✅ FIX: Backend trả về CONFIRMED (chờ xác nhận)
                h.btnAccept.setVisibility(View.VISIBLE);
                h.btnReject.setVisibility(View.VISIBLE);
                break;
            case "preparing":
            case "in_progress":
            case "processing":
            case "confirm":
            case "cooking": // ✅ FIX: Backend trả về COOKING (đang chuẩn bị, cần nút "Làm xong")
                h.btnReady.setVisibility(View.VISIBLE);
                h.btnReject.setVisibility(View.VISIBLE);
                break;
            case "ready":
                // ✅ FIX: Đơn READY hiển thị nút "Hoàn tất" để merchant có thể tự đánh dấu đơn là đã hoàn tất
                // (tự giao hàng hoặc không có shipper)
                h.btnComplete.setVisibility(View.VISIBLE);
                break;
            default:
                // delivered/cancelled/... -> ẩn hết
                break;
        }

        // Clicks (guard NO_POSITION)
        h.itemView.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onItemClick(m, p);
        });

        h.btnAccept.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onAccept(m, p);
        });

        h.btnReject.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onReject(m, p);
        });

        h.btnReady.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onReady(m, p);
        });

        h.btnComplete.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onComplete(m, p);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // ===== Helpers =====

    private void applyStatusChip(@NonNull VH h, @NonNull String status) {
        int colorText;
        int iconRes;
        int bgColor; // nhạt (alpha)

        switch (status) {
            case "delivered":
            case "completed":
            case "done":
            case "success":
                iconRes = R.drawable.ic_status_delivered;
                colorText = ContextCompat.getColor(ctx, R.color.ff_success);
                bgColor = withAlpha(colorText, 0.10f);
                h.tvStatus.setText("đã giao");
                break;

            case "preparing":
            case "in_progress":
            case "processing":
            case "shipping":
            case "awaiting_ship":
            case "cooking": // ✅ FIX: Backend trả về COOKING (đang chuẩn bị)
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(ctx, R.color.ff_warning);
                bgColor = withAlpha(colorText, 0.12f);
                h.tvStatus.setText("đang chuẩn bị");
                break;

            case "ready":
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(ctx, R.color.ff_info);
                bgColor = withAlpha(colorText, 0.12f);
                h.tvStatus.setText("sẵn sàng");
                break;

            case "cancelled":
            case "canceled":
            case "rejected":
            case "failed":
                iconRes = R.drawable.ic_status_cancelled;
                colorText = ContextCompat.getColor(ctx, R.color.ff_error);
                bgColor = withAlpha(colorText, 0.10f);
                h.tvStatus.setText("đã hủy");
                break;

            case "pending":
            case "new":
            case "awaiting_confirm":
            case "confirmed": // ✅ FIX: Backend trả về CONFIRMED (chờ xác nhận)
            default:
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(ctx, R.color.ff_text_secondary);
                bgColor = withAlpha(colorText, 0.08f);
                h.tvStatus.setText("chờ xác nhận");
                break;
        }

        h.ivStatus.setImageResource(iconRes);
        h.ivStatus.setColorFilter(colorText);
        h.tvStatus.setTextColor(colorText);

        // đổi màu nền chip (bg của layout_status_chip phải là shape bo góc)
        if (h.layoutStatusChip.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) h.layoutStatusChip
                    .getBackground().mutate();
            gd.setColor(bgColor);
        }
    }

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
            // Nếu backend đã trả kèm đơn vị
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
                break; // rút gọn 3 món
        }
        if (items.size() > shown)
            sb.append("…");
        return sb.toString();
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * alpha);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvItemsBrief, tvTotal, tvTime, tvStatus;
        ImageView ivStatus;
        View layoutStatusChip;
        View btnAccept, btnReject, btnReady, btnComplete;

        VH(@NonNull View v) {
            super(v);
            tvCode = v.findViewById(R.id.tv_order_code);
            tvItemsBrief = v.findViewById(R.id.tv_items_brief);
            tvTotal = v.findViewById(R.id.tv_total);
            tvTime = v.findViewById(R.id.tv_time);
            tvStatus = v.findViewById(R.id.tv_status);
            ivStatus = v.findViewById(R.id.iv_status);
            layoutStatusChip = v.findViewById(R.id.layout_status_chip);
            btnAccept = v.findViewById(R.id.btn_accept);
            btnReject = v.findViewById(R.id.btn_reject);
            btnReady = v.findViewById(R.id.btn_ready);
            btnComplete = v.findViewById(R.id.btn_complete);
        }
    }
}
