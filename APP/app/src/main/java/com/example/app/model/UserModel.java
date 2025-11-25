package com.example.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * UserModel: Mô hình dữ liệu cơ bản cho thông tin người dùng.
 * Dùng để lưu trữ/hiển thị thông tin người dùng đã đăng nhập.
 */
public class UserModel {

    // Thường là ID từ database
    @SerializedName("id")
    public String id;

    // Tên đầy đủ
    @SerializedName("name")
    public String name;

    // Email (cũng thường dùng làm username)
    @SerializedName("email")
    public String email;

    // Vai trò (ví dụ: "customer", "admin", "shipper")
    @SerializedName("role")
    public String role;

    // Ảnh đại diện (nếu có)
    @SerializedName("avatar_url")
    public String avatarUrl;

    // Các thông tin khác có thể thêm vào: phone, address, v.v.
}