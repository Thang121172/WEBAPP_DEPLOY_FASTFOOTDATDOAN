package com.example.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
 * ShipperOrdersAdapter
 * Hiển thị đơn hàng cho vai trò SHIPPER với các hành động theo trạng thái:
 * available / assigned|arrived_store / picked_up|on_the_way|delivering /
 * delivered|completed / failed|cancelled.
 *
 * YÊU CẦU LAYOUT: item_shipper_order.xml với các id:
 * - tv_order_code, tv_items_brief, tv_total, tv_time
 * - layout_status_chip, iv_status, tv_status
 * - btn_accept, btn_arrived, btn_picked, btn_on_the_way, btn_delivered,
 * btn_failed
 */
public class ShipperOrdersAdapter extends RecyclerView.Adapter<ShipperOrdersAdapter.VH> {

    public interface Listener {
        void onAccept(@NonNull Map<String, Object> order, int position);

        void onArrived(@NonNull Map<String, Object> order, int position);

        void onPicked(@NonNull Map<String, Object> order, int position);

        void onOnTheWay(@NonNull Map<String, Object> order, int position);

        void onDelivered(@NonNull Map<String, Object> order, int position);

        void onFailed(@NonNull Map<String, Object> order, int position, @Nullable String reason);

        default void onItemClick(@NonNull Map<String, Object> order, int position) {
        }
    }

    private final Context ctx;
    private final List<Map<String, Object>> data;
    private final Listener listener;
    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public ShipperOrdersAdapter(
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
            String digits = code.toString().replaceAll("\\D+", "");
            if (!TextUtils.isEmpty(digits))
                return Long.parseLong(digits);
        } catch (Exception ignore) {
        }
        return code.toString().hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shipper_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Map<String, Object> m = data.get(pos);

        // Code
        String code = "#" + asString(firstNonNull(m.get("order_id"), m.get("code"), pos + 1));
        h.tvCode.setText(code);

        // Items brief / total / time
        h.tvItemsBrief.setText(buildItemsBrief(m.get("items")));
        h.tvTotal.setText(formatVnd(firstNonNull(m.get("total"), "0")));
        h.tvTime.setText(asString(firstNonNull(m.get("created_at"), m.get("time"), "")));

        // Status normalize
        String status = asString(m.get("status")).trim().toLowerCase(Locale.ROOT);
        if (TextUtils.isEmpty(status))
            status = "available";
        
        // ✅ FIX: Nếu order đã có shipper_id (đã được assign) nhưng status vẫn là READY/CONFIRMED/COOKING,
        // thì coi như đang giao
        Object shipperId = m.get("shipper_id");
        boolean hasShipper = shipperId != null;
        if (hasShipper && (status.equals("ready") || status.equals("confirmed") || status.equals("cooking"))) {
            status = "shipping"; // Coi như đang giao
        }

        applyStatusChip(h, status);

        // Reset all buttons
        h.btnAccept.setVisibility(View.GONE);
        h.btnArrived.setVisibility(View.GONE);
        h.btnPicked.setVisibility(View.GONE);
        h.btnOnTheWay.setVisibility(View.GONE);
        h.btnDelivered.setVisibility(View.GONE);
        h.btnFailed.setVisibility(View.GONE);

        // Show buttons by status
        // ✅ FIX: Nếu order đã có shipper_id, không hiển thị nút "Nhận đơn"
        
