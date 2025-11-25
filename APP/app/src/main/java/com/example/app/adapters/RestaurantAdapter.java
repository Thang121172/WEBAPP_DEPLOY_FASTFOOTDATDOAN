package com.example.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.app.R;
import com.example.app.model.Restaurant;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private final Context context;
    private List<Restaurant> restaurantList;
    private final OnRestaurantClickListener clickListener;

    public RestaurantAdapter(Context context,
                             List<Restaurant> restaurantList,
                             OnRestaurantClickListener clickListener) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);

        // 1. Gán Tên
        holder.tvRestaurantName.setText(restaurant.getName());

        // 2. Gán Rating
        holder.tvRating.setText(String.valueOf(restaurant.getRating()));

        // 2.5. Gán Khoảng cách (nếu có)
        if (holder.tvDistance != null) {
            String distanceText = restaurant.getFormattedDistance();
            if (distanceText != null) {
                holder.tvDistance.setText(distanceText);
                holder.tvDistance.setVisibility(View.VISIBLE);
            } else {
                holder.tvDistance.setVisibility(View.GONE);
            }
        }

        // 3. Ảnh - ✅ FIX: Xử lý trường hợp imageUrl null hoặc rỗng
        String imageUrl = restaurant.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivImage);
        } else {
            // Nếu không có ảnh, hiển thị placeholder
            Glide.with(context)
                    .load(R.drawable.ic_image_placeholder)
                    .into(holder.ivImage);
        }

        // 4. Xử lý click
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRestaurantClick(restaurant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public void updateData(List<Restaurant> newRestaurantList) {
        this.restaurantList = newRestaurantList;
        notifyDataSetChanged();
    }

    public static class RestaurantViewHolder extends RecyclerView.ViewHolder {
        TextView tvRestaurantName;
        TextView tvRating;
        ImageView ivImage;
        TextView tvDescription;
        TextView tvDistance; // Khoảng cách

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRating = itemView.findViewById(R.id.tvRating);
            ivImage = itemView.findViewById(R.id.imgRestaurant);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDistance = itemView.findViewById(R.id.tvDistance); // Có thể null nếu layout chưa có
        }
    }
}
