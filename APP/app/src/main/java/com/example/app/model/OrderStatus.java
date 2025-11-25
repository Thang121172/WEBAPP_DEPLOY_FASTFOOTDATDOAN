package com.example.app.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import com.example.app.R;

/**
 * Class định nghĩa các model cần thiết cho việc hiển thị Chi tiết Đơn hàng.
 * Bao gồm trạng thái (StatusType), thông tin lịch sử trạng thái (OrderStatus),
 * thông tin món hàng (OrderItem), và chi tiết đơn hàng tổng thể (OrderDetail).
 */
public class OrderStatus {

    // =========================================================================
    // MODEL: OrderStatus (Thông tin về một bước/trạng thái trong lịch sử)
    // =========================================================================
    private String name;        // Tên trạng thái (VD: Đã giao hàng)
    private String note;        // Ghi chú chi tiết (VD: Shipper đã hoàn tất)
    private long timestamp;     // Thời điểm xảy ra trạng thái (millis)
    private StatusType statusType; // Phân loại trạng thái để tùy chỉnh màu/icon

    public OrderStatus(String name, String note, long timestamp, StatusType statusType) {
        this.name = name;
        this.note = note;
        this.timestamp = timestamp;
        this.statusType = statusType;
    }

    // Getters
    public String getName() { return name; }
    public String getNote() { return note; }
    public long getTimestamp() { return timestamp; }
    public StatusType getStatusType() { return statusType; }

    // =========================================================================
    // ENUM: StatusType (Phân loại trạng thái để ánh xạ tài nguyên)
    // =========================================================================
    public enum StatusType {
        // Ánh xạ với các màu và icon đã định nghĩa trong XML
        PENDING(R.color.ff_status_info, R.drawable.ic_status_inprogress),
        IN_PROGRESS(R.color.ff_status_warning, R.drawable.ic_status_inprogress),
        DELIVERED(R.color.ff_status_success, R.drawable.ic_status_delivered),
        CANCELLED(R.color.ff_status_error, R.drawable.ic_status_cancelled);

        @ColorRes private final int colorResId;
        @DrawableRes private final int iconResId;

        StatusType(@ColorRes int colorResId, @DrawableRes int iconResId) {
            this.colorResId = colorResId;
            this.iconResId = iconResId;
        }

        @ColorRes
        public int getColorResId() { return colorResId; }

        @DrawableRes
        public int getIconResId() { return iconResId; }
    }

    /**
     * Phương thức tĩnh để lấy StatusType từ tên chuỗi (giả định từ API).
     * @param statusName Tên trạng thái từ API.
     * @return StatusType tương ứng.
     */
    public static StatusType fromString(String statusName) {
        if (statusName == null) return StatusType.PENDING;
        switch (statusName.toUpperCase()) {
            case "PENDING":
                return StatusType.PENDING;
            case "IN_PROGRESS":
            case "DELIVERING":
                return StatusType.IN_PROGRESS;
            case "DELIVERED":
            case "COMPLETED":
                return StatusType.DELIVERED;
            case "CANCELLED":
            case "FAILED":
                return StatusType.CANCELLED;
            default:
                return StatusType.PENDING;
        }
    }

    // =========================================================================
    // MODEL: OrderItem (Thông tin về một món hàng trong đơn)
    // =========================================================================
    public static class OrderItem {
        private String name;
        private int quantity;
        private long price; // Đơn giá

        public OrderItem(String name, int quantity, long price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        // Getters
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public long getPrice() { return price; }
        public long getTotal() { return (long) quantity * price; } // Tổng giá trị của món hàng này
    }

    // =========================================================================
    // MODEL: OrderDetail (Thông tin chi tiết toàn bộ đơn hàng)
    // =========================================================================
    public static class OrderDetail {
        private String orderId;
        private String currentStatusName;
        private long orderDate; // Thời điểm đặt đơn
        private String addressTitle;
        private String addressDetail;
        private String paymentMethod;
        private long subtotal;
        private long deliveryFee;
        private long discount;
        private long total;
        private StatusType currentStatusType;
        private java.util.List<OrderItem> items;
        private java.util.List<OrderStatus> history;

        public OrderDetail(String orderId, String currentStatusName, long orderDate, String addressTitle, String addressDetail, String paymentMethod, long subtotal, long deliveryFee, long discount, java.util.List<OrderItem> items, java.util.List<OrderStatus> history) {
            this.orderId = orderId;
            this.currentStatusName = currentStatusName;
            this.orderDate = orderDate;
            this.addressTitle = addressTitle;
            this.addressDetail = addressDetail;
            this.paymentMethod = paymentMethod;
            this.subtotal = subtotal;
            this.deliveryFee = deliveryFee;
            this.discount = discount;
            // Tính tổng thanh toán cuối cùng
            this.total = (subtotal + deliveryFee) - discount;
            this.items = items;
            this.history = history;
            this.currentStatusType = fromString(currentStatusName); // Phân loại trạng thái hiện tại
        }

        // Getters
        public String getOrderId() { return orderId; }
        public String getCurrentStatusName() { return currentStatusName; }
        public long getOrderDate() { return orderDate; }
        public String getAddressTitle() { return addressTitle; }
        public String getAddressDetail() { return addressDetail; }
        public String getPaymentMethod() { return paymentMethod; }
        public long getSubtotal() { return subtotal; }
        public long getDeliveryFee() { return deliveryFee; }
        public long getDiscount() { return discount; }
        public long getTotal() { return total; }
        public StatusType getCurrentStatusType() { return currentStatusType; }
        public java.util.List<OrderItem> getItems() { return items; }
        public java.util.List<OrderStatus> getHistory() { return history; }
    }
}