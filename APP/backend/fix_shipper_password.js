const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function fixShipperPassword() {
  try {
    // Check and fix shipper account
    const result = await pool.query(`
      SELECT id, email, username, role, 
             password IS NULL as is_null,
             password = '' as is_empty,
             CASE WHEN password IS NULL THEN 0 
                  WHEN password = '' THEN 0 
                  ELSE LENGTH(password) END as pwd_length
      FROM users 
      WHERE email = 'shipper@gmail.com' OR username = 'shipper@gmail.com'
    `);
    
    if (result.rows.length === 0) {
      console.log('❌ Không tìm thấy tài khoản shipper@gmail.com');
      await pool.end();
      return;
    }
    
    const user = result.rows[0];
    console.log('📋 Thông tin tài khoản hiện tại:');
    console.log(JSON.stringify(user, null, 2));
    console.log();
    
    // Check if password is null or empty
    if (user.is_null || user.is_empty || user.pwd_length === 0) {
      console.log('⚠️  Mật khẩu đang trống hoặc rỗng!');
      console.log('🔧 Đang tạo mật khẩu mặc định: "123456"');
      
      const hashedPassword = bcrypt.hashSync('123456', 10);
      
      await pool.query(
        'UPDATE users SET password = $1 WHERE id = $2',
        [hashedPassword, user.id]
      );
      
      console.log('✅ Đã cập nhật mật khẩu thành công!');
      console.log();
      console.log('📝 Thông tin đăng nhập:');
      console.log('   Email: shipper@gmail.com');
      console.log('   Mật khẩu: 123456');
      console.log('   Role: ' + user.role);
    } else {
      console.log('✅ Tài khoản đã có mật khẩu (độ dài: ' + user.pwd_length + ' ký tự)');
      console.log('   Nếu quên mật khẩu, bạn có thể reset lại bằng cách chạy script này');
    }
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

fixShipperPassword();

