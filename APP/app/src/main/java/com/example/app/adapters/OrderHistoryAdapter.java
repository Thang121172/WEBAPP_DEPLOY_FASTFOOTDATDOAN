package com.example.app.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * OrderHistoryAdapter
 * Dữ liệu mỗi phần tử là Map<String,Object>, gợi ý key:
 * - status | state | event
 * - note | message | detail
 * - time | created_at | ts (ISO8601, epoch ms/s, hoặc chuỗi "yyyy-MM-dd
 * HH:mm:ss")
 *
 * Layout yêu cầu (item_order_history.xml):
 * - iv_status, tv_status, tv_time, tv_note, layout_status_chip
 */
public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.VH> {

    private final List<Map<String, Object>> data;

    public OrderHistoryAdapter(@NonNull List<Map<String, Object>> data) {
        this.data = data;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= data.size())
            return RecyclerView.NO_ID;
        Map<String, Object> m = data.get(position);
        Object id = firstNonNull(m.get("id"), m.get("_id"), m.get("ts"), m.get("time"), m.get("created_at"));
        if (id == null)
            return m.hashCode();
        try {
            // Nếu là số/epoch
            return Long.parseLong(id.toString().replaceAll("\\D+", ""));
        } catch (Exception ignore) {
            return id.toString().hashCode();
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> m = data.get(position);

        String rawStatus = asString(firstNonNull(m.get("status"), m.get("state"), m.get("event"), "update"))
                .trim().toLowerCase(Locale.ROOT);
        String note = asString(firstNonNull(m.get("note"), m.get("message"), m.get("detail"), ""));
        String timeDisp = formatTime(firstNonNull(m.get("time"), m.get("created_at"), m.get("ts")));

        applyStatusChip(h, rawStatus);
        h.tvTime.setText(timeDisp);
        h.tvNote.setText(TextUtils.isEmpty(note) ? "-" : note);
    }

    @Override
    public int getItemCount() {
        return data.size();
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
        return o == null ? null : o.toString();
    }

    private static String formatTime(Object t) {
        if (t == null)
            return "";
        try {
            String s = t.toString().trim();
            if (TextUtils.isEmpty(s))
                return "";
            // epoch ms
            if (s.matches("^\\d{13}$")) {
                long ms = Long.parseLong(s);
                return fmt(ms);
            }
            // epoch s
            if (s.matches("^\\d{10}$")) {
                long ms = Long.parseLong(s) * 1000L;
                return fmt(ms);
            }
            // ISO8601 hoặc yyyy-MM-dd HH:mm:ss
            // Thử ISO trước
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s);
                long ms = odt.toInstant().toEpochMilli();
                return fmt(ms);
            } catch (Exception ignored) {
            }
            // Thử "yyyy-MM-dd HH:mm:ss"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            long ms = sdf.parse(s).getTime();
            return fmt(ms);
        } catch (ParseException e) {
            return t.toString();
        } catch (Exception e) {
            return t.toString();
        }
    }

    private static String fmt(long ms) {
        java.time.Instant ins = java.time.Instant.ofEpochMilli(ms);
        java.time.ZonedDateTime z = java.time.ZonedDateTime.ofInstant(ins, java.time.ZoneId.systemDefault());
        // 24h, ngày/tháng
        return String.format(Locale.getDefault(), "%02d:%02d • %02d/%02d",
                z.getHour(), z.getMinute(), z.getDayOfMonth(), z.getMonthValue());
    }

    private void applyStatusChip(@NonNull VH h, @NonNull String status) {
        int colorText;
        int iconRes;
        int bgColor;

        switch (status) {
            case "delivered":
            case "completed":
            case "done":
            case "success":
                iconRes = R.drawable.ic_status_delivered;
                colorText = ContextCompat.getColor(h.itemView.getContext(), R.color.ff_success);
                bgColor = withAlpha(colorText, 0.10f);
                h.tvStatus.setText("Đã giao");
                break;

            case "shipping":
            case "on_the_way":
            case "preparing":
            case "processing":
            case "in_progress":
            case "assigned":
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(h.itemView.getContext(), R.color.ff_warning);
                bgColor = withAlpha(colorText, 0.12f);
                h.tvStatus.setText("Đang xử lý");
                break;

            case "cancelled":
            case "canceled":
            case "rejected":
            case "failed":
                iconRes = R.drawable.ic_status_cancelled;
                colorText = ContextCompat.getColor(h.itemView.getContext(), R.color.ff_error);
                bgColor = withAlpha(colorText, 0.10f);
                h.tvStatus.setText("Đã hủy");
                break;

            default:
                iconRes = R.drawable.ic_status_inprogress;
                colorText = ContextCompat.getColor(h.itemView.getContext(), R.color.ff_text_secondary);
                bgColor = withAlpha(colorText, 0.08f);
                h.tvStatus.setText(status);
                break;
        }

        h.ivStatus.setImageResource(iconRes);
        h.ivStatus.setColorFilter(colorText);
        h.tvStatus.setTextColor(colorText);

        if (h.layoutStatusChip.getBackground() instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable) h.layoutStatusChip.getBackground().mutate();
            gd.setColor(bgColor);
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * alpha);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ===== ViewHolder =====
    static class VH extends RecyclerView.ViewHolder {
        ImageView ivStatus;
        TextView tvStatus, tvTime, tvNote;
        View layoutStatusChip;

        VH(@NonNull View v) {
            super(v);
            ivStatus = v.findViewById(R.id.iv_status);
            tvStatus = v.findViewById(R.id.tv_status);
            tvTime = v.findViewById(R.id.tv_time);
            tvNote = v.findViewById(R.id.tv_note);
            layoutStatusChip = v.findViewById(R.id.layout_status_chip);
        }
    }
}
