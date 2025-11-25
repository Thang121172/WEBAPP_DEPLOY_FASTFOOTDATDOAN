package com.example.app.data;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.util.Log;

import com.example.app.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

/**
 * ApiClient
 * - Thiết lập Retrofit để tạo kết nối API, sử dụng BASE_URL từ BuildConfig.
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Log.d(TAG, "Khởi tạo Retrofit với BASE_URL: " + BuildConfig.BASE_URL);

            try {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BuildConfig.BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khởi tạo Retrofit: " + e.getMessage());
                throw new RuntimeException("Lỗi cấu hình Retrofit: " + e.getMessage());
            }
        }
        return retrofit;
    }
}