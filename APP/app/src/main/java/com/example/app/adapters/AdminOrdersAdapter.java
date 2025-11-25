package com.example.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.ViewHolder> {

    private List<Map<String, Object>> orders;
    private NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

    public AdminOrdersAdapter(List<Map<String, Object>> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> order = orders.get(position);
        
        // Order code
        Object code = order.get("code");
        holder.tvOrderCode.setText(code != null ? "Đơn #" + code.toString() : "N/A");
        
        // Status
        Object statusObj = order.get("status");
        String status = statusObj != null ? statusObj.toString() : "UNKNOWN";
        holder.tvStatus.setText(getStatusText(status));
        int statusColor = getStatusColor(holder.itemView.getContext(), status);
        holder.tvStatus.setBackgroundColor(statusColor);
        
        // Customer info
        Object customerName = order.get("customer_name");
        Object customerEmail = order.get("customer_email");
        if (customerName != null && !customerName.toString().isEmpty() && !customerName.toString().equals("N/A")) {
            holder.tvCustomer.setText("Khách hàng: " + customerName.toString());
        } else if (customerEmail != null) {
            holder.tvCustomer.setText("Khách hàng: " + customerEmail.toString());
        } else {
            holder.tvCustomer.setText("Khách hàng: N/A");
        }
        
        // Restaurant
        Object restaurantName = order.get("restaurant_name");
        holder.tvRestaurant.setText("Nhà hàng: " + (restaurantName != null ? restaurantName.toString() : "N/A"));
        
        // Total
        Object total = order.get("total");
        if (total != null) {
            try {
                double totalValue = ((Number) total).doubleValue();
                holder.tvTotal.setText(numberFormat.format(totalValue) + "₫");
            } catch (Exception e) {
                holder.tvTotal.setText(total.toString() + "₫");
            }
        } else {
            holder.tvTotal.setText("0₫");
        }
        
        // Address
        Object address = order.get("address");
        if (address != null) {
            holder.tvAddress.setText("Địa chỉ: " + address.toString());
        } else {
            holder.tvAddress.setText("Địa chỉ: N/A");
        }
        
        // Date
        Object createdAt = order.get("created_at");
        if (createdAt != null) {
            try {
                String dateStr = createdAt.toString();
                // Format date if needed
                holder.tvDate.setText("Ngày đặt: " + dateStr);
            } catch (Exception e) {
                holder.tvDate.setText("Ngày đặt: N/A");
            }
        } else {
            holder.tvDate.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ xử lý";
            case "CONFIRMED":
                return "Đã xác nhận";
            case "COOKING":
            case "PREPARING":
                return "Đang chuẩn bị";
            case "READY":
                return "Sẵn sàng";
            case "PICKED_UP":
                return "Đã nhận";
            case "DELIVERING":
            case "SHIPPING":
                return "Đang giao";
            case "DELIVERED":
                return "Đã giao";
            case "CANCELED":
            case "CANCELLED":
                return "Đã hủy";
            case "CANCEL_REQUESTED":
                return "Yêu cầu hủy";
            default:
                return status;
        }
    }

    private int getStatusColor(android.content.Context context, String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
            case "CONFIRMED":
                return ContextCompat.getColor(context, android.R.color.holo_orange_dark);
            case "COOKING":
            case "PREPARING":
                return ContextCompat.getColor(context, android.R.color.holo_blue_dark);
            case "READY":
                return ContextCompat.getColor(context, android.R.color.holo_blue_bright);
            case "DELIVERING":
            case "SHIPPING":
            case "PICKED_UP":
                return ContextCompat.getColor(context, android.R.color.holo_purple);
            case "DELIVERED":
                return ContextCompat.getColor(context, android.R.color.holo_green_dark);
            case "CANCELED":
            case "CANCELLED":
            case "CANCEL_REQUESTED":
                return ContextCompat.getColor(context, android.R.color.holo_red_dark);
            default:
                return ContextCompat.getColor(context, android.R.color.darker_gray);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderCode, tvStatus, tvCustomer, tvRestaurant, tvTotal, tvAddress, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvCustomer = itemView.findViewById(R.id.tv_customer);
            tvRestaurant = itemView.findViewById(R.id.tv_restaurant);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}

