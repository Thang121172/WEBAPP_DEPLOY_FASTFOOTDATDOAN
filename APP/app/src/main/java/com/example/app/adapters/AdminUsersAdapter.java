package com.example.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> users;
    private Callbacks callbacks;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface Callbacks {
        void onToggleStatus(int userId, boolean isActive);
    }

    public AdminUsersAdapter(Context context, List<Map<String, Object>> users, Callbacks callbacks) {
        this.context = context;
        this.users = users;
        this.callbacks = callbacks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> user = users.get(position);
        
        // User ID
        Object idObj = user.get("id");
        int userId = idObj != null ? ((Number) idObj).intValue() : 0;
        
        // Email
        Object email = user.get("email");
        holder.tvEmail.setText(email != null ? email.toString() : "N/A");
        
        // Full name
        Object fullName = user.get("full_name");
        holder.tvFullName.setText(fullName != null ? fullName.toString() : "Chưa cập nhật");
        
        // Phone
        Object phone = user.get("phone");
        if (phone != null && !phone.toString().isEmpty()) {
            holder.tvPhone.setText(phone.toString());
            holder.tvPhone.setVisibility(View.VISIBLE);
        } else {
            holder.tvPhone.setVisibility(View.GONE);
        }
        
        // Role
        Object role = user.get("role");
        String roleText = "N/A";
        int roleColor = 0xFF666666;
        if (role != null) {
            String roleStr = role.toString().toUpperCase();
            switch (roleStr) {
                case "USER":
                    roleText = "Khách hàng";
                    roleColor = 0xFF3498DB;
                    break;
                case "MERCHANT":
                    roleText = "Merchant";
                    roleColor = 0xFF27AE60;
                    break;
                case "SHIPPER":
                    roleText = "Shipper";
                    roleColor = 0xFFE67E22;
                    break;
                case "ADMIN":
                    roleText = "Admin";
                    roleColor = 0xFF9B59B6;
                    break;
                default:
                    roleText = roleStr;
            }
        }
        holder.tvRole.setText(roleText);
        holder.tvRole.setTextColor(roleColor);
        
        // Status
        Object isActiveObj = user.get("is_active");
        boolean isActive = true;
        if (isActiveObj != null) {
            if (isActiveObj instanceof Boolean) {
                isActive = (Boolean) isActiveObj;
            } else if (isActiveObj instanceof String) {
                isActive = Boolean.parseBoolean(isActiveObj.toString());
            }
        }
        
        holder.switchStatus.setChecked(isActive);
        holder.tvStatusLabel.setText(isActive ? "Hoạt động" : "Đã khóa");
        holder.tvStatusLabel.setTextColor(isActive ? 0xFF27AE60 : 0xFFE74C3C);
        holder.switchStatus.setOnCheckedChangeListener(null); // Clear listener to avoid triggering during binding
        
        holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            holder.tvStatusLabel.setText(isChecked ? "Hoạt động" : "Đã khóa");
            holder.tvStatusLabel.setTextColor(isChecked ? 0xFF27AE60 : 0xFFE74C3C);
            if (callbacks != null) {
                callbacks.onToggleStatus(userId, isChecked);
            }
        });
        
        // Created date
        Object createdAt = user.get("created_at");
        if (createdAt != null) {
            try {
                String dateStr = createdAt.toString();
                holder.tvCreatedDate.setText("Ngày tạo: " + dateStr);
                holder.tvCreatedDate.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                holder.tvCreatedDate.setVisibility(View.GONE);
            }
        } else {
            holder.tvCreatedDate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail, tvFullName, tvPhone, tvRole, tvCreatedDate, tvStatusLabel;
        Switch switchStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvFullName = itemView.findViewById(R.id.tv_full_name);
            tvPhone = itemView.findViewById(R.id.tv_phone);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvCreatedDate = itemView.findViewById(R.id.tv_created_date);
            tvStatusLabel = itemView.findViewById(R.id.tv_status_label);
            switchStatus = itemView.findViewById(R.id.switch_status);
        }
    }
}

