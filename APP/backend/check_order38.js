const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function checkOrder38() {
  try {
    // Check order #38
    const orderResult = await pool.query(`
      SELECT id, restaurant_id, status, 
             COALESCE(total_amount, total, 0) as total 
      FROM orders 
      WHERE id = 38
    `);
    
    console.log('📦 Đơn hàng #38:');
    console.log(JSON.stringify(orderResult.rows[0], null, 2));
    console.log();
    
    // Check revenue for restaurant #4
    const revenueResult = await pool.query(`
      SELECT 
        COUNT(*) as total_orders,
        SUM(CASE WHEN status = 'DELIVERED' THEN COALESCE(total_amount, total, 0) ELSE 0 END) as revenue
      FROM orders
      WHERE restaurant_id = 4
    `);
    
    console.log('💰 Doanh thu của nhà hàng #4:');
    console.log(JSON.stringify(revenueResult.rows[0], null, 2));
    console.log();
    
    // Check READY orders for restaurant #4
    const readyOrdersResult = await pool.query(`
      SELECT id, status, COALESCE(total_amount, total, 0) as total
      FROM orders
      WHERE restaurant_id = 4 AND status = 'READY'
      ORDER BY id DESC
    `);
    
    console.log(`📋 Đơn READY của nhà hàng #4 (${readyOrdersResult.rows.length} đơn):`);
    readyOrdersResult.rows.forEach(order => {
      console.log(`  - Đơn #${order.id}: ${parseFloat(order.total).toLocaleString()}₫`);
    });
    console.log();
    
    // Check DELIVERED orders for restaurant #4
    const deliveredOrdersResult = await pool.query(`
      SELECT id, status, COALESCE(total_amount, total, 0) as total
      FROM orders
      WHERE restaurant_id = 4 AND status = 'DELIVERED'
      ORDER BY id DESC
      LIMIT 5
    `);
    
    console.log(`✅ Đơn DELIVERED của nhà hàng #4 (mới nhất):`);
    deliveredOrdersResult.rows.forEach(order => {
      console.log(`  - Đơn #${order.id}: ${parseFloat(order.total).toLocaleString()}₫`);
    });
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

checkOrder38();