        switch (status) {
            case "available":
            case "pending": // ✅ FIX: Orders trong tab "Mới" có thể có status PENDING
            case "ready": // ✅ FIX: Orders trong tab "Mới" có thể có status READY (chưa có shipper_id)
            case "confirmed": // ✅ FIX: Orders trong tab "Mới" có thể có status CONFIRMED (chưa có shipper_id)
            case "cooking": // ✅ FIX: Orders trong tab "Mới" có thể có status COOKING (chưa có shipper_id)
                if (!hasShipper) {
                    h.btnAccept.setVisibility(View.VISIBLE);
                }
                break;

            case "assigned":
            case "arrived_store":
                h.btnArrived.setVisibility(View.VISIBLE); // xác nhận đã tới quán
                h.btnPicked.setVisibility(View.VISIBLE); // đã lấy hàng
                // nếu muốn cho phép lên "on_the_way" trực tiếp:
                // h.btnOnTheWay.setVisibility(View.VISIBLE);
                h.btnFailed.setVisibility(View.VISIBLE);
                break;

            case "picked_up":
            case "on_the_way":
            case "delivering":
            case "shipping": // ✅ FIX: Backend trả về SHIPPING khi shipper accept order (hoặc đã được normalize từ ready/confirmed/cooking nếu có shipper_id)
                if (hasShipper) {
                    h.btnOnTheWay.setVisibility(View.VISIBLE);
                    h.btnDelivered.setVisibility(View.VISIBLE);
                    h.btnFailed.setVisibility(View.VISIBLE);
                }
                break;

            case "delivered":
            case "completed":
            case "failed":
            case "cancelled":
            case "canceled":
                // ẩn hết
                break;

            default:
                // ẩn an toàn
                break;
        }

        // Clicks (bảo vệ NO_POSITION)
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

        h.btnArrived.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onArrived(m, p);
        });

        h.btnPicked.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onPicked(m, p);
        });

        h.btnOnTheWay.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onOnTheWay(m, p);
        });

        h.btnDelivered.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            listener.onDelivered(m, p);
        });

        h.btnFailed.setOnClickListener(v -> {
            if (listener == null)
                return;
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            // reason có thể null nếu bạn mở dialog lấy lý do sau
            listener.onFailed(m, p, null);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // =============== Helpers ===============

    private void applyStatusChip(@NonNull VH h, @NonNull String status) {
        int colorText;
        int iconRes;
        int bgColor;

        switch (status) {
            case "delivered":
            case "completed":
                iconRes = R.drawable.ic_status_delivered;
                colorText = ContextCompat.getColor(ctx, R.color.ff_success);
                bgColor = withAlpha(colorText, 0.10f);
                h.tvStatus.setText("đã giao");
                break;

            case "picked_up":
            case "on_the_way":
            case "delivering":
            case "shipping": // ✅ FIX: Backend trả về SHIPPING khi shipper accept order
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(ctx, R.color.ff_warning);
                bgColor = withAlpha(colorText, 0.12f);
                h.tvStatus.setText("đang giao");
                break;

            case "assigned":
            case "arrived_store":
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(ctx, R.color.ff_info);
                bgColor = withAlpha(colorText, 0.12f);
                h.tvStatus.setText("chờ lấy");
                break;

            case "failed":
            case "cancelled":
            case "canceled":
                iconRes = R.drawable.ic_status_cancelled;
                colorText = ContextCompat.getColor(ctx, R.color.ff_error);
                bgColor = withAlpha(colorText, 0.10f);
                h.tvStatus.setText("thất bại");
                break;

            case "available":
            default:
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(ctx, R.color.ff_text_secondary);
                bgColor = withAlpha(colorText, 0.08f);
                h.tvStatus.setText("chưa nhận");
                break;
        }

        h.ivStatus.setImageResource(iconRes);
        h.ivStatus.setColorFilter(colorText);
        h.tvStatus.setTextColor(colorText);

        Drawable bg = h.layoutStatusChip.getBackground();
        if (bg instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable) bg.mutate();
            gd.setColor(bgColor);
        } else {
            // fallback: set trực tiếp màu nền nếu không phải shape
            h.layoutStatusChip.setBackgroundColor(bgColor);
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

    private String formatVnd(Object v) {
        try {
            double d = Double.parseDouble(v.toString().replaceAll("[^0-9.]", ""));
            return vndFmt.format(d) + " đ";
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

    // =============== ViewHolder ===============

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCode, tvItemsBrief, tvTotal, tvTime, tvStatus;
        ImageView ivStatus;
        View layoutStatusChip;

        View btnAccept, btnArrived, btnPicked, btnOnTheWay, btnDelivered, btnFailed;

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
            btnArrived = v.findViewById(R.id.btn_arrived);
            btnPicked = v.findViewById(R.id.btn_picked);
            btnOnTheWay = v.findViewById(R.id.btn_on_the_way);
            btnDelivered = v.findViewById(R.id.btn_delivered);
            btnFailed = v.findViewById(R.id.btn_failed);
        }
    }
}
