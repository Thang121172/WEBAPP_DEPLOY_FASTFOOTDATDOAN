package com.example.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * Restaurant
 * 
 * Map trực tiếp JSON từ backend:
 * { id, name, address, rating, image_url }
 */
public class Restaurant {
    
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;

    @SerializedName("address")
    public String address;

    @SerializedName("rating")
    public float rating;
    
    @SerializedName("image_url")
    public String imageUrl; // có thể null nếu backend chưa trả

    @SerializedName("lat")
    public Double latitude; // vĩ độ

    @SerializedName("lng")
    public Double longitude; // kinh độ

    @SerializedName("distance")
    public Double distance; // khoảng cách (km) - chỉ có khi query với location

    // Constructor không tham số: cần cho Gson
    public Restaurant() {
    }

    // Constructor đầy đủ: tiện tạo thủ công
    public Restaurant(String id, String name, String address, float rating, String imageUrl) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public float getRating() {
        return rating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getDistance() {
        return distance;
    }

    /**
     * Format khoảng cách để hiển thị
     * @return String như "1.2 km" hoặc "500 m"
     */
    public String getFormattedDistance() {
        if (distance == null) return null;
        if (distance < 1.0) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }
}