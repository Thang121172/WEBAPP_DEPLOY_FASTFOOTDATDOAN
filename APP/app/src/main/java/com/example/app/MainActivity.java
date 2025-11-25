package com.example.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu; // Import cần thiết
import android.view.MenuItem; // Import cần thiết
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.app.auth.AuthEvents;
import com.example.app.ui.AuthLoginActivity;
import com.example.app.network.AuthClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * MainActivity
 * - FIX: Đã chuyển xử lý Menu sang onCreateOptionsMenu/onOptionsItemSelected để ổn định hơn.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    private MaterialToolbar toolbar;
    private FloatingActionButton fab;

    private AuthClient authClient;

    private boolean authReceiverRegistered = false;

    private final BroadcastReceiver authClearedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (AuthEvents.ACTION_AUTH_SESSION_CLEARED.equals(i.getAction())) {
                String reason = i.getStringExtra(AuthEvents.EXTRA_REASON);
                authClient.clearSession();
                launchAuthActivity();
                Toast.makeText(
                        c,
                        reason != null ? reason : "Phiên đã hết hạn, vui lòng đăng nhập lại.",
                        Toast.LENGTH_SHORT).show();
                refreshRoleUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authClient = new AuthClient(this);

        if (!authClient.isSignedIn()) {
            Log.d(TAG, "User not signed in. Launching AuthActivity.");
            authClient.clearSession();
            launchAuthActivity();
            return;
        }

        String role = authClient.getRole();

        if (TextUtils.isEmpty(role)) {
            Log.w(TAG, "Signed in but role is empty. Forcing re-login to clear faulty local data.");
            authClient.clearSession();
            launchAuthActivity();
            return;
        }

        // =======================================================================
        // === KHỞI TẠO UI ===
        // =======================================================================
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
            int startDest = determineStartDestination(role);
            navGraph.setStartDestination(startDest);
            navController.setGraph(navGraph);
        }

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        // DÒNG NÀY RẤT QUAN TRỌNG: Gắn MaterialToolbar vào Activity
        setSupportActionBar(toolbar);

        // AppBarConfiguration
        if (navController != null) {
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.customerHomeFragment, R.id.merchantHomeFragment, R.id.shipperDashboardFragment, R.id.adminHomeFragment
            ).build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        }

        // FAB chỉ cho ADMIN
        fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> Snackbar.make(v, getString(R.string.admin_action), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.ok_label),
                            click -> Toast.makeText(
                                    this,
                                    getString(R.string.action_label),
                                    Toast.LENGTH_SHORT).show())
                    .show());
        }

        refreshRoleUI();
        
        // Xử lý intent để navigate tới checkoutFragment nếu cần (sau khi navController đã được khởi tạo)
        handleNavigationIntent(getIntent());

        // Loại bỏ toolbar.setOnMenuItemClickListener nếu sử dụng onCreateOptionsMenu/onOptionsItemSelected
        // if (toolbar != null) { ... }
    }

    // =========================================================================
    // === XỬ LÝ MENU (Cách truyền thống, ổn định hơn) ===
    // =========================================================================

    /**
     * Gắn menu XML (menu_main.xml) vào Toolbar.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Đây là nơi file menu_main.xml được đọc và gắn vào Toolbar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Bắt sự kiện click vào các mục menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            doLogout();
            return true;
        } else if (id == R.id.action_profile) {
            // ✅ FIX: Navigate đến shipper profile nếu đang ở shipper dashboard
            if (navController != null) {
                int currentDest = navController.getCurrentDestination() != null 
                    ? navController.getCurrentDestination().getId() 
                    : -1;
                if (currentDest == R.id.shipperDashboardFragment) {
                    navController.navigate(R.id.action_shipperDashboardFragment_to_shipperProfileFragment);
                    return true;
                }
            }
            Toast.makeText(this, "Chỉ khả dụng cho shipper", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            // Menu item này có submenu, không cần xử lý ở đây
            return false;
        }

        // Nếu không phải action_logout/action_settings, Navigation Component sẽ xử lý.
        if (navController != null) {
            return NavigationUI.onNavDestinationSelected(item, navController)
                    || super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }
    // =========================================================================

    private int determineStartDestination(@NonNull String role) {
        int dest = R.id.customerHomeFragment;

        String r = role.trim().toLowerCase();
        if ("admin".equals(r)) {
            dest = R.id.adminHomeFragment;
        } else if ("shop".equals(r) || "merchant".equals(r)) {
            dest = R.id.merchantHomeFragment;
        } else if ("shipper".equals(r)) {
            dest = R.id.shipperDashboardFragment;
        } else {
            dest = R.id.customerHomeFragment;
        }

        Log.d(TAG, "Starting destination set to: " + getResources().getResourceEntryName(dest) + " (Role: " + role + ")");
        return dest;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!authReceiverRegistered) {
            IntentFilter filter = new IntentFilter(AuthEvents.ACTION_AUTH_SESSION_CLEARED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(authClearedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(authClearedReceiver, filter);
            }
            authReceiverRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authReceiverRegistered) {
            try {
                unregisterReceiver(authClearedReceiver);
            } catch (IllegalArgumentException ignored) {
            } finally {
                authReceiverRegistered = false;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authClient.isSignedIn() && !isFinishing()) {
            launchAuthActivity();
            return;
        }
        refreshRoleUI();
    }
    
    private void handleNavigationIntent(Intent intent) {
        if (intent != null && "checkout".equals(intent.getStringExtra("navigate_to"))) {
            if (navController != null) {
                try {
                    navController.navigate(R.id.checkoutFragment);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to checkoutFragment: " + e.getMessage());
                }
            }
            // Xóa extra để tránh navigate lại
            intent.removeExtra("navigate_to");
        }
    }

    public void refreshRoleUI() {
        if (fab == null)
            return;
        String role = authClient.getRole();
        if (role != null && role.equalsIgnoreCase("admin")) {
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    private void doLogout() {
        authClient.logout(authClient.getRefreshToken(), null);
        authClient.clearSession();
        launchAuthActivity();
        Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();
    }

    private void launchAuthActivity() {
        Intent intent = new Intent(this, AuthLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null && appBarConfiguration != null) {
            return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}