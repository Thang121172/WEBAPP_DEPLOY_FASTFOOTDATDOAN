package com.example.app.api;

import com.example.app.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Sử dụng BASE_URL từ BuildConfig (đã cấu hình trong build.gradle.kts)
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            // Thêm Logging Interceptor để dễ dàng debug network
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public static RestaurantApiService getRestaurantService() {
        return getClient().create(RestaurantApiService.class);
    }
}