const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function checkAdminAccount() {
  try {
    const result = await pool.query(`
      SELECT id, username, email, role 
      FROM users 
      WHERE role = 'ADMIN'
      ORDER BY id DESC
      LIMIT 5
    `);
    
    console.log('📋 Tài khoản Admin:\n');
    if (result.rows.length === 0) {
      console.log('❌ Không tìm thấy tài khoản admin nào!');
    } else {
      result.rows.forEach((user, index) => {
        console.log(`${index + 1}. ID: ${user.id}`);
        console.log(`   Username: ${user.username}`);
        console.log(`   Email: ${user.email}`);
        console.log();
      });
      
      // Lấy admin đầu tiên
      const admin = result.rows[0];
      console.log('✅ Thông tin đăng nhập:');
      console.log(`   Email/Username: ${admin.email || admin.username}`);
      console.log(`   Password: (vui lòng kiểm tra script tạo admin hoặc reset lại)`);
    }
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

checkAdminAccount();

