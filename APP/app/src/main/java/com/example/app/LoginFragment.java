package com.example.app;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.app.databinding.FragmentLoginBinding;
import com.example.app.network.AuthApi;
import com.example.app.network.AuthClient;
import com.example.app.ui.AuthLoginActivity; // Import cần thiết

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginFragment (Node backend)
 *
 * - Đăng nhập qua AuthClient.login(loginId, password)
 * - Lưu token/profile trong AuthClient
 * - Nếu 401 do chưa verify: gửi lại OTP và điều hướng sang OtpVerifyFragment
 * - Điều hướng ra khỏi AuthLoginActivity sang MainActivity khi thành công.
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthClient authClient;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authClient = new AuthClient(requireContext());

        // Ẩn lỗi khi gõ lại
        if (binding.etUsername != null) {
            binding.etUsername.addTextChangedListener(SimpleTextWatchers.hide(binding.tvError));
        }
        if (binding.etPassword != null) {
            binding.etPassword.addTextChangedListener(SimpleTextWatchers.hide(binding.tvError));
        }

        // Đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            binding.tvError.setVisibility(View.GONE);

            final String loginId = binding.etUsername.getText() != null
                    ? binding.etUsername.getText().toString().trim().toLowerCase()
                    : "";
            final String password = binding.etPassword.getText() != null
                    ? binding.etPassword.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(loginId) || TextUtils.isEmpty(password)) {
                binding.tvError.setText(getString(R.string.err_login_fill));
                binding.tvError.setVisibility(View.VISIBLE);
                return;
            }

            hideKeyboard();
            setLoading(true);

            authClient.login(loginId, password, new Callback<AuthApi.LoginResponse>() {
                @Override
                public void onResponse(
                        Call<AuthApi.LoginResponse> call,
                        Response<AuthApi.LoginResponse> response) {
                    if (!isAdded())
                        return;
                    setLoading(false);

                    if (response.isSuccessful()
                            && response.body() != null
                            // Giả định AuthClient đã tự lưu token thành công ở đây
                            && !TextUtils.isEmpty(authClient.getAccessToken())) {

                        Toast.makeText(getContext(), getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                        // ===============================================
                        // === Xử lý điều hướng sang MainActivity ===
                        // ===============================================

                        // 1. Gọi hàm onAuthSuccess() của Activity cha (AuthLoginActivity)
                        // để chuyển sang MainActivity và đóng AuthLoginActivity hiện tại.
                        if (getActivity() instanceof AuthLoginActivity) {
                            ((AuthLoginActivity) getActivity()).onAuthSuccess();
                        } else {
                            // Trường hợp fallback nếu Fragment không gắn vào AuthLoginActivity
                            Toast.makeText(getContext(), "Lỗi: Không thể chuyển màn hình chính.", Toast.LENGTH_LONG).show();
                        }

                        // ===============================================

                    } else {
                        String msg = safeParseErrorMessage(response);
                        if (TextUtils.isEmpty(msg))
                            msg = getString(R.string.login_failed);
                        binding.tvError.setText(msg);
                        binding.tvError.setVisibility(View.VISIBLE);

                        if (looksLikeUnverified(msg)) {
                            resendOtpAndGoOtp(loginId, password);
                        }
                    }
                }

                @Override
                public void onFailure(Call<AuthApi.LoginResponse> call, Throwable t) {
                    if (!isAdded())
                        return;
                    setLoading(false);
                    String msg = (t.getMessage() != null) ? t.getMessage() : getString(R.string.network_error);
                    binding.tvError.setText(msg);
                    binding.tvError.setVisibility(View.VISIBLE);
                }
            });
        });

        // “Chưa có tài khoản? Đăng ký”
        binding.tvToRegister.setOnClickListener(v -> {
            NavController nav = NavHostFragment.findNavController(LoginFragment.this);
            tryNavigate(nav, R.id.action_loginFragment_to_registerFragment, R.id.registerFragment);
        });

        // “Quên mật khẩu”
        binding.tvForgot.setOnClickListener(v -> {
            NavController nav = NavHostFragment.findNavController(LoginFragment.this);
            tryNavigate(nav, R.id.action_loginFragment_to_forgotFragment, R.id.forgotFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ========================
    // Helpers
    // ========================

    private void setLoading(boolean loading) {
        if (binding == null)
            return;
        binding.etUsername.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
        binding.btnLogin.setEnabled(!loading);
        binding.tvToRegister.setEnabled(!loading);
        binding.tvForgot.setEnabled(!loading);
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        if (!isAdded() || getView() == null)
            return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    // Hàm navigateByRole đã bị loại bỏ vì không cần thiết cho việc chuyển Activity.

    private boolean tryNavigate(NavController nav, int... destIds) {
        for (int id : destIds) {
            try {
                nav.navigate(id);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Nullable
    private String safeParseErrorMessage(Response<?> response) {
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

    private boolean looksLikeUnverified(@Nullable String msg) {
        if (TextUtils.isEmpty(msg))
            return false;
        String m = msg.toLowerCase();
        return m.contains("verify")
                || m.contains("unverified")
                || m.contains("chưa xác thực")
                || m.contains("not verified");
    }

    private void resendOtpAndGoOtp(@NonNull String email, @NonNull String passwordJustTried) {
        if (!isAdded())
            return;
        setLoading(true);
        Toast.makeText(getContext(),
                getString(R.string.account_not_verified_resending_otp),
                Toast.LENGTH_SHORT).show();

        authClient.requestRegisterOtp(email, new Callback<AuthApi.GenericResponse>() {
            @Override
            public void onResponse(Call<AuthApi.GenericResponse> call, Response<AuthApi.GenericResponse> resp) {
                if (!isAdded())
                    return;
                setLoading(false);

                if (resp.isSuccessful()) {
                    Toast.makeText(getContext(),
                            getString(R.string.otp_sent_check_email_or_logs),
                            Toast.LENGTH_LONG).show();

                    Bundle b = new Bundle();
                    b.putString(OtpVerifyFragment.ARG_MODE, "register");
                    b.putString(OtpVerifyFragment.ARG_EMAIL, email);
                    b.putString(OtpVerifyFragment.ARG_PASSWORD, passwordJustTried);

                    // ĐÃ THÊM: Kiểm tra trạng thái gắn kết trước khi điều hướng để tránh lỗi
                    if (isAdded()) {
                        NavHostFragment.findNavController(LoginFragment.this)
                                .navigate(R.id.action_loginFragment_to_otpVerifyFragment, b);
                    }
                } else {
                    Toast.makeText(getContext(),
                            getString(R.string.err_cannot_send_otp_auto),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthApi.GenericResponse> call, Throwable t) {
                if (!isAdded())
                    return;
                setLoading(false);
                Toast.makeText(getContext(),
                        getString(R.string.network_error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Ẩn lỗi khi user nhập lại
    private static class SimpleTextWatchers {
        static android.text.TextWatcher hide(View errorView) {
            return new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (errorView != null)
                        errorView.setVisibility(View.GONE);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            };
        }
    }
}