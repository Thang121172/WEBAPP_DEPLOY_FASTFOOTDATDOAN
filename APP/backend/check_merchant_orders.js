/**
 * Script để kiểm tra đơn hàng của merchant
 * Usage: node check_merchant_orders.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.PGDATABASE || process.env.POSTGRES_DB || 'fastfood';
const DB_USER = process.env.DB_USER || process.env.PGUSER || process.env.POSTGRES_USER || 'app';
const DB_PASS = process.env.DB_PASSWORD || process.env.PGPASSWORD || process.env.POSTGRES_PASSWORD || '123456';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASS,
});

async function checkMerchantOrders() {
  try {
    console.log('=== Kiểm tra đơn hàng của Merchant ===\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    const merchantEmail = 'merchant.honai@gmail.com';

    // 1. Tìm merchant user
    const userResult = await pool.query(
      'SELECT id, username, email, role FROM users WHERE email = $1',
      [merchantEmail]
    );

    if (userResult.rows.length === 0) {
      console.log('❌ Không tìm thấy merchant với email:', merchantEmail);
      await pool.end();
      process.exit(1);
    }

    const merchant = userResult.rows[0];
    console.log(`✅ Merchant: ID=${merchant.id}, Email=${merchant.email}\n`);

    // 2. Tìm restaurant của merchant
    const restaurantResult = await pool.query(
      `SELECT r.id, r.name FROM restaurants r
       WHERE r.id = $1 OR r.id IN (SELECT restaurant_id FROM user_restaurants WHERE user_id = $1)`,
      [merchant.id]
    );

    let restaurantId;
    if (restaurantResult.rows.length === 0) {
      console.log('⚠️  Không tìm thấy restaurant, dùng user_id làm restaurant_id');
      restaurantId = merchant.id;
    } else {
      restaurantId = restaurantResult.rows[0].id;
      console.log(`✅ Restaurant: ID=${restaurantId}, Name=${restaurantResult.rows[0].name || 'N/A'}\n`);
    }

    // 3. Tìm tất cả đơn hàng
    const allOrdersResult = await pool.query(
      'SELECT id, restaurant_id, user_id, status, total_amount, created_at FROM orders ORDER BY created_at DESC LIMIT 10'
    );
    console.log(`📦 Tổng số đơn hàng (10 mới nhất): ${allOrdersResult.rows.length}\n`);
    
    if (allOrdersResult.rows.length > 0) {
      console.log('Danh sách đơn hàng:');
      allOrdersResult.rows.forEach((order, idx) => {
        console.log(`  ${idx + 1}. Order ID=${order.id}, Restaurant ID=${order.restaurant_id}, Status=${order.status}, Amount=${order.total_amount}`);
      });
      console.log('');
    }

    // 4. Tìm đơn hàng của merchant này
    const merchantOrdersResult = await pool.query(
      `SELECT o.id, o.restaurant_id, o.user_id, o.status, o.total_amount, o.created_at, u.username as customer_name
       FROM orders o
       LEFT JOIN users u ON u.id = o.user_id
       WHERE o.restaurant_id = $1
       ORDER BY o.created_at DESC`,
      [restaurantId]
    );

    console.log(`📋 Đơn hàng của merchant (restaurant_id=${restaurantId}): ${merchantOrdersResult.rows.length}\n`);

    if (merchantOrdersResult.rows.length === 0) {
      console.log('❌ Không có đơn hàng nào!');
      console.log('\n🔍 Kiểm tra các đơn hàng có restaurant_id khác:');
      const otherOrders = allOrdersResult.rows.filter(o => o.restaurant_id !== restaurantId);
      if (otherOrders.length > 0) {
        console.log(`   Có ${otherOrders.length} đơn hàng với restaurant_id khác:`);
        otherOrders.forEach(o => {
          console.log(`   - Order ID=${o.id}, Restaurant ID=${o.restaurant_id}`);
        });
      }
    } else {
      console.log('Danh sách đơn hàng:');
      merchantOrdersResult.rows.forEach((order, idx) => {
        console.log(`  ${idx + 1}. Order ID=${order.id}`);
        console.log(`     - Customer: ${order.customer_name || 'N/A'}`);
        console.log(`     - Status: ${order.status}`);
        console.log(`     - Amount: ${order.total_amount}`);
        console.log(`     - Created: ${order.created_at}`);
        console.log('');
      });
    }

    // 5. Kiểm tra đơn hàng theo status
    const statuses = ['new', 'preparing', 'in_progress', 'ready', 'completed', 'cancelled'];
    console.log('\n📊 Đơn hàng theo status:');
    for (const status of statuses) {
      const statusResult = await pool.query(
        'SELECT COUNT(*) as count FROM orders WHERE restaurant_id = $1 AND status = $2',
        [restaurantId, status.toUpperCase()]
      );
      const count = parseInt(statusResult.rows[0].count, 10);
      if (count > 0) {
        console.log(`   - ${status.toUpperCase()}: ${count}`);
      }
    }

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err.message);
    console.error('Stack:', err.stack);
    await pool.end();
    process.exit(1);
  }
}

checkMerchantOrders();

