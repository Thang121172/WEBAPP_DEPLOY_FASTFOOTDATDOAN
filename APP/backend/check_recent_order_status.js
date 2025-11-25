const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function checkRecentOrders() {
  try {
    // Check 5 most recent orders
    const result = await pool.query(`
      SELECT id, status, shipper_id, restaurant_id, created_at
      FROM orders 
      ORDER BY id DESC 
      LIMIT 5
    `);
    
    console.log('📦 5 đơn hàng mới nhất:\n');
    result.rows.forEach((order, index) => {
      console.log(`${index + 1}. Đơn #${order.id}:`);
      console.log(`   - Status: ${order.status}`);
      console.log(`   - Shipper ID: ${order.shipper_id || 'NULL (chưa có shipper)'}`);
      console.log(`   - Restaurant ID: ${order.restaurant_id}`);
      console.log(`   - Created: ${order.created_at}`);
      console.log();
    });
    
    // Check orders without shipper that should be visible
    const availableOrders = await pool.query(`
      SELECT id, status, shipper_id
      FROM orders
      WHERE shipper_id IS NULL 
        AND status IN ('PENDING', 'CONFIRMED', 'COOKING', 'READY')
      ORDER BY id DESC
      LIMIT 10
    `);
    
    console.log(`\n✅ Đơn hàng có thể nhận (chưa có shipper): ${availableOrders.rows.length} đơn\n`);
    availableOrders.rows.forEach((order, index) => {
      console.log(`${index + 1}. Đơn #${order.id}: status = ${order.status}`);
    });
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

checkRecentOrders();

