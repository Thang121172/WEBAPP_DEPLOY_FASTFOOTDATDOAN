package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.app.network.AuthApi;
import com.example.app.network.AuthClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpVerifyFragment extends Fragment {

    private static final String TAG = "OtpVerifyFragment";
    private static final int OTP_LENGTH = 6;

    // ===== Bundle args =====
    public static final String ARG_MODE = "mode"; // "register" | "reset"
    public static final String ARG_EMAIL = "email";
    public static final String ARG_INFO_MSG = "info_msg";
    public static final String ARG_PASSWORD = "password"; // optional (đăng nhập tự động sau register)
    public static final String ARG_NAME = "name"; // optional

    private EditText etEmail, etOtpCode;
    private EditText etNewPassword, etConfirmPassword;
    private TextView tvNewPasswordLabel, tvConfirmPasswordLabel;
    private com.google.android.material.textfield.TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextView tvStatus, tvResendHint;
    private Button btnVerify, btnResend;
    private ProgressBar progressBar;

    private AuthClient authClient;

    private String argMode = "register";
    private String argEmail = "";
    private String argPassword = null; // nếu có thì auto-login sau verify (register)
    private String infoMsg = "";

    private CountDownTimer resendTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authClient = new AuthClient(requireContext());

        etEmail = view.findViewById(R.id.et_email);
        etOtpCode = view.findViewById(R.id.et_otp_code);
        tvStatus = view.findViewById(R.id.tv_status);
        tvResendHint = view.findViewById(R.id.tv_resend_hint);
        btnVerify = view.findViewById(R.id.btn_verify);
        btnResend = view.findViewById(R.id.btn_resend);
        progressBar = view.findViewById(R.id.progress_loading);

        // New/Confirm password (đã có trong layout)
        tvNewPasswordLabel = view.findViewById(R.id.tv_new_password_label);
        tilNewPassword = view.findViewById(R.id.til_new_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        tvConfirmPasswordLabel = view.findViewById(R.id.tv_confirm_password_label);
        tilConfirmPassword = view.findViewById(R.id.til_confirm_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);

        // === Read args ===
        Bundle args = getArguments();
        if (args != null) {
            argMode = args.getString(ARG_MODE, "register");
            argEmail = args.getString(ARG_EMAIL, "");
            if (argEmail != null)
                argEmail = argEmail.trim().toLowerCase();
            argPassword = args.getString(ARG_PASSWORD, null);
            infoMsg = args.getString(ARG_INFO_MSG, "");
        }

        // Bind email readonly
        if (!TextUtils.isEmpty(argEmail) && etEmail != null) {
            etEmail.setText(argEmail);
            etEmail.setEnabled(false);
        }
        if (!TextUtils.isEmpty(infoMsg) && tvStatus != null) {
            tvStatus.setText(infoMsg);
        }

        // Hiển thị/hide phần đổi mật khẩu nếu mode=reset
        final boolean isReset = "reset".equalsIgnoreCase(argMode);
        setResetFieldsVisible(isReset);

        // Cấu hình OTP input
        if (etOtpCode != null) {
            etOtpCode.setFilters(new InputFilter[] { new InputFilter.LengthFilter(OTP_LENGTH) });
            etOtpCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    clearStatus();
                    if (!isReset && s != null && s.length() == OTP_LENGTH) {
                        doVerifyOrReset();
                    }
                }
            });
        }

        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                if (!btnVerify.isEnabled())
                    return;
                doVerifyOrReset();
            });
        }

        // Resend OTP
        if (btnResend != null) {
            btnResend.setEnabled(true);
            btnResend.setAlpha(1f);
            btnResend.setOnClickListener(v -> {
                if (!btnResend.isEnabled())
                    return;
                doResend(isReset);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resendTimer != null)
            resendTimer.cancel();
    }

    // =========================
    // Main action: Verify or Reset
    // =========================
    private void doVerifyOrReset() {
        final String email = argEmail;
        final String otp = getTrim(etOtpCode);

        if (TextUtils.isEmpty(email)) {
            showStatus(getString(R.string.otp_need_email));
            return;
        }
        if (TextUtils.isEmpty(otp) || otp.length() != OTP_LENGTH) {
            showStatus(getString(R.string.otp_need_code));
            return;
        }

        final boolean isReset = "reset".equalsIgnoreCase(argMode);
        if (isReset) {
            final String newPwd = getTrim(etNewPassword);
            final String confirm = getTrim(etConfirmPassword);

            if (TextUtils.isEmpty(newPwd) || TextUtils.isEmpty(confirm)) {
                showStatus(getString(R.string.err_fill_reset_fields));
                return;
            }
            if (!newPwd.equals(confirm)) {
                showStatus(getString(R.string.err_password_mismatch));
                return;
            }
            if (newPwd.length() < 6) {
                showStatus(getString(R.string.otp_need_password));
                return;
            }
            if (newPwd.matches("^\\d+$")) {
                showStatus(getString(R.string.err_not_all_digits));
                return;
            }

            setLoading(true);
            authClient.resetPassword(email, otp, newPwd, new Callback<AuthApi.ResetPasswordResponse>() {
                @Override
                public void onResponse(Call<AuthApi.ResetPasswordResponse> call,
                                       Response<AuthApi.ResetPasswordResponse> response) {
                    if (!isAdded())
                        return;
                    setLoading(false);

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), getString(R.string.reset_success), Toast.LENGTH_SHORT).show();
                        goLogin();
                    } else {
                        String err = getString(R.string.err_cannot_reset_password);
                        try {
                            if (response.errorBody() != null) {
                                String raw = response.errorBody().string();
                                if (!TextUtils.isEmpty(raw))
                                    err = raw;
                            }
                        } catch (Exception ignored) {
                        }
                        showStatus(err);
                    }
                }

                @Override
                public void onFailure(Call<AuthApi.ResetPasswordResponse> call, Throwable t) {
                    if (!isAdded())
                        return;
                    setLoading(false);
                    showStatus(getString(R.string.err_connection_friendly));
                }
            });

        } else {
            // VERIFY (REGISTER) MODE
            setLoading(true);
            authClient.verifyOtp(email, otp, new Callback<AuthApi.VerifyOtpResponse>() {
                @Override
                public void onResponse(Call<AuthApi.VerifyOtpResponse> call,
                                       Response<AuthApi.VerifyOtpResponse> response) {
                    if (!isAdded())
                        return;
                    setLoading(false);

                    if (response.isSuccessful() && response.body() != null &&
                            !TextUtils.isEmpty(response.body().accessToken)) {

                        // ✅ FIX: LƯU ACCESS TOKEN VÀ ROLE SAU KHI VERIFY THÀNH CÔNG
                        String accessToken = response.body().accessToken;
                        String role = response.body().role; // Backend trả về role từ database
                        Log.d("OtpVerifyFragment", "Verify OTP success, role from backend: " + role);
                        
                        // Đảm bảo role không null hoặc rỗng
                        if (TextUtils.isEmpty(role)) {
                            Log.w("OtpVerifyFragment", "Role is empty from backend, defaulting to USER");
                            role = "USER";
                        }
                        
                        authClient.saveAuthData(accessToken, role);
                        Log.d("OtpVerifyFragment", "Saved auth data with role: " + role);

                        Toast.makeText(getContext(), getString(R.string.verify_success), Toast.LENGTH_SHORT).show();
                        launchHomeActivity(); // FIX 2: SỬ DỤNG Intent để chuyển Activity và xóa back stack
                    } else {
                        // verify OK nhưng không trả token → thử login nếu có password
                        if (response.isSuccessful() && response.body() != null &&
                                TextUtils.isEmpty(response.body().error)) {
                            proceedAfterVerifyFallback();
                        } else {
                            String err = getString(R.string.otp_confirm_fail);
                            try {
                                if (response.errorBody() != null) {
                                    String raw = response.errorBody().string();
                                    if (!TextUtils.isEmpty(raw))
                                        err = raw;
                                }
                            } catch (Exception ignored) {
                            }
                            showStatus(err);
                        }
                    }
                }

                @Override
                public void onFailure(Call<AuthApi.VerifyOtpResponse> call, Throwable t) {
                    if (!isAdded())
                        return;
                    setLoading(false);
                    showStatus(getString(R.string.otp_network_error_confirm,
                            t.getMessage() != null ? t.getMessage() : "?"));
                }
            });
        }
    }

    /**
     * Fallback khi verify-otp không trả token:
     * - nếu có password từ Register -> login(email, password)
     * - nếu không -> quay về Login
     */
    private void proceedAfterVerifyFallback() {
        if (!TextUtils.isEmpty(argEmail) && !TextUtils.isEmpty(argPassword)) {
            setLoading(true);
            authClient.login(argEmail, argPassword, new Callback<AuthApi.LoginResponse>() {
                @Override
                public void onResponse(Call<AuthApi.LoginResponse> call, Response<AuthApi.LoginResponse> resp) {
                    if (!isAdded())
                        return;
                    setLoading(false);

                    if (resp.isSuccessful() && resp.body() != null &&
                            !TextUtils.isEmpty(resp.body().accessToken)) {

                        // FIX 1B: LƯU ACCESS TOKEN VÀ ROLE SAU KHI FALLBACK LOGIN THÀNH CÔNG
                        String accessToken = resp.body().accessToken;
                        String role = resp.body().role; // Giả định resp.body().role có
                        authClient.saveAuthData(accessToken, role);

                        Toast.makeText(getContext(), getString(R.string.otp_verify_success), Toast.LENGTH_SHORT).show();
                        launchHomeActivity(); // FIX 2B: SỬ DỤNG Intent để chuyển Activity và xóa back stack
                    } else {
                        goLogin();
                    }
                }

                @Override
                public void onFailure(Call<AuthApi.LoginResponse> call, Throwable t) {
                    if (!isAdded())
                        return;
                    setLoading(false);
                    goLogin();
                }
            });
        } else {
            goLogin();
        }
    }

    // =========================
    // RESEND OTP
    // =========================
    private void doResend(boolean isReset) {
        if (TextUtils.isEmpty(argEmail)) {
            showStatus(getString(R.string.otp_need_email));
            return;
        }
        setLoading(true);

        Callback<AuthApi.GenericResponse> cb = new Callback<AuthApi.GenericResponse>() {
            @Override
            public void onResponse(Call<AuthApi.GenericResponse> call, Response<AuthApi.GenericResponse> resp) {
                if (!isAdded())
                    return;
                setLoading(false);

                if (resp.isSuccessful()) {
                    Toast.makeText(getContext(),
                            getString(R.string.otp_sent_check_email_or_logs),
                            Toast.LENGTH_SHORT).show();

                    int windowSec = parseIntSafe(resp.headers().get("X-OTP-Window-Seconds"), 0);
                    int remaining = parseIntSafe(resp.headers().get("X-OTP-Remaining"), -1);
                    int limit = parseIntSafe(resp.headers().get("X-OTP-Limit"), -1);
                    if (windowSec <= 0)
                        windowSec = parseIntSafe(resp.headers().get("Retry-After"), 0);

                    if (remaining >= 0 && limit >= 0 && tvResendHint != null) {
                        tvResendHint.setText(getString(R.string.otp_quota_hint, remaining, limit));
                        tvResendHint.setVisibility(View.VISIBLE);
                    }

                    if (windowSec > 0)
                        startResendCountdown(windowSec);
                } else {
                    String err = getString(R.string.err_cannot_send_otp);
                    try {
                        if (resp.errorBody() != null) {
                            String raw = resp.errorBody().string();
                            if (!TextUtils.isEmpty(raw))
                                err = raw;
                        }
                    } catch (Exception ignored) {
                    }
                    showStatus(err);
                }
            }

            @Override
            public void onFailure(Call<AuthApi.GenericResponse> call, Throwable t) {
                if (!isAdded())
                    return;
                setLoading(false);
                showStatus(getString(R.string.err_connection_friendly));
            }
        };

        if (isReset) {
            authClient.requestResetOtp(argEmail, cb);
        } else {
            authClient.requestRegisterOtp(argEmail, cb);
        }
    }

    private void startResendCountdown(int seconds) {
        if (btnResend == null)
            return;
        btnResend.setEnabled(false);
        btnResend.setAlpha(0.6f);

        if (resendTimer != null)
            resendTimer.cancel();
        resendTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (btnResend != null)
                    btnResend.setText(getString(R.string.otp_resend_countdown, millisUntilFinished / 1000L));
            }

            @Override
            public void onFinish() {
                if (btnResend != null) {
                    btnResend.setEnabled(true);
                    btnResend.setAlpha(1f);
                    btnResend.setText(getString(R.string.otp_resend));
                }
            }
        }.start();
    }

    // =========================
    // Helpers
    // =========================
    private void setResetFieldsVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        // ✅ FIX: Set visibility cho cả TextInputLayout (cha) và EditText (con)
        if (tvNewPasswordLabel != null)
            tvNewPasswordLabel.setVisibility(v);
        if (tilNewPassword != null)
            tilNewPassword.setVisibility(v);
        if (etNewPassword != null)
            etNewPassword.setVisibility(v);
        if (tvConfirmPasswordLabel != null)
            tvConfirmPasswordLabel.setVisibility(v);
        if (tilConfirmPassword != null)
            tilConfirmPassword.setVisibility(v);
        if (etConfirmPassword != null)
            etConfirmPassword.setVisibility(v);
    }

    private void setLoading(boolean loading) {
        if (btnVerify != null)
            btnVerify.setEnabled(!loading);
        if (btnResend != null)
            btnResend.setEnabled(!loading);
        if (etOtpCode != null)
            etOtpCode.setEnabled(!loading);
        if (etNewPassword != null)
            etNewPassword.setEnabled(!loading);
        if (etConfirmPassword != null)
            etConfirmPassword.setEnabled(!loading);
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String msg) {
        if (tvStatus != null)
            tvStatus.setText(msg);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void clearStatus() {
        if (tvStatus != null)
            tvStatus.setText("");
    }

    private void goLogin() {
        try {
            NavHostFragment.findNavController(OtpVerifyFragment.this)
                    .navigate(R.id.action_otpVerifyFragment_to_loginFragment);
        } catch (Exception e) {
            Log.e(TAG, "Nav to Login failed: " + e.getMessage());
        }
    }

    /**
     * [ĐÃ SỬA] Thay thế goHomeByRole() bằng cách khởi chạy MainActivity.
     * Đây là cách chuẩn để chuyển từ màn hình Auth sang màn hình chính và xóa back stack.
     */
    private void launchHomeActivity() {
        if (getContext() == null) return;

        // Intent để khởi chạy MainActivity
        Intent intent = new Intent(getContext(), MainActivity.class);

        // Các cờ quan trọng để xóa back stack, ngăn không cho user quay lại màn hình Auth.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc AuthActivity hiện tại
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private static String getTrim(EditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString().trim();
    }

    private static int parseIntSafe(String s, int def) {
        if (TextUtils.isEmpty(s))
            return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}