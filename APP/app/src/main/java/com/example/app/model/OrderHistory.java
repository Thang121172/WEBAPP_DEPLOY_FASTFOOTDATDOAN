package com.example.app.model;

/**
 * Lớp Model đại diện cho một bước (step) trong lịch sử trạng thái của đơn hàng.
 * Dùng để cung cấp dữ liệu cho OrderHistoryAdapter.
 */
public class OrderHistory {
    private String title; // Tên trạng thái (VD: Đã giao hàng, Đang vận chuyển)
    private String status; // Mã trạng thái gốc (VD: DELIVERED, SHIPPING)
    private String time; // Thời gian cập nhật (đã định dạng, VD: 14:30, 20/07)
    private String note; // Ghi chú chi tiết (nếu có)
    private boolean isCurrentStep; // Bước này có phải là bước hiện tại/cuối cùng không

    // Constructor
    public OrderHistory(String title, String status, String time, String note, boolean isCurrentStep) {
        this.title = title;
        this.status = status;
        this.time = time;
        this.note = note;
        this.isCurrentStep = isCurrentStep;
    }

    // --- Getters ---

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getTime() {
        return time;
    }

    public String getNote() {
        return note;
    }

    public boolean isCurrentStep() {
        return isCurrentStep;
    }

    // --- Setters (tùy chọn, thêm vào nếu cần thay đổi giá trị) ---

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setCurrentStep(boolean currentStep) {
        isCurrentStep = currentStep;
    }
}