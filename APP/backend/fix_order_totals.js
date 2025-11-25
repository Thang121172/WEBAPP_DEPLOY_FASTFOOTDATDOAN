const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function fixOrderTotals() {
  const client = await pool.connect();
  
  try {
    await client.query('BEGIN');
    
    console.log('🔍 Tìm các đơn hàng có total = 0 hoặc NULL...\n');
    
    // Tìm tất cả đơn có total = 0 hoặc NULL
    const ordersResult = await client.query(`
      SELECT 
        o.id,
        o.total,
        o.total_amount,
        COALESCE(SUM(oi.quantity * oi.price), 0) as calculated_total
      FROM orders o
      LEFT JOIN order_items oi ON oi.order_id = o.id
      WHERE (o.total = 0 OR o.total IS NULL OR o.total_amount = 0 OR o.total_amount IS NULL)
        AND EXISTS (SELECT 1 FROM order_items WHERE order_id = o.id)
      GROUP BY o.id, o.total, o.total_amount
      ORDER BY o.id DESC
    `);
    
    if (ordersResult.rows.length === 0) {
      console.log('✅ Không có đơn nào cần sửa!');
      await client.query('COMMIT');
      return;
    }
    
    console.log(`📦 Tìm thấy ${ordersResult.rows.length} đơn hàng cần sửa:\n`);
    
    let fixedCount = 0;
    
    for (const order of ordersResult.rows) {
      const orderId = order.id;
      const currentTotal = parseFloat(order.total) || 0;
      const currentTotalAmount = parseFloat(order.total_amount) || 0;
      const calculatedTotal = parseFloat(order.calculated_total) || 0;
      
      if (calculatedTotal > 0 && calculatedTotal !== currentTotal) {
        console.log(`  📝 Đơn #${orderId}:`);
        console.log(`     - Total hiện tại: ${currentTotal}`);
        console.log(`     - Total tính từ items: ${calculatedTotal}`);
        
        // Update total và total_amount
        await client.query(
          'UPDATE orders SET total = $1, total_amount = $1, updated_at = NOW() WHERE id = $2',
          [calculatedTotal, orderId]
        );
        
        console.log(`     ✅ Đã cập nhật thành ${calculatedTotal}₫\n`);
        fixedCount++;
      } else if (calculatedTotal === 0) {
        console.log(`  ⚠️  Đơn #${orderId}: Không có items hoặc total = 0 (bỏ qua)\n`);
      }
    }
    
    await client.query('COMMIT');
    
    console.log(`\n✅ Hoàn thành! Đã sửa ${fixedCount} đơn hàng.`);
    
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('❌ Lỗi:', err);
    throw err;
  } finally {
    client.release();
    await pool.end();
  }
}

fixOrderTotals().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});

