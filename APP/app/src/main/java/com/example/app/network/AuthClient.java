package com.example.app.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.app.auth.AuthEvents;
import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * AuthClient — client cho Node backend (index.js)
 *
 * Endpoints dùng trong class:
 * - POST /auth/send-otp { email }
 * - POST /auth/verify-otp { email, otp }
 * - POST /auth/register { username,password,role,name }
 * - POST /auth/login { username,password }
 * - POST /auth/refresh { refresh_token }
 * - POST /auth/logout (Authorization + optional X-Refresh-Token)
 * - POST /auth/reset-password { email, otp, new_password }
 * - GET /dev/last-otp?email=... (DEV only)
 */
public class AuthClient {

    private static final String TAG = "AuthClient";

    private static final String PREFS = "auth_session";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";

    private final Context appContext;
    private final Retrofit retrofit;
    private final AuthApi api;
    private final SharedPreferences prefs;

    public AuthClient(Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.retrofit = BackendConfig.getRetrofit(appContext);
        this.api = retrofit.create(AuthApi.class);
        this.prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Backward-compat: giữ nguyên cách gọi cũ new AuthClient(ctx).getRetrofit() */
    public Retrofit getRetrofit() {
        return retrofit;
    }

    /** Khuyến nghị: dùng trực tiếp interface */
    public AuthApi api() {
        return api;
    }

    // =========================
    // Mô hình phản hồi chung
    // =========================

    /**
     * Mô hình phản hồi chung từ API, chứa thông tin trạng thái và message.
     */
    public static class GenericResponse {
        @SerializedName("status")
        public int status; // Ví dụ: 200, 400

        @SerializedName("message")
        public String message; // Thông báo từ server

        // Các phương thức tiện ích để kiểm tra trạng thái
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }


    // =========================
    // Session helpers
    // =========================

    private static String normEmail(@Nullable String email) {
        if (email == null)
            return "";
        return email.trim().toLowerCase();
    }

    // Hàm này đảm bảo giá trị chuỗi rỗng vẫn được lưu, chứ không bị remove khỏi SharedPreferences.
    private static String normString(String s) {
        return s == null ? "" : s.trim();
    }

    /** Lưu phiên (đồng bộ access -> BackendConfig để Interceptor gắn header) */
    public void saveSession(@Nullable String access,
                            @Nullable String refresh,
                            @Nullable String role,
                            @Nullable String email) {

        // 1. Xử lý Access Token (lưu vào BackendConfig)
        if (!TextUtils.isEmpty(access)) {
            BackendConfig.saveAccessToken(appContext, access);
        } else {
            BackendConfig.clearAccessToken(appContext);
        }

        // 2. Xử lý các token khác (lưu vào SharedPreferences)
        SharedPreferences.Editor ed = prefs.edit();

        // [ĐÃ CHỈNH SỬA] Luôn lưu giá trị (kể cả chuỗi rỗng) để tránh mất key
        ed.putString(KEY_REFRESH, normString(refresh));
        ed.putString(KEY_ROLE, normString(role));
        ed.putString(KEY_EMAIL, normString(email));

        ed.apply();

        if (!TextUtils.isEmpty(role))
            Log.d(TAG, "Saved session, role=" + role);
    }

    /** * [QUAN TRỌNG] Lưu Access Token và Role. Giữ lại Refresh Token và Email hiện tại.
     * Sử dụng cho trường hợp OtpVerifyFragment hoặc Refresh Token thành công.
     */
    public void saveAuthData(String accessToken, @Nullable String role) {
        // Lấy thông tin session hiện tại để giữ lại refresh token và email
        String currentRefreshToken = getRefreshToken();
        String currentEmail = getEmail();
        // Lấy role hiện tại. Nếu role mới có giá trị, sử dụng role mới.
        String newRole = normString(role);
        if(TextUtils.isEmpty(newRole)) {
            newRole = getRole(); // Giữ role cũ nếu role mới rỗng/null
        }

        // Gọi saveSession để cập nhật Access Token và Role mới
        saveSession(accessToken, currentRefreshToken, newRole, currentEmail);
        Log.d(TAG, "saveAuthData called. Access Token updated. Role: " + (newRole != null ? newRole : "N/A"));
    }

    /** Xóa toàn bộ dữ liệu phiên (Access, Refresh, Role, Email) */
    public void clearSession() {
        BackendConfig.clearAccessToken(appContext);
        // [ĐÃ CHỈNH SỬA] Dùng putString("") thay vì remove để giữ key tránh lỗi khi get
        prefs.edit()
                .putString(KEY_REFRESH, "")
                .putString(KEY_ROLE, "")
                .putString(KEY_EMAIL, "")
                .apply();

        // Giả định BackendConfig.resetRetrofit() có tồn tại
        BackendConfig.resetRetrofit();
        sendAuthCleared("manual_clear");
    }

    /** Broadcast cho UI (điều hướng/ẩn nút…) khi phiên bị xoá */
    private void sendAuthCleared(@Nullable String reason) {
        Intent i = new Intent(AuthEvents.ACTION_AUTH_SESSION_CLEARED);
        if (!TextUtils.isEmpty(reason))
            i.putExtra(AuthEvents.EXTRA_REASON, reason);
        appContext.sendBroadcast(i);
    }

    @Nullable
    public String getAccessToken() {
        // Giả định BackendConfig.getAccessToken(appContext) tồn tại
        return BackendConfig.getAccessToken(appContext);
    }

    @Nullable
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH, null);
    }

    @Nullable
    public String getRole() {
        // [ĐÃ CHỈNH SỬA] Trả về chuỗi rỗng "" nếu giá trị là null (hoặc không tìm thấy)
        return prefs.getString(KEY_ROLE, "");
    }

    @Nullable
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    /** Kiểm tra tính hợp lệ của phiên.
     * Phiên được coi là đăng nhập nếu có Access Token VÀ có Refresh Token.
     */
    public boolean isSignedIn() {
        return !TextUtils.isEmpty(getAccessToken()) && !TextUtils.isEmpty(getRefreshToken());
    }

    // =================================
    // 1) Gửi OTP
    // =================================
    public void sendOtp(String email, Callback<AuthApi.GenericResponse> cb) {
        api.sendOtp(new AuthApi.SendOtpRequest(normEmail(email))).enqueue(safe(cb));
    }

    public void requestRegisterOtp(String email, Callback<AuthApi.GenericResponse> cb) {
        sendOtp(email, cb);
    }

    // =================================
    // 2) Xác minh OTP
    // =================================
    public void verifyOtp(String email, String otp, Callback<AuthApi.VerifyOtpResponse> cb) {
        final String em = normEmail(email);
        if (TextUtils.isEmpty(em) || TextUtils.isEmpty(otp)) {
            if (cb != null)
                cb.onFailure(null, new Throwable("missing_email_or_otp"));
            return;
        }
        api.verifyOtp(new AuthApi.VerifyOtpRequest(em, otp)).enqueue(new Callback<AuthApi.VerifyOtpResponse>() {
            @Override
            public void onResponse(Call<AuthApi.VerifyOtpResponse> call, Response<AuthApi.VerifyOtpResponse> res) {
                if (res.isSuccessful() && res.body() != null && !TextUtils.isEmpty(res.body().accessToken)) {
                    AuthApi.VerifyOtpResponse body = res.body();
                    // Đảm bảo lưu đầy đủ: access, refresh, role và email
                    saveSession(body.accessToken, body.refreshToken, body.role, em);
                    Log.d(TAG, "verify-otp success, role=" + body.role);
                } else {
                    Log.w(TAG, "verify-otp failed http=" + res.code());
                }
                if (cb != null)
                    cb.onResponse(call, res);
            }

            @Override
            public void onFailure(Call<AuthApi.VerifyOtpResponse> call, Throwable t) {
                if (cb != null)
                    cb.onFailure(call, t);
            }
        });
    }

    // =================================
    // 3) Đăng ký
    // =================================
    public void register(String fullName, String email, @Nullable String phone, String password,
                         Callback<AuthApi.RegisterResponse> cb) {
        registerWithRole(email, password, fullName, "USER", cb);
    }

    public void register(String email, String password, @Nullable String fullName,
                         Callback<AuthApi.RegisterResponse> cb) {
        registerWithRole(email, password, fullName, "USER", cb);
    }
    
    // ✅ FIX: Method register với role parameter
    public void registerWithRole(String email, String password, @Nullable String fullName, String role,
                         Callback<AuthApi.RegisterResponse> cb) {
        String finalRole = (role != null && !role.trim().isEmpty()) ? role.toUpperCase() : "USER";
        AuthApi.RegisterRequest body = new AuthApi.RegisterRequest(
                normEmail(email), password, finalRole, fullName == null ? "" : fullName.trim());
        api.register(body).enqueue(safe(cb));
    }

    // =================================
    // 4) Đăng nhập & Refresh
    // =================================
    public void login(String email, String password, Callback<AuthApi.LoginResponse> cb) {
        api.login(new AuthApi.LoginRequest(normEmail(email), password))
                .enqueue(new Callback<AuthApi.LoginResponse>() {
                    @Override
                    public void onResponse(Call<AuthApi.LoginResponse> call, Response<AuthApi.LoginResponse> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            AuthApi.LoginResponse body = res.body();

                            // [ĐÃ CHỈNH SỬA] Đảm bảo role không phải null hoặc rỗng khi lưu. Mặc định là 'customer'
                            String finalRole = TextUtils.isEmpty(body.role) ? "customer" : body.role;

                            // Đảm bảo gọi saveSession với Access Token, Refresh Token, Role và Email
                            saveSession(
                                    body.accessToken,
                                    body.refreshToken,
                                    finalRole, // <-- Sử dụng giá trị đã kiểm tra
                                    normEmail(email));

                            // Log sau khi lưu session để kiểm tra tính hợp lệ
                            if (!TextUtils.isEmpty(getAccessToken())) {
                                Log.d(TAG, "login success, role=" + finalRole);
                            } else {
                                Log.w(TAG, "login success but no access token provided in response.");
                            }
                        } else {
                            Log.w(TAG, "login failed http=" + res.code());
                        }
                        if (cb != null)
                            cb.onResponse(call, res);
                    }

                    @Override
                    public void onFailure(Call<AuthApi.LoginResponse> call, Throwable t) {
                        if (cb != null)
                            cb.onFailure(call, t);
                    }
                });
    }

    public void refresh(Callback<AuthApi.RefreshResponse> cb) {
        final String rt = getRefreshToken();
        if (TextUtils.isEmpty(rt)) {
            // Nếu không có Refresh Token, phiên không thể làm mới, buộc clear và thông báo
            clearSession();
            sendAuthCleared("refresh_fail_no_token");
            if (cb != null)
                cb.onFailure(null, new Throwable("no_refresh_token"));
            return;
        }
        api.refresh(new AuthApi.RefreshRequest(rt)).enqueue(new Callback<AuthApi.RefreshResponse>() {
            @Override
            public void onResponse(Call<AuthApi.RefreshResponse> call, Response<AuthApi.RefreshResponse> res) {
                if (res.isSuccessful() && res.body() != null && !TextUtils.isEmpty(res.body().accessToken)) {
                    AuthApi.RefreshResponse body = res.body();

                    // Cập nhật Access Token MỚI, Refresh Token MỚI (nếu có),
                    // Giữ lại Role và Email CŨ (getRole(), getEmail()).
                    String newRefreshToken = body.refreshToken != null ? body.refreshToken : getRefreshToken();

                    saveSession(body.accessToken, newRefreshToken, getRole(), getEmail());

                } else {
                    // Nếu Refresh thất bại (token hết hạn, bị thu hồi, v.v.)
                    Log.w(TAG, "refresh failed http=" + res.code() + ", clearing session.");
                    clearSession();
                    sendAuthCleared("refresh_fail_api");
                }
                if (cb != null)
                    cb.onResponse(call, res);
            }

            @Override
            public void onFailure(Call<AuthApi.RefreshResponse> call, Throwable t) {
                // Lỗi mạng hoặc lỗi hệ thống khi refresh
                Log.e(TAG, "refresh failed network error, clearing session.", t);
                clearSession();
                sendAuthCleared("refresh_fail_network");
                if (cb != null)
                    cb.onFailure(call, t);
            }
        });
    }

    // =================================
    // 5) Logout
    // =================================
    public void logout(@Nullable String refreshToken, @Nullable Callback<AuthApi.GenericResponse> cb) {
        api.logout(refreshToken).enqueue(new Callback<AuthApi.GenericResponse>() {
            @Override
            public void onResponse(Call<AuthApi.GenericResponse> call, Response<AuthApi.GenericResponse> res) {
                // Bất kể API trả về gì, cứ clear session và thông báo cho UI
                clearSession();
                sendAuthCleared("logout_success");
                if (cb != null)
                    cb.onResponse(call, res);
            }

            @Override
            public void onFailure(Call<AuthApi.GenericResponse> call, Throwable t) {
                // Ngay cả khi lỗi mạng khi gọi logout, vẫn phải clear session cục bộ
                clearSession();
                sendAuthCleared("logout_failure");
                if (cb != null)
                    cb.onFailure(call, t);
            }
        });
    }

    public void logout() {
        logout(getRefreshToken(), null);
    }

    // =================================
    // 6) Reset password bằng OTP
    // =================================
    public void requestResetOtp(String email, Callback<AuthApi.GenericResponse> cb) {
        sendOtp(email, cb);
    }

    public void resetPassword(String email, String otp, String newPassword,
                              Callback<AuthApi.ResetPasswordResponse> cb) {
        AuthApi.ResetPasswordRequest body = new AuthApi.ResetPasswordRequest(normEmail(email), otp, newPassword);
        api.resetPassword(body).enqueue(safe(cb));
    }

    // =================================
    // 7) DEV helper
    // =================================
    public void devLastOtp(String email, @Nullable String devToken, Callback<AuthApi.DevLastOtpResponse> cb) {
        api.devLastOtp(normEmail(email), devToken).enqueue(safe(cb));
    }

    // ========= Utils =========
    private static <T> Callback<T> safe(Callback<T> cb) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (cb != null)
                    cb.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (cb != null)
                    cb.onFailure(call, t);
            }
        };
    }
}