const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function fixTotalAmount() {
  const client = await pool.connect();
  
  try {
    await client.query('BEGIN');
    
    console.log('🔍 Tìm các đơn hàng có total_amount = 0 nhưng total > 0...\n');
    
    // Update total_amount = total cho các đơn có total_amount = 0 nhưng total > 0
    const updateResult = await client.query(`
      UPDATE orders 
      SET total_amount = total, updated_at = NOW()
      WHERE (total_amount = 0 OR total_amount IS NULL) 
        AND total > 0
      RETURNING id, total, total_amount, status
    `);
    
    if (updateResult.rows.length === 0) {
      console.log('✅ Không có đơn nào cần sửa!');
      await client.query('COMMIT');
      return;
    }
    
    console.log(`✅ Đã cập nhật ${updateResult.rows.length} đơn hàng:\n`);
    
    updateResult.rows.forEach(order => {
      console.log(`  📦 Đơn #${order.id} (${order.status}):`);
      console.log(`     - Total: ${parseFloat(order.total).toLocaleString()}₫`);
      console.log(`     - Total amount: ${parseFloat(order.total_amount).toLocaleString()}₫\n`);
    });
    
    await client.query('COMMIT');
    
    console.log(`\n✅ Hoàn thành!`);
    
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('❌ Lỗi:', err);
    throw err;
  } finally {
    client.release();
    await pool.end();
  }
}

fixTotalAmount().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});

