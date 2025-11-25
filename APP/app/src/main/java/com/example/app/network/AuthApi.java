package com.example.app.network;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Retrofit interface khớp NodeJS backend (đã gia cố key mapping & headers). */
public interface AuthApi {

        // ... (Giữ nguyên các DTOs/Classes) ...

        // ========= Health =========
        @GET("health") // đổi thành "api/health" nếu backend của bạn như vậy
        Call<HealthResponse> health();

        class HealthResponse {
                public boolean ok;
                public String version;
        }

        // ========= DTOs =========

        /** /auth/register */
        class RegisterRequest {
                @SerializedName(value = "username", alternate = { "email" })
                public String username; // server dùng username=email

                @SerializedName("password")
                public String password;

                @SerializedName(value = "role", alternate = { "ROLE" })
                public String role; // server ép USER, gửi USER cho rõ

                @SerializedName(value = "name", alternate = { "full_name", "fullName" })
                public String name;

                public RegisterRequest(String username, String password, String role, String name) {
                        this.username = username;
                        this.password = password;
                        this.role = role;
                        this.name = name;
                }
        }

        class RegisterResponse {
                @SerializedName(value = "ok", alternate = { "success" })
                public Boolean ok;

                @SerializedName(value = "otp_sent", alternate = { "otpSent" })
                public Boolean otpSent;

                @SerializedName("user")
                public UserInfo user;

                public static class UserInfo {
                        @SerializedName(value = "id", alternate = { "user_id", "userId" })
                        public Integer id;

                        @SerializedName(value = "username", alternate = { "email" })
                        public String username;

                        @SerializedName(value = "role", alternate = { "ROLE" })
                        public String role;
                }
        }

        /** /auth/send-otp */
        class SendOtpRequest {
                @SerializedName(value = "email", alternate = { "username" })
                public String email;

                public SendOtpRequest(String email) {
                        this.email = email;
                }
        }

        /** Phản hồi “generic” cho nhiều endpoint */
        class GenericResponse {
                @SerializedName(value = "ok", alternate = { "success" })
                public Boolean ok;

                @SerializedName(value = "message", alternate = { "detail", "msg" })
                public String message;

                @SerializedName(value = "error", alternate = { "err", "code" })
                public String error;

                // Backend trả khi 429 (rate-limit)
                @SerializedName(value = "retry_after_seconds", alternate = { "retryAfterSeconds", "retry_after" })
                public Integer retryAfterSeconds;
        }

        /** /auth/verify-otp */
        class VerifyOtpRequest {
                @SerializedName(value = "email", alternate = { "username" })
                public String email;

                @SerializedName(value = "otp", alternate = { "code" })
                public String otp;

                public VerifyOtpRequest(String email, String otp) {
                        this.email = email;
                        this.otp = otp;
                }
        }

        class VerifyOtpResponse {
                @SerializedName(value = "accessToken", alternate = { "access_token", "token" })
                public String accessToken;

                @SerializedName(value = "refreshToken", alternate = { "refresh_token" })
                public String refreshToken;

                @SerializedName(value = "role", alternate = { "ROLE" })
                public String role;

                @SerializedName(value = "message", alternate = { "detail", "msg" })
                public String message;

                @SerializedName("error")
                public String error;
        }

        /** /auth/login */
        class LoginRequest {
                @SerializedName(value = "username", alternate = { "email" })
                public String username;

                @SerializedName("password")
                public String password;

                public LoginRequest(String u, String p) {
                        this.username = u;
                        this.password = p;
                }
        }

        class LoginResponse {
                @SerializedName(value = "accessToken", alternate = { "access_token", "token" })
                public String accessToken;

                @SerializedName(value = "refreshToken", alternate = { "refresh_token" })
                public String refreshToken;

                @SerializedName(value = "role", alternate = { "ROLE" })
                public String role;

                @SerializedName(value = "message", alternate = { "detail", "msg" })
                public String message;

                @SerializedName("error")
                public String error;
        }

        /** /auth/refresh */
        class RefreshRequest {
                @SerializedName(value = "refresh_token", alternate = { "refreshToken" })
                public String refreshToken;

                public RefreshRequest(String t) {
                        this.refreshToken = t;
                }
        }

        class RefreshResponse {
                @SerializedName(value = "accessToken", alternate = { "access_token", "token" })
                public String accessToken;

                @SerializedName(value = "refreshToken", alternate = { "refresh_token" })
                public String refreshToken;

                @SerializedName(value = "message", alternate = { "detail", "msg" })
                public String message;

                @SerializedName("error")
                public String error;
        }

        /** /auth/reset-password */
        class ResetPasswordRequest {
                @SerializedName(value = "email", alternate = { "username" })
                public String email;

                @SerializedName(value = "otp", alternate = { "code" })
                public String otp;

                @SerializedName(value = "new_password", alternate = { "newPassword" })
                public String newPassword;

                public ResetPasswordRequest(String email, String otp, String newPassword) {
                        this.email = email;
                        this.otp = otp;
                        this.newPassword = newPassword;
                }
        }

        class ResetPasswordResponse {
                @SerializedName(value = "ok", alternate = { "success" })
                public Boolean ok;

                @SerializedName(value = "message", alternate = { "detail", "msg" })
                public String message;

                @SerializedName("error")
                public String error;
        }

        /** /dev/last-otp (DEV only) */
        class DevLastOtpResponse {
                @SerializedName(value = "ok", alternate = { "success" })
                public Boolean ok;

                @SerializedName("code")
                public String code; // chỉ hiện khi DEBUG_SHOW_OTP=true

                @SerializedName("note")
                public String note;

                @SerializedName(value = "expires_at", alternate = { "expiresAt" })
                public String expiresAt;

                @SerializedName("used")
                public Boolean used;

                @SerializedName("attempts")
                public Integer attempts;

                @SerializedName("error")
                public String error;
        }

        // ========= Endpoints =========

        // *** Đã loại bỏ dấu "/" đầu tiên để đảm bảo đường dẫn tương đối ***
        @Headers("Accept: application/json")
        @POST("auth/register")
        Call<RegisterResponse> register(@Body RegisterRequest body);

        @Headers("Accept: application/json")
        @POST("auth/send-otp")
        Call<GenericResponse> sendOtp(@Body SendOtpRequest body);

        @Headers("Accept: application/json")
        @POST("auth/verify-otp")
        Call<VerifyOtpResponse> verifyOtp(@Body VerifyOtpRequest body);

        @Headers("Accept: application/json")
        @POST("auth/login")
        Call<LoginResponse> login(@Body LoginRequest body);

        @Headers("Accept: application/json")
        @POST("auth/refresh")
        Call<RefreshResponse> refresh(@Body RefreshRequest body);

        // Backend có thể yêu cầu Authorization Bearer (đã gắn qua Interceptor của
        // OkHttp).
        // X-Refresh-Token là optional để server xóa refresh token cũ khi logout.
        @Headers("Accept: application/json")
        @POST("auth/logout")
        Call<GenericResponse> logout(@Header("X-Refresh-Token") String refreshTokenOptional);

        // Forgot password
        @Headers("Accept: application/json")
        @POST("auth/reset-password")
        Call<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest body);

        // DEV helper (chỉ dùng local/dev)
        @Headers("Accept: application/json")
        @GET("dev/last-otp")
        Call<DevLastOtpResponse> devLastOtp(
                @Query("email") String email,
                @Header("X-Dev-Token") String devTokenOptional);
}