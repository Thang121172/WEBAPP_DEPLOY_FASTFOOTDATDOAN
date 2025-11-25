package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class OtpSuccessFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Khớp với layout đã đổi tên: fragment_otp_success.xml
        return inflater.inflate(R.layout.fragment_otp_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btn = view.findViewById(R.id.btn_go_login);
        if (btn != null) {
            btn.setOnClickListener(v -> safeGoLogin());
        } else {
            // Không có nút: giữ hành vi thủ công, không auto-pop.
        }
    }

    private void safeGoLogin() {
        if (!isAdded())
            return;
        try {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.loginFragment);
        } catch (Exception ignored) {
            // Nếu đang ở Login hoặc backstack không hợp lệ thì bỏ qua.
        }
    }
}
