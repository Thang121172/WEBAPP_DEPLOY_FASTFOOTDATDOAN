package com.example.app.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * BackendConfig
 *
 * - Quản lý BASE_URL và cung cấp Retrofit/OkHttp dùng chung
 * - Tự attach "Authorization: Bearer <token>" nếu đã đăng nhập
 * - Tự động silent refresh khi gặp 401 bằng OkHttp Authenticator (retry 1 lần)
 * - Broadcast cho UI khi session bị clear
 */
public final class BackendConfig {

    private BackendConfig() {
    }

    // ============================
    // Fake BuildConfig (tự định nghĩa)
    // ============================
    // Bật/tắt log HTTP
    private static final boolean DEBUG = true;
    // Chỉ để gắn User-Agent, không quan trọng
    private static final String VERSION_NAME = "1.0";

    // ============================
    // Broadcast cho UI khi phiên bị xoá
    // ============================
    public static final String ACTION_AUTH_SESSION_CLEARED = "com.example.app.ACTION_AUTH_SESSION_CLEARED";
    public static final String EXTRA_REASON = "reason";

    private static void sendAuthClearedBroadcast(Context context, String reason) {
        try {
            Intent i = new Intent(ACTION_AUTH_SESSION_CLEARED);
            i.putExtra(EXTRA_REASON, reason);
            context.sendBroadcast(i);
        } catch (Throwable ignored) {
        }
    }

    // ============================
    // Endpoint constants
    // ============================
    public static final String PATH_REGISTER = "auth/register";
    public static final String PATH_VERIFY_OTP = "auth/verify-otp";

    // ============================
    // Access token
    // ============================
    private static final String PREF_ACCESS = "fastfood_auth";
    private static final String KEY_ACCESS = "accessToken";

    public static void saveAccessToken(Context context, String token) {
        SharedPreferences sp = context.getSharedPreferences(PREF_ACCESS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ACCESS, token).apply();
    }

