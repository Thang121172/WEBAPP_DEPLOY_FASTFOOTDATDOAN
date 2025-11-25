package com.example.app.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MerchantMenuAdapter
 * Hiển thị danh sách món ăn với nút Sửa và Xóa
 */
public class MerchantMenuAdapter extends RecyclerView.Adapter<MerchantMenuAdapter.VH> {

    public interface Listener {
        void onEdit(@NonNull Map<String, Object> item, int position);
        void onDelete(@NonNull Map<String, Object> item, int position);
    }

    private final Context context;
    private final List<Map<String, Object>> data;
    private final Listener listener;

    public MerchantMenuAdapter(@NonNull Context context,
                               @NonNull List<Map<String, Object>> data,
                               @NonNull Listener listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_merchant_menu, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, Object> item = data.get(position);

        // Name
        String name = getString(item.get("name"));
        holder.tvName.setText(TextUtils.isEmpty(name) ? "Không có tên" : name);

        // Description
        String description = getString(item.get("description"));
        holder.tvDescription.setText(TextUtils.isEmpty(description) ? "Không có mô tả" : description);
        holder.tvDescription.setVisibility(TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);

        // Price
        Object priceObj = item.get("price");
        double price = 0.0;
        if (priceObj instanceof Number) {
            price = ((Number) priceObj).doubleValue();
        } else if (priceObj != null) {
            try {
                price = Double.parseDouble(priceObj.toString());
            } catch (Exception ignored) {
            }
        }
        holder.tvPrice.setText(formatCurrency(price));

        // Edit button
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item, holder.getAdapterPosition());
            }
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDescription;
        TextView tvPrice;
        com.google.android.material.button.MaterialButton btnEdit;
        com.google.android.material.button.MaterialButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    private static String getString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private static String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }
}

