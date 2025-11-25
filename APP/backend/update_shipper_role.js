const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function updateShipperRole() {
  try {
    // Check current role
    const checkResult = await pool.query(`
      SELECT id, email, role 
      FROM users 
      WHERE email = 'shipper@gmail.com'
    `);
    
    if (checkResult.rows.length === 0) {
      console.log('❌ Không tìm thấy tài khoản shipper@gmail.com');
      await pool.end();
      return;
    }
    
    const user = checkResult.rows[0];
    console.log('📋 Role hiện tại:', user.role);
    
    if (user.role !== 'SHIPPER') {
      console.log('🔧 Đang cập nhật role từ "' + user.role + '" sang "SHIPPER"...');
      
      const updateResult = await pool.query(`
        UPDATE users 
        SET role = 'SHIPPER' 
        WHERE email = 'shipper@gmail.com' 
        RETURNING id, email, role
      `);
      
      console.log('✅ Đã cập nhật role thành công!');
      console.log(JSON.stringify(updateResult.rows[0], null, 2));
    } else {
      console.log('✅ Role đã đúng là SHIPPER');
    }
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

updateShipperRole();

