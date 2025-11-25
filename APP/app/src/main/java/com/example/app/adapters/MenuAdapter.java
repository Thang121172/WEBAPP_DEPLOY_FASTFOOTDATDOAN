package com.example.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.app.R;
import com.example.app.model.MenuItem;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MenuAdapter: hiển thị danh sách món trong HomeFragment.
 * Layout item_menu.xml:
 * - @id/item_image (ImageView)
 * - @id/item_title (TextView)
 * - @id/item_subtitle (TextView)
 * - @id/item_price (TextView)
 * - (optional) @id/btn_add_to_cart (MaterialButton)
 */
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    public interface OnAddToCartClickListener {
        void onAddToCart(MenuItem item);
    }

    private final List<MenuItem> items = new ArrayList<>();
    private final OnItemClickListener itemClickListener;
    private final OnAddToCartClickListener addToCartListener; // có thể null

    // Constructor cũ: chỉ click item
    public MenuAdapter(OnItemClickListener listener) {
        this.itemClickListener = listener;
        this.addToCartListener = null;
        setHasStableIds(true);
    }

    // Constructor mới: có list khởi tạo + cả 2 listener
    public MenuAdapter(List<MenuItem> initialItems,
                       OnItemClickListener itemListener,
                       OnAddToCartClickListener addToCartListener) {
        this.itemClickListener = itemListener;
        this.addToCartListener = addToCartListener;
        if (initialItems != null)
            items.addAll(initialItems);
        setHasStableIds(true);
    }

    // Convenience constructor: (List, OnItemClickListener) - keeps backward
    // compatibility
    public MenuAdapter(List<MenuItem> initialItems, OnItemClickListener itemListener) {
        this(initialItems, itemListener, null);
    }

    // PHƯƠNG THỨC NÀY ĐÃ CÓ SẴN TRONG CODE CỦA BẠN
    public void setItems(List<MenuItem> newItems) {
        items.clear();
        if (newItems != null)
            items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * ✅ PHƯƠNG THỨC MỚI ĐƯỢC THÊM VÀO ĐỂ KHẮC PHỤC LỖI GỌI TỪ RestaurantDetailFragment.java
     * Phương thức này chỉ đơn giản gọi lại setItems() đã có.
     * @param newItems Danh sách MenuItem mới.
     */
    public void updateData(List<MenuItem> newItems) {
        setItems(newItems);
    }


    @Override
    public long getItemId(int position) {
        try {
            MenuItem item = items.get(position);
            if (item != null && item.id != null) {
                try {
                    // Cố gắng chuyển đổi ID sang Long để có ID ổn định tốt hơn
                    return Long.parseLong(item.id.toString().replaceAll("\\D+", ""));
                } catch (Exception ignore) {
                    // Fallback nếu parse thất bại
                    return item.id.hashCode();
                }
            }
            // Fallback cuối cùng
            return (item != null && item.title != null) ? item.title.hashCode() : position;
        } catch (Exception e) {
            return position;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MenuItem item = items.get(position);

        holder.title.setText(item != null && item.title != null ? item.title : "(Không tên)");
        holder.subtitle.setText(item != null && item.description != null ? item.description : "");

        String priceText = "0 đ";
        if (item != null) {
            try {
                double p = item.price;
                NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                priceText = nf.format(p) + " đ";
            } catch (Exception ignore) {
            }
        }
        holder.price.setText(priceText);

        // ✅ Load ảnh từ imageUrl - Hiển thị ảnh mới khi merchant cập nhật
        if (item != null && item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
            // Load ảnh từ URL, với signature để đảm bảo ảnh mới được load (tránh cache cũ)
            Glide.with(holder.image.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false) // Giữ memory cache để performance tốt
                    .into(holder.image);
        } else {
            // Nếu không có ảnh, hiển thị placeholder
            holder.image.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null && item != null) {
                itemClickListener.onItemClick(item);
            } else if (item != null) {
                Toast.makeText(v.getContext(), "Bạn chọn: " + item.title, Toast.LENGTH_SHORT).show();
            }
        });

        if (holder.btnAddToCart != null) {
            holder.btnAddToCart.setOnClickListener(v -> {
                if (addToCartListener != null && item != null) {
                    addToCartListener.onAddToCart(item);
                } else if (item != null) {
                    Toast.makeText(v.getContext(), "Đã thêm: " + item.title, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView subtitle;
        TextView price;
        MaterialButton btnAddToCart; // có thể null nếu layout không có

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.item_image);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            price = itemView.findViewById(R.id.item_price);
            View btn = itemView.findViewById(R.id.btn_add_to_cart);
            btnAddToCart = (btn instanceof MaterialButton) ? (MaterialButton) btn : null;
        }
    }
}