const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function resetAdminPassword() {
  try {
    // Tìm admin account
    const result = await pool.query(`
      SELECT id, username, email, role 
      FROM users 
      WHERE role = 'ADMIN'
      ORDER BY id DESC
      LIMIT 1
    `);
    
    if (result.rows.length === 0) {
      console.log('❌ Không tìm thấy tài khoản admin!');
      await pool.end();
      return;
    }
    
    const admin = result.rows[0];
    
    // Set password mặc định: admin123
    const newPassword = 'admin123';
    const hashedPassword = bcrypt.hashSync(newPassword, 10);
    
    await pool.query(
      'UPDATE users SET password = $1 WHERE id = $2',
      [hashedPassword, admin.id]
    );
    
    console.log('✅ Đã đặt lại mật khẩu admin!\n');
    console.log('📝 Thông tin đăng nhập:');
    console.log(`   Email/Username: ${admin.email || admin.username}`);
    console.log(`   Password: ${newPassword}\n`);
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

resetAdminPassword();

