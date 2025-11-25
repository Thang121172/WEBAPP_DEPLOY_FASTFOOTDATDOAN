/**
 * Script để tạo tài khoản admin
 * Usage: node create_admin_account.js [email] [password] [name]
 * 
 * Nếu không truyền tham số, sẽ dùng giá trị mặc định:
 * - Email: admin@gmail.com
 * - Password: admin123
 * - Name: Admin User
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

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

async function createAdminAccount() {
  try {
    // Lấy tham số từ command line hoặc dùng giá trị mặc định
    const email = process.argv[2] || 'admin@gmail.com';
    const password = process.argv[3] || 'admin123';
    const name = process.argv[4] || 'Admin User';

    console.log('========================================');
    console.log('  TẠO TÀI KHOẢN ADMIN');
    console.log('========================================\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);
    console.log(`Email: ${email}`);
    console.log(`Password: ${password}`);
    console.log(`Name: ${name}\n`);

    // Validate email
    if (!email || !email.includes('@gmail.com')) {
      console.error('❌ Email phải là địa chỉ Gmail!');
      process.exit(1);
    }

    // Validate password
    if (!password || password.length < 6) {
      console.error('❌ Mật khẩu phải có ít nhất 6 ký tự!');
      process.exit(1);
    }

    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      // 1. Kiểm tra xem user đã tồn tại chưa
      const userResult = await client.query(
        'SELECT id, username, email, role, verified FROM users WHERE email = $1',
        [email]
      );

      let userId;
      if (userResult.rows.length > 0) {
        const user = userResult.rows[0];
        console.log('⚠️  User đã tồn tại! Đang cập nhật thành admin...');
        userId = user.id;
        
        // Cập nhật role thành ADMIN và password mới
        const hashedPassword = bcrypt.hashSync(password, 10);
        await client.query(
          `UPDATE users 
           SET password = $1, role = $2, verified = $3
           WHERE id = $4
           RETURNING id, username, email, role, verified`,
          [hashedPassword, 'ADMIN', true, userId]
        );
        console.log(`✅ Đã cập nhật user thành admin: ID ${userId}`);
      } else {
        console.log('📝 Tạo user mới...');
        const hashedPassword = bcrypt.hashSync(password, 10);
        const insertUserResult = await client.query(
          `INSERT INTO users (username, email, password, role, verified)
           VALUES ($1, $2, $3, $4, $5)
           RETURNING id, username, email, role, verified`,
          [email, email, hashedPassword, 'ADMIN', true]
        );
        userId = insertUserResult.rows[0].id;
        console.log(`✅ Đã tạo user mới: ID ${userId}`);
      }

      await client.query('COMMIT');

      // Lấy thông tin user vừa tạo
      const finalUserResult = await client.query(
        'SELECT id, username, email, role, verified, created_at FROM users WHERE id = $1',
        [userId]
      );

      const finalUser = finalUserResult.rows[0];

      console.log('\n========================================');
      console.log('  ✅ TẠO TÀI KHOẢN THÀNH CÔNG!');
      console.log('========================================');
      console.log(`ID: ${finalUser.id}`);
      console.log(`Email: ${finalUser.email}`);
      console.log(`Username: ${finalUser.username}`);
      console.log(`Role: ${finalUser.role}`);
      console.log(`Verified: ${finalUser.verified}`);
      console.log(`Created At: ${finalUser.created_at}`);
      console.log('\nBạn có thể đăng nhập bằng email và mật khẩu đã nhập.');
      console.log('========================================\n');

    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }

  } catch (error) {
    console.error('\n❌ Lỗi khi tạo tài khoản admin:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

// Chạy script
createAdminAccount();
