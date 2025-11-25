package com.example.app.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * AuthService — Retrofit interface mỏng, tái dùng models từ AuthApi.
 *
 * Lưu ý:
 * - KHÔNG dùng dấu "/" đầu path vì baseUrl đã có "/" ở cuối.
 * - Chỉ giữ các endpoint đang có ở backend: register, verify-otp, health.
 * - Loại bỏ login (chưa có trên backend hiện tại).
 */
public interface AuthService {

    // POST /auth/register
    Call<AuthApi.RegisterResponse> register(@Body AuthApi.RegisterRequest body);

    // POST /auth/verify-otp
    @POST("auth/verify-otp")
    Call<AuthApi.VerifyOtpResponse> verifyOtp(@Body AuthApi.VerifyOtpRequest body);

    // GET /health (ping DB)
    @GET("health")
    Call<AuthApi.HealthResponse> health();

    // Đặt @POST cho /auth/register (khai báo tường minh sau cùng để dễ nhìn)
    @POST("auth/register")
    Call<AuthApi.RegisterResponse> _register(@Body AuthApi.RegisterRequest body);
}
