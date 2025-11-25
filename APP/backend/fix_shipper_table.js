const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function fixShipperTable() {
  try {
    // Check if shipper exists in users table
    const userResult = await pool.query(`
      SELECT id, email, role 
      FROM users 
      WHERE email = 'shipper@gmail.com'
    `);
    
    if (userResult.rows.length === 0) {
      console.log('❌ Không tìm thấy tài khoản shipper@gmail.com trong bảng users');
      await pool.end();
      return;
    }
    
    const user = userResult.rows[0];
    console.log('✅ Tìm thấy user:', JSON.stringify(user, null, 2));
    console.log();
    
    // Check if shipper exists in shippers table
    const shipperResult = await pool.query(`
      SELECT id, available, lat, lng 
      FROM shippers 
      WHERE id = $1
    `, [user.id]);
    
    if (shipperResult.rows.length === 0) {
      console.log('⚠️  Shipper chưa có trong bảng shippers!');
      console.log('🔧 Đang thêm shipper vào bảng shippers...');
      
      // Create shipper record
      await pool.query(`
        INSERT INTO shippers (id, available, lat, lng, updated_at)
        VALUES ($1, $2, $3, $4, NOW())
      `, [user.id, true, null, null]);
      
      console.log('✅ Đã thêm shipper vào bảng shippers!');
      console.log(`   - ID: ${user.id}`);
      console.log(`   - Available: true`);
      console.log(`   - Lat/Lng: null (sẽ cập nhật sau)`);
    } else {
      const shipper = shipperResult.rows[0];
      console.log('✅ Shipper đã có trong bảng shippers:');
      console.log(JSON.stringify(shipper, null, 2));
      
      // Ensure available is true
      if (!shipper.available) {
        console.log('🔧 Đang cập nhật available = true...');
        await pool.query('UPDATE shippers SET available = $1 WHERE id = $2', [true, user.id]);
        console.log('✅ Đã cập nhật available');
      }
    }
    
    // Check available orders
    const ordersResult = await pool.query(`
      SELECT id, status, shipper_id, created_at
      FROM orders
      WHERE shipper_id IS NULL 
        AND status IN ('PENDING', 'CONFIRMED', 'COOKING', 'READY')
      ORDER BY id DESC
      LIMIT 5
    `);
    
    console.log();
    console.log(`📦 Đơn hàng có thể nhận: ${ordersResult.rows.length} đơn`);
    ordersResult.rows.forEach((order, index) => {
      console.log(`   ${index + 1}. Đơn #${order.id}: status = ${order.status}, created = ${order.created_at}`);
    });
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

fixShipperTable();

