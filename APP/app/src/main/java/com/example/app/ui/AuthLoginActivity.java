package com.example.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app.MainActivity;
import com.example.app.R;

public class AuthLoginActivity extends AppCompatActivity {

    private static final String TAG = "AuthLoginActivity";

    /**
     * Interface để các Fragment (Login, Register) gọi ngược lại
     * Activity khi quá trình xác thực hoàn tất thành công.
     */
    public interface AuthSuccessListener {
        void onAuthSuccess();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_login);
    }

    /**
     * Hàm này được gọi từ các Fragment (Login/Register) sau khi lưu token thành công.
     * Nó chịu trách nhiệm chuyển sang MainActivity và đóng AuthLoginActivity.
     */
    public void onAuthSuccess() {
        Log.d(TAG, "Auth successful. Navigating to MainActivity.");

        Intent intent = new Intent(this, MainActivity.class);
        // Cờ NEW_TASK và CLEAR_TASK không cần thiết ở đây vì MainActivity đã xử lý
        // việc kiểm tra phiên và AuthLoginActivity chỉ là một lớp con của Task.
        startActivity(intent);

        // *** ĐIỂM QUAN TRỌNG NHẤT: KẾT THÚC Activity đăng nhập này. ***
        finish();
    }
}