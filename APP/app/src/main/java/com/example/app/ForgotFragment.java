package com.example.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.app.databinding.FragmentForgotBinding;
import com.example.app.network.AuthApi;
import com.example.app.network.AuthClient;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ForgotFragment
 *
 * Flow:
 * 1) Gửi OTP reset qua /auth/send-otp (AuthClient.requestResetOtp).
 * 2) Điều hướng sang OtpVerifyFragment (mode="reset") để nhập OTP + mật khẩu
 * mới.
 */
public class ForgotFragment extends Fragment {

    private FragmentForgotBinding binding;
    private AuthClient authClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authClient = new AuthClient(requireContext());

        // Ẩn lỗi khi gõ lại
        binding.tvError.setVisibility(View.GONE);
        binding.tvInfo.setVisibility(View.GONE);
        binding.etEmail.addTextChangedListener(SimpleWatchers.hide(binding.tvError, binding.tvInfo));

        // Gửi OTP reset
        binding.btnSendOtp.setOnClickListener(v -> {
            if (!binding.btnSendOtp.isEnabled())
                return;

            binding.tvError.setVisibility(View.GONE);
            binding.tvInfo.setVisibility(View.GONE);

            final String emailInput = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim().toLowerCase()
                    : "";

            if (TextUtils.isEmpty(emailInput)) {
                showError(getString(R.string.enter_email));
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                showError(getString(R.string.err_invalid_email));
                return;
            }

            setLoading(true);

            authClient.requestResetOtp(emailInput, new Callback<AuthApi.GenericResponse>() {
                @Override
                public void onResponse(Call<AuthApi.GenericResponse> call, Response<AuthApi.GenericResponse> response) {
                    if (!isAdded())
                        return;
                    setLoading(false);

                    if (response.isSuccessful()) {
                        // Thông báo và chuyển qua màn OTP reset
                        showInfo(getString(R.string.otp_sent_check_email_or_logs));

                        Bundle b = new Bundle();
                        b.putString("mode", "reset");
                        b.putString("email", emailInput);
                        b.putString("info_msg", getString(R.string.otp_enter_prompt));

                        // Dùng action đúng từ nav_graph
                        try {
                            NavHostFragment.findNavController(ForgotFragment.this)
                                    .navigate(R.id.action_forgotFragment_to_otpVerifyFragment, b);
                        } catch (Exception e) {
                            // Fallback điều hướng trực tiếp (nếu action bị đổi tên)
                            try {
                                NavHostFragment.findNavController(ForgotFragment.this)
                                        .navigate(R.id.otpVerifyFragment, b);
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        String msg = readErrorMessage(response);
                        if (TextUtils.isEmpty(msg))
                            msg = getString(R.string.err_cannot_send_otp);
                        showError(msg);
                    }
                }

                @Override
                public void onFailure(Call<AuthApi.GenericResponse> call, Throwable t) {
                    if (!isAdded())
                        return;
                    setLoading(false);
                    showError(getString(R.string.network_error));
                }
            });
        });

        // Quay lại Login
        binding.tvBackLogin.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(ForgotFragment.this)
                        .navigate(R.id.loginFragment);
            } catch (Exception ignored) {
            }
        });
    }

    private void setLoading(boolean loading) {
        if (binding == null)
            return;
        binding.btnSendOtp.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
        binding.tvBackLogin.setEnabled(!loading);
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
        binding.tvInfo.setVisibility(View.GONE);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showInfo(String msg) {
        binding.tvInfo.setText(msg);
        binding.tvInfo.setVisibility(View.VISIBLE);
        binding.tvError.setVisibility(View.GONE);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private String readErrorMessage(Response<?> response) {
        try {
            if (response != null && response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (!TextUtils.isEmpty(raw)) {
                    JSONObject obj = new JSONObject(raw);
                    if (obj.has("message"))
                        return obj.optString("message");
                    if (obj.has("detail"))
                        return obj.optString("detail");
                    if (obj.has("error"))
                        return obj.optString("error");
                    return raw;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Ẩn lỗi/info khi user nhập lại
    private static class SimpleWatchers {
        static android.text.TextWatcher hide(View errorView, View infoView) {
            return new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (errorView != null)
                        errorView.setVisibility(View.GONE);
                    if (infoView != null)
                        infoView.setVisibility(View.GONE);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            };
        }
    }
}
