package com.example.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app.MainActivity;
import com.example.app.network.BackendConfig;

public abstract class BaseActivity extends AppCompatActivity {

    private final BroadcastReceiver authClearedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BackendConfig.ACTION_AUTH_SESSION_CLEARED.equals(intent.getAction())) {
                // Điều hướng về MainActivity (NavHost -> loginFragment), xoá back stack
                Intent i = new Intent(BaseActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                // Kết thúc Activity hiện tại
                BaseActivity.this.finish();
                // Nếu cần đọc lý do:
                // String reason = intent.getStringExtra(BackendConfig.EXTRA_REASON);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(
                authClearedReceiver,
                new IntentFilter(BackendConfig.ACTION_AUTH_SESSION_CLEARED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(authClearedReceiver);
        } catch (Throwable ignored) {
        }
    }
}
