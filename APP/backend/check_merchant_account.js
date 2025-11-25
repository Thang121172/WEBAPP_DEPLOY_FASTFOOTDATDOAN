/**
 * Script để kiểm tra tài khoản merchant
 * Usage: node check_merchant_account.js
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

async function checkMerchantAccount() {
  try {
    console.log('=== Kiểm tra tài khoản Merchant ===\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    const email = 'merchant.honai@gmail.com';
    const password = '123456';

    // Tìm user
    const userResult = await pool.query(
      'SELECT id, username, email, role, verified, password FROM users WHERE email = $1',
      [email]
    );

    if (userResult.rows.length === 0) {
      console.log('❌ Không tìm thấy tài khoản với email:', email);
      console.log('\nTạo tài khoản mới...');
      
      const hashedPassword = bcrypt.hashSync(password, 10);
      const insertResult = await pool.query(
        `INSERT INTO users (username, email, password, role, verified)
         VALUES ($1, $2, $3, $4, $5)
         RETURNING id, username, email, role, verified`,
        [email, email, hashedPassword, 'MERCHANT', true]
      );
      
      console.log('✅ Đã tạo tài khoản mới:');
      console.log(insertResult.rows[0]);
      console.log(`\n📧 Email: ${email}`);
      console.log(`🔑 Password: ${password}`);
    } else {
      const user = userResult.rows[0];
      console.log('✅ Tìm thấy tài khoản:');
      console.log(`   - ID: ${user.id}`);
      console.log(`   - Username: ${user.username}`);
      console.log(`   - Email: ${user.email}`);
      console.log(`   - Role: ${user.role}`);
      console.log(`   - Verified: ${user.verified}`);
      console.log(`   - Has Password: ${user.password ? 'Yes' : 'No'}\n`);

      // Kiểm tra password
      if (user.password) {
        const passwordMatch = bcrypt.compareSync(password, user.password);
        console.log(`🔑 Kiểm tra password "${password}": ${passwordMatch ? '✅ ĐÚNG' : '❌ SAI'}`);
        
        if (!passwordMatch) {
          console.log('\n⚠️  Password không khớp. Đang cập nhật password...');
          const newHashedPassword = bcrypt.hashSync(password, 10);
          await pool.query(
            'UPDATE users SET password = $1 WHERE id = $2',
            [newHashedPassword, user.id]
          );
          console.log('✅ Đã cập nhật password mới');
        }
      } else {
        console.log('\n⚠️  Tài khoản chưa có password. Đang tạo password...');
        const hashedPassword = bcrypt.hashSync(password, 10);
        await pool.query(
          'UPDATE users SET password = $1 WHERE id = $2',
          [hashedPassword, user.id]
        );
        console.log('✅ Đã tạo password mới');
      }

      // Đảm bảo role là MERCHANT
      if (user.role !== 'MERCHANT') {
        console.log(`\n⚠️  Role hiện tại là "${user.role}", đang đổi thành MERCHANT...`);
        await pool.query('UPDATE users SET role = $1 WHERE id = $2', ['MERCHANT', user.id]);
        console.log('✅ Đã cập nhật role thành MERCHANT');
      }

      // Đảm bảo verified = true
      if (!user.verified) {
        console.log('\n⚠️  Tài khoản chưa verified. Đang cập nhật...');
        await pool.query('UPDATE users SET verified = $1 WHERE id = $2', [true, user.id]);
        console.log('✅ Đã cập nhật verified = true');
      }

      console.log('\n📝 Thông tin đăng nhập:');
      console.log(`   Email: ${email}`);
      console.log(`   Password: ${password}`);
      console.log(`   Role: MERCHANT`);
    }

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err.message);
    console.error('Stack:', err.stack);
    await pool.end();
    process.exit(1);
  }
}

checkMerchantAccount();

