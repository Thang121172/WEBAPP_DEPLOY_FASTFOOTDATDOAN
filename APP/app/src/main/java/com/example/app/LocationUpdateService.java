package com.example.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.app.network.AuthClient;
import com.example.app.network.ShipperApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Foreground service gửi vị trí shipper về server định kỳ.
 *
 * YÊU CẦU:
 * - Manifest: <service ... foregroundServiceType="location" />
 * - Quyền: ACCESS_FINE_LOCATION/COARSE, (tuỳ) POST_NOTIFICATIONS cho Android
 * 13+
 * - play-services-location dependency.
 */
public class LocationUpdateService extends Service {

    public static final String NOTI_CHANNEL_ID = "shipper_tracking";
    public static final int NOTI_ID = 11001;
    public static final String ACTION_STOP = "com.example.app.ACTION_STOP_LOCATION";
    // Public action to start the service
    public static final String ACTION_START = "com.example.app.ACTION_START_LOCATION";
    // Extra to override push interval (ms)
    public static final String EXTRA_INTERVAL_MS = "com.example.app.EXTRA_INTERVAL_MS";

    // Interval cấu hình (ms)
    private static final long LOCATION_INTERVAL_MS = TimeUnit.SECONDS.toMillis(20);
    private static final long LOCATION_FASTEST_MS = TimeUnit.SECONDS.toMillis(10);

    // Throttle gửi API
    private static final long API_MIN_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    private ShipperApi shipperApi;
    private long lastApiSentAt = 0L;

    @Override
    public void onCreate() {
        super.onCreate();

        shipperApi = new AuthClient(this).getRetrofit().create(ShipperApi.class);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        ensureChannel();
        startForeground(NOTI_ID, buildNotification(null));

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    maybeSendLocation(loc);
                    Notification n = buildNotification(loc);
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.notify(NOTI_ID, n);
                }
            }
        };

        requestLocationUpdates();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelfSafely();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // không support bind
    }

    // ========== Location ==========

    private void requestLocationUpdates() {
        if (!hasLocationPermission()) {
            stopSelfSafely();
            return;
        }

        LocationRequest req = new LocationRequest.Builder(LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        try {
            fusedClient.requestLocationUpdates(
                    req,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException se) {
            stopSelfSafely();
        }
    }

    private void stopLocationUpdates() {
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void maybeSendLocation(@NonNull Location loc) {
        long now = System.currentTimeMillis();
        if (now - lastApiSentAt < API_MIN_INTERVAL_MS)
            return;

        Map<String, Object> body = new HashMap<>();
        body.put("lat", loc.getLatitude());
        body.put("lng", loc.getLongitude());
        if (loc.hasAccuracy())
            body.put("accuracy", loc.getAccuracy());

        lastApiSentAt = now;

        shipperApi.updateLocation(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                    @NonNull Response<Map<String, Object>> response) {
                // im lặng
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call,
                    @NonNull Throwable t) {
                // im lặng, lần sau gửi lại
            }
        });
    }

    // ========== Notification ==========

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    NOTI_CHANNEL_ID,
                    "Theo dõi vị trí Shipper",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Cập nhật vị trí thời gian thực để giao hàng");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(@Nullable Location last) {
        String content = "Đang cập nhật vị trí giao hàng…";
        if (last != null) {
            content = String.format(
                    "Vị trí: %.5f, %.5f (±%.0fm)",
                    last.getLatitude(),
                    last.getLongitude(),
                    last.hasAccuracy() ? last.getAccuracy() : 0f);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPI = PendingIntent.getActivity(
                this, 1001, openIntent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(this, LocationUpdateService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPI = PendingIntent.getService(
                this, 1002, stopIntent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, NOTI_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Đang theo dõi vị trí")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(openPI)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_delete,
                        "Dừng",
                        stopPI))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void stopSelfSafely() {
        stopLocationUpdates();
        stopForeground(true);
        stopSelf();
    }
}
