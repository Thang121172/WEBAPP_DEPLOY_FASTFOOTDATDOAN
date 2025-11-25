package com.example.app;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.example.app.R;
import com.example.app.adapters.CartAdapter;
import com.example.app.data.CartRepository;
import com.example.app.model.CartItem;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CartActivity: Xử lý hiển thị và tương tác với giỏ hàng.
 * - Cần khai báo trong AndroidManifest.xml
 */
public class CartActivity extends AppCompatActivity implements CartAdapter.OnTotalsListener {

    private TextView tvSubtotal;
    private TextView tvTotal;
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private CartRepository cartRepository;

    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_cart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Khởi tạo Views
        recyclerView = findViewById(R.id.rv_cart_items);
        tvSubtotal = findViewById(R.id.tv_cart_subtotal);
        tvTotal = findViewById(R.id.tv_cart_total);

        // Khởi tạo CartRepository
        cartRepository = CartRepository.getInstance();

        // Thiết lập RecyclerView và Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, new ArrayList<>(), R.layout.item_cart_item);
        cartAdapter.setTotalsListener(this);
        recyclerView.setAdapter(cartAdapter);

        // Quan sát thay đổi trong giỏ hàng
        cartRepository.getCartItems().observe(this, new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if (cartItems != null) {
                    cartAdapter.updateData(cartItems);
                    updateTotals();
                }
            }
        });

        // Quan sát tổng tiền
        cartRepository.getCartSubtotal().observe(this, new Observer<Double>() {
            @Override
            public void onChanged(Double subtotal) {
                updateTotals();
            }
        });

        findViewById(R.id.btn_to_checkout).setOnClickListener(v -> {
            // Kiểm tra giỏ hàng có rỗng không
            List<CartItem> items = cartRepository.getCartItems().getValue();
            if (items == null || items.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Quay về MainActivity và navigate tới CheckoutFragment
            Intent intent = new Intent(CartActivity.this, MainActivity.class);
            intent.putExtra("navigate_to", "checkout");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        
        // Xử lý thay đổi số lượng từ adapter
        cartAdapter.setOnQuantityChangeListener((item, newQuantity) -> {
            cartRepository.updateItemQuantity(item.cartItemId, newQuantity);
        });

        // Cập nhật tổng tiền ban đầu
        updateTotals();
    }

    /**
     * Cập nhật tổng tiền từ dữ liệu thực trong giỏ hàng
     */
    private void updateTotals() {
        List<CartItem> items = cartRepository.getCartItems().getValue();
        if (items == null || items.isEmpty()) {
            tvSubtotal.setText("0 đ");
            tvTotal.setText("0 đ");
            return;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal price = BigDecimal.valueOf(item.getPrice());
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            subtotal = subtotal.add(price.multiply(qty));
        }

        tvSubtotal.setText(vnd.format(subtotal.longValue()) + " đ");
        tvTotal.setText(vnd.format(subtotal.longValue()) + " đ");
    }

    @Override
    public void onTotalsCalculated(BigDecimal total) {
        // Cập nhật từ CartAdapter callback
        tvSubtotal.setText(vnd.format(total.longValue()) + " đ");
        tvTotal.setText(vnd.format(total.longValue()) + " đ");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}