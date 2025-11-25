package com.example.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.pm.PackageManager;

import com.example.app.LocationUpdateService;

public final class ShipperLocationHelper {

    private ShipperLocationHelper() {
    }

    // ===== Permission request codes =====
    public static final int REQ_CODE_LOCATION = 7011;
    public static final int REQ_CODE_NOTIF = 7012;

    // ===== Public APIs (Activity) =====

    /**
     * Bắt đầu cập nhật vị trí nếu đã đủ quyền; nếu chưa, tự xin quyền.
     * 
     * @param intervalMs chu kỳ gửi vị trí (ms), ví dụ 15_000
     */
    public static void startUpdatesIfPermitted(@NonNull Activity activity, long intervalMs) {
        if (!hasLocationPermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_CODE_LOCATION);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationsPermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[] { android.Manifest.permission.POST_NOTIFICATIONS },
                    REQ_CODE_NOTIF);
            // Không return: nếu người dùng từ chối thông báo, service vẫn có thể chạy,
            // nhưng khuyến nghị xin để tránh crash khi đăng thông báo trên một số thiết bị
            // tuỳ biến.
        }
        startService(activity.getApplicationContext(), intervalMs);
    }

    public static void stopUpdates(@NonNull Context ctx) {
        Intent i = new Intent(ctx, LocationUpdateService.class);
        i.setAction(LocationUpdateService.ACTION_STOP);
        ctx.startService(i);
    }

    // ===== Public APIs (Fragment) =====

    public static void startUpdatesIfPermitted(@NonNull Fragment fragment, long intervalMs) {
        Activity activity = fragment.getActivity();
        if (activity == null)
            return;

        if (!hasLocationPermission(activity)) {
            fragment.requestPermissions(
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_CODE_LOCATION);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationsPermission(activity)) {
            fragment.requestPermissions(
                    new String[] { android.Manifest.permission.POST_NOTIFICATIONS },
                    REQ_CODE_NOTIF);
        }
        startService(activity.getApplicationContext(), intervalMs);
    }

    /**
     * Forward từ onRequestPermissionsResult của Activity/Fragment về đây.
     * Trả về true nếu helper đã xử lý (để bạn biết có cần xử lý thêm không).
     */
    public static boolean onRequestPermissionsResult(@NonNull Object host,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            long intervalMs) {
        if (requestCode != REQ_CODE_LOCATION && requestCode != REQ_CODE_NOTIF) {
            return false;
        }

        Context ctx = null;
        if (host instanceof Activity) {
            ctx = (Activity) host;
        } else if (host instanceof Fragment) {
            ctx = ((Fragment) host).getActivity();
        }
        if (ctx == null)
            return true;

        // Nếu là kết quả xin location → nếu granted thì start service
        if (requestCode == REQ_CODE_LOCATION) {
            if (granted(grantResults)) {
                startService(ctx.getApplicationContext(), intervalMs);
            }
            // Nếu user từ chối thì không làm gì — bạn có thể show dialog hướng dẫn.
            return true;
        }

        // Nếu là kết quả xin notif → không bắt buộc để chạy service. Không cần xử lý
        // thêm.
        return true;
    }

    // ===== Optional: Location settings helpers =====

    /** Kiểm tra người dùng đã bật location providers (GPS/Network) chưa. */
    public static boolean isLocationEnabled(@NonNull Context ctx) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null)
            return false;
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignore) {
            return false;
        }
    }

    /** Gợi ý mở màn hình bật Location khi đang tắt. */
    public static void promptEnableLocation(@NonNull Activity activity) {
        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivity(i);
    }

    // ===== Internals =====

    private static void startService(@NonNull Context appCtx, long intervalMs) {
        Intent i = new Intent(appCtx, LocationUpdateService.class);
        i.setAction(LocationUpdateService.ACTION_START);
        i.putExtra(LocationUpdateService.EXTRA_INTERVAL_MS, intervalMs);
        ContextCompat.startForegroundService(appCtx, i);
    }

    private static boolean hasLocationPermission(@NonNull Context ctx) {
        return ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasPostNotificationsPermission(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return true;
        return ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean granted(@Nullable int[] grantResults) {
        return grantResults != null
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
