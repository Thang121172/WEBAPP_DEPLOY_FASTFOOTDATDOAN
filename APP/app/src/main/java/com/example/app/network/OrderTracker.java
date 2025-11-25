package com.example.app.network;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * OrderTracker
 *
 * - Quản lý kết nối Socket.IO để nhận realtime:
 * + "order:update" (trạng thái đơn)
 * + "shipper:location" (tọa độ shipper)
 *
 * Gợi ý khởi tạo từ Fragment:
 * String baseRoot = BackendConfig.getRetrofitRoot(ctx).baseUrl().toString(); //
 * KHÔNG /api/
 * OrderTracker t = new OrderTracker(ctx, baseRoot);
 * t.connect();
 * t.joinOrder(orderId);
 * t.onOrderUpdate(...);
 * t.onShipperLocation(...);
 */
public class OrderTracker {
    private static final String TAG = "OrderTracker";
    private Socket socket;

    private final Context appContext;
    private final String baseRootUrl; // ví dụ: http://10.0.2.2:8000/
    private String bearerToken; // Authorization

    // Giữ orderId để auto-join khi connect xong
    private @Nullable String pendingOrderId = null;

    // Listeners để tiện off() khi disconnect
    private final List<ListenerPair> registered = new ArrayList<>();

    private static class ListenerPair {
        final String event;
        final Emitter.Listener listener;

        ListenerPair(String e, Emitter.Listener l) {
            event = e;
            listener = l;
        }
    }

    public OrderTracker(Context ctx, String backendBaseRoot) {
        this.appContext = ctx.getApplicationContext();
        // Đảm bảo baseRoot không chứa "/api" và luôn kết thúc bằng "/"
        String base = backendBaseRoot == null ? "" : backendBaseRoot.trim();
        if (!base.endsWith("/"))
            base = base + "/";
        this.baseRootUrl = base;

        // Lấy sẵn access token (nếu có) từ BackendConfig
        this.bearerToken = BackendConfig.getAccessToken(appContext);

        buildSocket();
    }

    /** Nếu cần thay token lúc runtime (ví dụ vừa refresh token) */
    public void setAuthToken(@Nullable String token) {
        this.bearerToken = token;
        // Muốn áp dụng header mới cần rebuild socket
        rebuildAndReconnectIfNeeded();
    }

    private void rebuildAndReconnectIfNeeded() {
        boolean wasConnected = socket != null && socket.connected();
        if (socket != null) {
            try {
                // off mọi listeners để tránh leak
                offAllRegistered();
                socket.off();
                socket.disconnect();
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        buildSocket();
        if (wasConnected)
            connect();
    }

    private void buildSocket() {
        try {
            IO.Options opts = new IO.Options();
            // Ưu tiên websocket (tránh polling chậm)
            opts.transports = new String[] { "websocket" };
            // Reconnect
            opts.reconnection = true;
            opts.reconnectionAttempts = 10;
            opts.reconnectionDelay = 1500; // ms
            opts.reconnectionDelayMax = 6000; // ms
            opts.timeout = 10_000; // ms

            // Đính kèm Authorization nếu có
            if (bearerToken != null && !bearerToken.trim().isEmpty()) {
                Map<String, List<String>> headers = new HashMap<>();
                List<String> authList = new ArrayList<>();
                authList.add("Bearer " + bearerToken);
                headers.put("Authorization", authList);
                opts.extraHeaders = headers;
            }

            // Path socket.io mặc định là "/socket.io/", đa số backend giữ nguyên
            // Nếu backend đổi path, set opts.path = "/custom-socket-path";
            socket = IO.socket(baseRootUrl, opts);

            // Sự kiện hệ thống
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, args -> Log.i(TAG, "socket DISCONNECT"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "socket CONNECT_ERROR: " + (args != null && args.length > 0 ? args[0] : "unknown"));
            });
            // Some socket.io-client versions may not expose reconnect constants; use
            // literal event names
            socket.on("reconnect_attempt", args -> Log.w(TAG, "socket RECONNECT_ATTEMPT"));
            socket.on("reconnect_failed", args -> Log.e(TAG, "socket RECONNECT_FAILED"));

        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid socket url: " + baseRootUrl, e);
        }
    }

    private final Emitter.Listener onConnect = args -> {
        Log.i(TAG, "socket CONNECTED");
        // Tự join order đang pending (nếu có)
        if (pendingOrderId != null && socket != null && socket.connected()) {
            socket.emit("joinOrder", pendingOrderId);
            Log.i(TAG, "emit joinOrder (auto): " + pendingOrderId);
        }
    };

    // ===== Public API =====

    public void connect() {
        if (socket != null && !socket.connected())
            socket.connect();
    }

    public void disconnect() {
        if (socket != null) {
            try {
                offAllRegistered();
                socket.off();
                socket.disconnect();
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void joinOrder(String orderId) {
        this.pendingOrderId = orderId;
        if (socket != null && socket.connected()) {
            socket.emit("joinOrder", orderId);
            Log.i(TAG, "emit joinOrder: " + orderId);
        } else {
            Log.i(TAG, "join deferred until connected: " + orderId);
        }
    }

    public void leaveOrder(String orderId) {
        if (socket != null && socket.connected()) {
            socket.emit("leaveOrder", orderId);
            Log.i(TAG, "emit leaveOrder: " + orderId);
        }
        if (orderId != null && orderId.equals(pendingOrderId)) {
            pendingOrderId = null;
        }
    }
    
    /** Identify user để join vào user room */
    public void identify(String userId, String role) {
        if (socket != null && socket.connected()) {
            org.json.JSONObject payload = new org.json.JSONObject();
            try {
                payload.put("userId", userId);
                if (role != null) {
                    payload.put("role", role);
                }
                socket.emit("identify", payload);
                Log.i(TAG, "emit identify: userId=" + userId + ", role=" + role);
            } catch (org.json.JSONException e) {
                Log.e(TAG, "Error creating identify payload", e);
            }
        } else {
            Log.w(TAG, "Socket not connected, cannot identify user");
        }
    }

    /** Lắng nghe cập nhật trạng thái đơn */
    public void onOrderUpdate(Emitter.Listener listener) {
        on("order:update", listener);
    }

    /** Lắng nghe vị trí shipper */
    public void onShipperLocation(Emitter.Listener listener) {
        on("shipper:location", listener);
    }

    /** Đăng ký listener cho event bất kỳ và tự quản lý lifecycle để off() */
    public void on(String event, Emitter.Listener listener) {
        if (socket == null)
            return;
        socket.on(event, listener);
        registered.add(new ListenerPair(event, listener));
    }

    private void offAllRegistered() {
        if (socket == null)
            return;
        for (ListenerPair p : registered) {
            try {
                socket.off(p.event, p.listener);
            } catch (Exception ignored) {
            }
        }
        registered.clear();
    }
}
