package com.example.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.app.network.AuthApi;
import com.example.app.network.AuthClient;
import com.google.gson.Gson; // Thêm import thư viện Gson để parse lỗi
import com.google.gson.JsonSyntaxException;

import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RegisterFragment (OTP register flow – Node backend)
 *
 * B1: Nhập Họ tên + Email + SĐT + Mật khẩu
 * B2: Gọi /auth/register -> backend tạo user & tự gửi OTP
 * B3: Điều hướng sang OtpVerifyFragment (mode="register", email, info_msg)
 */
public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    // Keys dùng chung với OtpVerifyFragment
    public static final String ARG_MODE = "mode"; // "register" | "reset"
    public static final String ARG_EMAIL = "email";
    public static final String ARG_INFO_MSG = "info_msg";
    public static final String ARG_PASSWORD = "password"; // optional
    public static final String ARG_NAME = "name"; // optional

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPassword;
    private Spinner spRole;
    private Button btnSendOtp;
    private TextView tvError;
    private TextView tvInfo;
    private TextView tvToLogin;
    private ProgressBar progressBar;

    private AuthClient authClient;

    // Regex: mật khẩu không được toàn số
    private static final Pattern ALL_DIGITS = Pattern.compile("^\\d+$");

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authClient = new AuthClient(requireContext());

        etFullName = view.findViewById(R.id.et_full_name);
        etEmail = view.findViewById(R.id.et_email);
        etPhone = view.findViewById(R.id.et_phone);
        etPassword = view.findViewById(R.id.et_password);
        spRole = view.findViewById(R.id.sp_role);
        btnSendOtp = view.findViewById(R.id.btn_send_otp);
        tvError = view.findViewById(R.id.tv_error);
        tvInfo = view.findViewById(R.id.tv_info);
        tvToLogin = view.findViewById(R.id.tv_to_login);
        progressBar = view.findViewById(R.id.progress_loading);

        setupRoleSpinner();

        if (btnSendOtp != null) {
            btnSendOtp.setOnClickListener(v -> {
                if (!btnSendOtp.isEnabled())
                    return; // chống double tap
                hideMessages();

                final String fullNameInput = getTrim(etFullName);
                final String emailInput = getLower(etEmail);
                final String phoneInput = getTrim(etPhone);
                final String passInput = getTrim(etPassword);
                final String roleInput = getRoleFromSpinner(); // chỉ dùng UI

                // ===== Validation =====
                if (TextUtils.isEmpty(fullNameInput) ||
                        TextUtils.isEmpty(emailInput) ||
                        TextUtils.isEmpty(phoneInput) ||
                        TextUtils.isEmpty(passInput)) {
                    showError(getString(R.string.err_fill_fields));
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    showError(getString(R.string.err_invalid_email));
                    return;
                }
                // Theo rule hiện tại
                if (!emailInput.endsWith("@gmail.com")) {
                    showError(getString(R.string.err_use_gmail));
                    return;
                }

                if (phoneInput.length() < 8) {
                    showError(getString(R.string.err_invalid_phone));
                    return;
                }

                if (passInput.length() < 6) {
                    showError(getString(R.string.otp_need_password));
                    return;
                }
                if (ALL_DIGITS.matcher(passInput).matches()) {
                    showError(getString(R.string.err_not_all_digits));
                    return;
                }

                setLoading(true);

                // ===== GỌI /auth/register (Node) — đúng thứ tự tham số =====
                // ✅ FIX: Truyền role từ spinner vào register
                String roleToRegister = getRoleFromSpinner();
                authClient.registerWithRole(
                        emailInput, // email/username
                        passInput, // password
                        fullNameInput, // full name
                        roleToRegister, // role từ spinner
                        new Callback<AuthApi.RegisterResponse>() {
                            @Override
                            public void onResponse(
                                    Call<AuthApi.RegisterResponse> call,
                                    Response<AuthApi.RegisterResponse> response) {
                                if (!isAdded())
                                    return;
                                setLoading(false);

                                // TRƯỜNG HỢP 1: Thành công (200 OK)
                                if (response.isSuccessful() && response.body() != null) {
                                    // Bắt buộc phải có ok = true hoặc otpSent = true
                                    if (Boolean.TRUE.equals(response.body().ok) || Boolean.TRUE.equals(response.body().otpSent)) {
                                        // server thường trả ok + otp_sent
                                        String info = getString(R.string.otp_sent_check_email_or_logs);
                                        Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();
                                        // sang OTP
                                        navigateToOtp(emailInput, info, passInput, fullNameInput);
                                        return;
                                    }
                                }

                                // TRƯỜNG HỢP 2: Lỗi (4xx, 5xx, hoặc 200 OK nhưng body là lỗi)
                                String errorMessage = parseError(response.errorBody());

                                // Nếu không parse được, kiểm tra mã HTTP
                                if (TextUtils.isEmpty(errorMessage)) {
                                    if (response.code() == 409) { // 409 Conflict thường là trùng email
                                        errorMessage = "Email này đã được đăng ký. Vui lòng đăng nhập.";
                                    } else if (response.code() >= 400 && response.code() < 500) {
                                        errorMessage = getString(R.string.err_invalid_request_friendly) + " (" + response.code() + ")";
                                    } else if (response.code() >= 500) {
                                        errorMessage = getString(R.string.err_server_down_friendly) + " (" + response.code() + ")";
                                    } else {
                                        // Lỗi chung (ví dụ: response.isSuccessful() là true nhưng body rỗng hoặc logic lỗi)
                                        errorMessage = getString(R.string.err_could_not_create_user_friendly);
                                    }
                                }
                                showError(errorMessage);
                            }

                            @Override
                            public void onFailure(Call<AuthApi.RegisterResponse> call, Throwable t) {
                                if (!isAdded())
                                    return;
                                setLoading(false);
                                String m = (t.getMessage() != null && !t.getMessage().contains("Canceled"))
                                        ? t.getMessage()
                                        : getString(R.string.err_connection_friendly);
                                showError(m);
                            }
                        });
            });
        }

        if (tvToLogin != null) {
            tvToLogin.setOnClickListener(v -> {
                try {
                    NavController nav = NavHostFragment.findNavController(RegisterFragment.this);
                    nav.navigate(R.id.action_registerFragment_to_loginFragment);
                } catch (Exception e) {
                    Log.e(TAG, "Nav to login failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Lỗi điều hướng.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String parseError(ResponseBody errorBody) {
        if (errorBody == null) {
            return null;
        }
        try {
            // Lấy chuỗi JSON lỗi
            String rawJson = errorBody.string();
            // Reset lại stream để có thể đọc lại lần nữa nếu cần
            // (Tuy nhiên, errorBody.string() đã đóng stream, nên cần gọi response.errorBody().charStream() nếu muốn an toàn,
            // nhưng cách này đơn giản hơn cho mục đích parse)

            // Phân tích thành đối tượng GenericResponse
            Gson gson = new Gson();
            AuthApi.GenericResponse errorResponse = gson.fromJson(rawJson, AuthApi.GenericResponse.class);

            if (errorResponse != null) {
                // Ưu tiên trường message
                if (!TextUtils.isEmpty(errorResponse.message)) {
                    return errorResponse.message;
                }
                // Sau đó là trường error
                if (!TextUtils.isEmpty(errorResponse.error)) {
                    return errorResponse.error;
                }
            }
            // Nếu không có trường nào, trả về chuỗi raw để debug
            return rawJson;

        } catch (JsonSyntaxException e) {
            // Lỗi khi JSON không hợp lệ
            Log.e(TAG, "Error parsing error body JSON: " + e.getMessage());
        } catch (Exception e) {
            // Lỗi I/O khi đọc body
            Log.e(TAG, "Error reading error body: " + e.getMessage());
        }
        return null;
    }


    private void setupRoleSpinner() {
        if (spRole == null)
            return;
        // NOTE: Bạn đang không dùng 'roleInput' trong authClient.register, hãy đảm bảo
        // authClient có hàm overload nhận cả role
        String[] roles = new String[] { "customer", "merchant", "shipper" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roles);
        spRole.setAdapter(adapter);
        spRole.setSelection(0);
    }

    private String getRoleFromSpinner() {
        if (spRole == null || spRole.getSelectedItem() == null)
            return "customer";
        return String.valueOf(spRole.getSelectedItem()).trim().toLowerCase();
    }

    private static String getTrim(EditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static String getLower(EditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim().toLowerCase();
    }

    private void setLoading(boolean loading) {
        if (btnSendOtp != null)
            btnSendOtp.setEnabled(!loading);
        if (tvToLogin != null)
            tvToLogin.setEnabled(!loading);
        if (etFullName != null)
            etFullName.setEnabled(!loading);
        if (etEmail != null)
            etEmail.setEnabled(!loading);
        if (etPhone != null)
            etPhone.setEnabled(!loading);
        if (etPassword != null)
            etPassword.setEnabled(!loading);
        if (spRole != null)
            spRole.setEnabled(!loading);
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        if (tvError != null) {
            tvError.setText(msg);
            tvError.setVisibility(View.VISIBLE);
        }
        if (tvInfo != null)
            tvInfo.setVisibility(View.GONE);
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void hideMessages() {
        if (tvError != null)
            tvError.setVisibility(View.GONE);
        if (tvInfo != null)
            tvInfo.setVisibility(View.GONE);
    }

    private void navigateToOtp(String emailInput, String infoMsg, String password, String fullName) {
        try {
            Bundle b = new Bundle();
            b.putString(ARG_MODE, "register");
            b.putString(ARG_EMAIL, emailInput);
            if (!TextUtils.isEmpty(infoMsg))
                b.putString(ARG_INFO_MSG, infoMsg);
            if (!TextUtils.isEmpty(password))
                b.putString(ARG_PASSWORD, password);
            if (!TextUtils.isEmpty(fullName))
                b.putString(ARG_NAME, fullName);

            NavController nav = NavHostFragment.findNavController(RegisterFragment.this);
            nav.navigate(R.id.action_registerFragment_to_otpVerifyFragment, b);
        } catch (Exception e) {
            Log.e(TAG, "Nav error: " + e.getMessage());
            Toast.makeText(getContext(), "Lỗi điều hướng.", Toast.LENGTH_SHORT).show();
        }
    }
}