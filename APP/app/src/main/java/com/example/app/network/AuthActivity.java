package com.example.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AuthActivity chứa các Fragment liên quan đến đăng nhập/đăng ký/OTP.
 * Đây là đích đến sau khi người dùng logout khỏi MainActivity.
 */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Giả định layout này chứa NavHostFragment để điều hướng giữa Login, Register, OTP.
        setContentView(R.layout.activity_auth_login);
    }
}