    @Nullable
    public static String getAccessToken(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_ACCESS, Context.MODE_PRIVATE);
        String t = sp.getString(KEY_ACCESS, null);
        return (t == null || t.trim().isEmpty()) ? null : t;
    }

    public static void clearAccessToken(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_ACCESS, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_ACCESS).apply();
    }

    // ============================
    // Refresh/role/email (khớp AuthClient)
    // ============================
    private static final String PREF_SESSION = "auth_session";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";

    @Nullable
    private static String getRefreshToken(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
        return sp.getString(KEY_REFRESH, null);
    }

    private static void saveRefreshToken(Context context, @Nullable String refresh) {
        SharedPreferences sp = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
        if (TextUtils.isEmpty(refresh))
            sp.edit().remove(KEY_REFRESH).apply();
        else
            sp.edit().putString(KEY_REFRESH, refresh).apply();
    }

    /** Xoá toàn bộ session khi refresh fail → phát broadcast để UI điều hướng Login. */
    public static void clearAllSession(Context context) {
        SharedPreferences a = context.getSharedPreferences(PREF_ACCESS, Context.MODE_PRIVATE);
        a.edit().remove(KEY_ACCESS).apply();
        SharedPreferences s = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
        s.edit().remove(KEY_REFRESH).remove(KEY_ROLE).remove(KEY_EMAIL).apply();
        sendAuthClearedBroadcast(context, "refresh_failed_or_unauthorized");
    }

    // ============================
    // Base URL helpers
    // ============================

    /**
     * Trả về root URL dạng http://host:port/
     *
     * Emulator  → http://103.75.182.180:8000/ (VPS IP)
     * Máy thật  → Sử dụng BuildConfig.BASE_URL (VPS IP)
     */
    public static String baseRoot() {
        String url;
        if (isEmulator()) {
            url = "http://103.75.182.180:8000/";
        } else {
            url = com.example.app.BuildConfig.BASE_URL;
        }

        url = url.trim();
        if (!startsWithHttp(url)) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    // (Giữ lại cho tương thích, nếu chỗ khác có dùng)
    private static String extractPort(String url) {
        if (TextUtils.isEmpty(url)) return null;
        try {
            Pattern pattern = Pattern.compile(":(\\d+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean isEmulator() {
        try {
            String fp = Build.FINGERPRINT != null ? Build.FINGERPRINT : "";
            String model = Build.MODEL != null ? Build.MODEL : "";
            String manu = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
            String prod = Build.PRODUCT != null ? Build.PRODUCT : "";
            if (fp.startsWith("generic") || fp.contains("emulator"))
                return true;
            if (model.contains("Emulator") || model.contains("Android SDK built for x86"))
                return true;
            if (manu.contains("Genymotion"))
                return true;
            if (prod.contains("sdk_google"))
                return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean startsWithHttp(String url) {
        if (TextUtils.isEmpty(url))
            return false;
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String ensureSlash(String s) {
        if (TextUtils.isEmpty(s))
            return "/";
        return s.endsWith("/") ? s : s + "/";
    }

    // ============================
    // OkHttp/Retrofit builders
    // ============================
    private static volatile Retrofit retrofitRoot;
    private static volatile Retrofit retrofitApi;
    private static volatile OkHttpClient refreshOnlyClient; // client phụ cho /auth/refresh

    private static OkHttpClient buildHttpClient(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        // Gắn Accept + Authorization
        Interceptor headerInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "FastFood-Android/" + VERSION_NAME);

            String token = getAccessToken(context);
            if (!TextUtils.isEmpty(token)) {
                builder.header("Authorization", "Bearer " + token);
            }
            return chain.proceed(builder.build());
        };

        return new OkHttpClient.Builder()
                .addInterceptor(headerInterceptor)
                .addInterceptor(logging)
                .authenticator(new TokenRefreshAuthenticator(context.getApplicationContext()))
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .protocols(Arrays.asList(Protocol.HTTP_1_1))
                .build();
    }

    /** Client phụ (không Authenticator) để gọi /auth/refresh */
    private static OkHttpClient buildRefreshOnlyClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .protocols(Arrays.asList(Protocol.HTTP_1_1))
                .build();
    }

    /** Retrofit base = http://host:port/ */
    public static Retrofit getRetrofitRoot(Context context) {
        if (retrofitRoot == null) {
            synchronized (BackendConfig.class) {
                if (retrofitRoot == null) {
                    if (refreshOnlyClient == null)
                        refreshOnlyClient = buildRefreshOnlyClient();
                    retrofitRoot = new Retrofit.Builder()
                            .baseUrl(baseRoot())
                            .client(buildHttpClient(context))
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofitRoot;
    }

    /** Retrofit base = http://host:port/ (không /api/) */
    public static Retrofit getRetrofitApi(Context context) {
        if (retrofitApi == null) {
            synchronized (BackendConfig.class) {
                if (retrofitApi == null) {
                    if (refreshOnlyClient == null)
                        refreshOnlyClient = buildRefreshOnlyClient();
                    retrofitApi = new Retrofit.Builder()
                            .baseUrl(baseRoot())
                            .client(buildHttpClient(context))
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofitApi;
    }

    /** Convenience: mặc định trả về base (không /api/) */
    public static Retrofit getRetrofit(Context context) {
        return getRetrofitApi(context);
    }

    /** Reset cả hai instance (khi logout, đổi môi trường, v.v.) */
    public static void resetRetrofit() {
        retrofitRoot = null;
        retrofitApi = null;
    }

    // ============================
    // Authenticator: silent refresh once
    // ============================
    private static final class TokenRefreshAuthenticator implements Authenticator {
        private final Context app;
        private static final Object LOCK = new Object();

        TokenRefreshAuthenticator(Context app) {
            this.app = app.getApplicationContext();
        }

        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
            // Không refresh cho chính request /auth/refresh
            if (response.request().url().encodedPath().endsWith("/auth/refresh")) {
                return null;
            }

            // Chỉ thử refresh một lần
            if (responseCount(response) >= 2) {
                return null;
            }

            final String oldBearer = extractBearer(response.request().header("Authorization"));

            synchronized (LOCK) {
                // Nếu thread khác vừa refresh xong → dùng token mới
                final String current = getAccessToken(app);
                if (!TextUtils.isEmpty(current) && !TextUtils.equals(current, oldBearer)) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + current)
                            .build();
                }

                // Lấy refresh token
                final String refresh = getRefreshToken(app);
                if (TextUtils.isEmpty(refresh)) {
                    clearAllSession(app);
                    return null;
                }

                // Gọi /auth/refresh (sync) bằng refreshOnlyClient
                try {
                    String url = ensureSlash(baseRoot()) + "auth/refresh";
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    JSONObject body = new JSONObject();
                    body.put("refresh_token", refresh);

                    Request req = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(JSON, body.toString()))
                            .header("Accept", "application/json")
                            .build();

                    Response r = refreshOnlyClient.newCall(req).execute();
                    if (!r.isSuccessful()) {
                        clearAllSession(app);
                        return null;
                    }

                    String raw = r.body() != null ? r.body().string() : "";
                    JSONObject obj = new JSONObject(raw);
                    String newAccess = obj.optString("accessToken", obj.optString("access_token", ""));
                    String newRefresh = obj.optString("refreshToken", obj.optString("refresh_token", ""));

                    if (TextUtils.isEmpty(newAccess)) {
                        clearAllSession(app);
                        return null;
                    }

                    // Lưu token mới
                    saveAccessToken(app, newAccess);
                    if (!TextUtils.isEmpty(newRefresh)) {
                        saveRefreshToken(app, newRefresh);
                    }

                    // Retry request ban đầu với access token mới
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + newAccess)
                            .build();

                } catch (Exception e) {
                    clearAllSession(app);
                    return null;
                }
            }
        }

        private static int responseCount(Response r) {
            int count = 1;
            while ((r = r.priorResponse()) != null)
                count++;
            return count;
        }

        @Nullable
        private static String extractBearer(@Nullable String authHeader) {
            if (authHeader == null)
                return null;
            if (authHeader.startsWith("Bearer "))
                return authHeader.substring(7);
            return authHeader;
        }
    }
}
