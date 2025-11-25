package com.example.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminCancelRequestsAdapter extends RecyclerView.Adapter<AdminCancelRequestsAdapter.ViewHolder> {

    private List<Map<String, Object>> orders;
    private Callbacks callbacks;
    private NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface Callbacks {
        void onApprove(int orderId);
        void onReject(int orderId);
    }

    public AdminCancelRequestsAdapter(List<Map<String, Object>> orders, Callbacks callbacks) {
        this.orders = orders;
        this.callbacks = callbacks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_cancel_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> order = orders.get(position);
        
        // Order code
        Object code = order.get("code");
        holder.tvOrderCode.setText(code != null ? "Đơn #" + code.toString() : "N/A");
        
        // Customer info
        Object customerName = order.get("customer_name");
        Object customerEmail = order.get("customer_email");
        if (customerName != null) {
            holder.tvCustomer.setText(customerName.toString());
        } else if (customerEmail != null) {
            holder.tvCustomer.setText(customerEmail.toString());
        } else {
            holder.tvCustomer.setText("Khách hàng");
        }
        
        // Restaurant
        Object restaurantName = order.get("restaurant_name");
        holder.tvRestaurant.setText(restaurantName != null ? restaurantName.toString() : "N/A");
        
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
        
        // Cancel reason
        Object reason = order.get("cancel_reason");
        if (reason != null) {
            holder.tvReason.setText("Lý do: " + reason.toString());
            holder.tvReason.setVisibility(View.VISIBLE);
        } else {
            holder.tvReason.setVisibility(View.GONE);
        }
        
        // Address
        Object address = order.get("delivery_address");
        if (address != null) {
            holder.tvAddress.setText("Địa chỉ: " + address.toString());
            holder.tvAddress.setVisibility(View.VISIBLE);
        } else {
            holder.tvAddress.setVisibility(View.GONE);
        }
        
        // Date
        Object createdAt = order.get("created_at");
        if (createdAt != null) {
            try {
                // Try to parse date string
                String dateStr = createdAt.toString();
                holder.tvDate.setText("Ngày đặt: " + dateStr);
            } catch (Exception e) {
                holder.tvDate.setText("Ngày đặt: N/A");
            }
        } else {
            holder.tvDate.setText("");
        }
        
        // Buttons
        Object orderIdObj = order.get("id");
        if (orderIdObj != null) {
            int orderId = ((Number) orderIdObj).intValue();
            
            holder.btnApprove.setOnClickListener(v -> {
                if (callbacks != null) {
                    callbacks.onApprove(orderId);
                }
            });
            
            holder.btnReject.setOnClickListener(v -> {
                if (callbacks != null) {
                    callbacks.onReject(orderId);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderCode, tvCustomer, tvRestaurant, tvTotal, tvReason, tvAddress, tvDate;
        Button btnApprove, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvCustomer = itemView.findViewById(R.id.tv_customer);
            tvRestaurant = itemView.findViewById(R.id.tv_restaurant);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvReason = itemView.findViewById(R.id.tv_reason);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}

