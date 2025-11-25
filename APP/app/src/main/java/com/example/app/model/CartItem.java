package com.example.app.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Model đại diện cho 1 item trong giỏ hàng.
 */
public class CartItem {

    public String cartItemId;
    public String productId;
    public String title;
    public String brandName;
    public double price;
    public int quantity;
    public String imageUrl;
    public String options;
    public int restaurantId; // ID của nhà hàng
    public Double restaurantDistance; // Khoảng cách từ restaurant đến khách hàng (km)

    /**
     * ✅ Constructor CŨ – dành cho code cũ vẫn đang dùng:
     * new CartItem(itemId, title, brandName, price, quantity, options)
     */
    public CartItem(String itemId,
                    String title,
                    String brandName,
                    double price,
                    int quantity,
                    String options) {

        this.cartItemId = UUID.randomUUID().toString();
        this.productId = itemId;
        this.title = title;
        this.brandName = brandName;
        this.price = price;
        this.quantity = quantity;
        this.options = options;
        this.restaurantId = 0; // Mặc định 0 nếu không có
    }

    /**
     * ✅ Constructor MỚI (8 tham số → KHÔNG bị trùng signature)
     * dùng bởi CartRepository.addToCart(...)
     */
    public CartItem(String cartItemId,
                    String productId,
                    String title,
                    double price,
                    int quantity,
                    String imageUrl,
                    String options,
                    int restaurantId) {
        this(cartItemId, productId, title, price, quantity, imageUrl, options, restaurantId, null);
    }

    /**
     * ✅ Constructor MỚI với distance (9 tham số)
     */
    public CartItem(String cartItemId,
                    String productId,
                    String title,
                    double price,
                    int quantity,
                    String imageUrl,
                    String options,
                    int restaurantId,
                    Double restaurantDistance) {

        this.cartItemId = (cartItemId != null) ? cartItemId : UUID.randomUUID().toString();
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.options = options;
        this.restaurantId = restaurantId;
        this.restaurantDistance = restaurantDistance;
    }

    // --- GETTERS ---

    public String getName() { return title; }
    public String getTitle() { return title; }

    public String getBrandName() {
        return (brandName != null && !brandName.isEmpty()) ? brandName : "Chưa xác định";
    }

    public String getItemId() { return productId; }
    public String getProductId() { return productId; }

    public String getCartItemId() { return cartItemId; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public String getImageUrl() { return imageUrl; }
    public String getOptions() { return options; }
    public int getRestaurantId() { return restaurantId; }
    public Double getRestaurantDistance() { return restaurantDistance; }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getLineTotal() {
        return price * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem)) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(productId, cartItem.productId) &&
                Objects.equals(options, cartItem.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, options);
    }
}
