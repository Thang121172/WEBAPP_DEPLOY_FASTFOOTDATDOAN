/**
 * Script để tạo tài khoản shipper
 * Usage: node create_shipper_account.js [email] [password] [name]
 * 
 * Nếu không truyền tham số, sẽ dùng giá trị mặc định:
 * - Email: shipper@gmail.com
 * - Password: 123456
 * - Name: Shipper
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

async function createShipperAccount() {
  try {
    // Lấy tham số từ command line hoặc dùng giá trị mặc định
    const email = process.argv[2] || 'shipper@gmail.com';
    const password = process.argv[3] || '123456';
    const name = process.argv[4] || 'Shipper';

    console.log('=== Tạo tài khoản Shipper ===\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);
    console.log(`Email: ${email}`);
    console.log(`Password: ${password}`);
    console.log(`Name: ${name}\n`);

    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      // 1. Kiểm tra xem user đã tồn tại chưa
      const userResult = await client.query(
        'SELECT id, username, email, role, verified, password FROM users WHERE email = $1',
        [email]
      );

      let userId;
      if (userResult.rows.length === 0) {
        console.log('📝 Tạo user mới...');
        const hashedPassword = bcrypt.hashSync(password, 10);
        const insertUserResult = await client.query(
          `INSERT INTO users (username, email, password, role, verified)
           VALUES ($1, $2, $3, $4, $5)
           RETURNING id, username, email, role, verified`,
          [email, email, hashedPassword, 'SHIPPER', true]
        );
        userId = insertUserResult.rows[0].id;
        console.log(`✅ Đã tạo user mới: ID ${userId}`);
      } else {
        const user = userResult.rows[0];
        userId = user.id;
        console.log(`✅ Tìm thấy user: ID ${userId}, Role: ${user.role}`);

        // Cập nhật password nếu cần
        if (!user.password || !bcrypt.compareSync(password, user.password)) {
          console.log('📝 Cập nhật password...');
          const hashedPassword = bcrypt.hashSync(password, 10);
          await client.query('UPDATE users SET password = $1 WHERE id = $2', [hashedPassword, userId]);
          console.log('✅ Đã cập nhật password');
        }

        // Đảm bảo role là SHIPPER
        if (user.role !== 'SHIPPER') {
          console.log(`📝 Đổi role từ "${user.role}" sang "SHIPPER"...`);
          await client.query('UPDATE users SET role = $1 WHERE id = $2', ['SHIPPER', userId]);
          console.log('✅ Đã cập nhật role thành SHIPPER');
        }

        // Đảm bảo verified = true
        if (!user.verified) {
          console.log('📝 Cập nhật verified = true...');
          await client.query('UPDATE users SET verified = $1 WHERE id = $2', [true, userId]);
          console.log('✅ Đã cập nhật verified');
        }
      }

      // 2. Kiểm tra xem đã có record trong bảng shippers chưa
      const shipperResult = await client.query(
        'SELECT id, available, lat, lng FROM shippers WHERE id = $1',
        [userId]
      );

      if (shipperResult.rows.length === 0) {
        console.log('📝 Tạo record trong bảng shippers...');
        await client.query(
          `INSERT INTO shippers (id, available, lat, lng, updated_at)
           VALUES ($1, $2, $3, $4, NOW())`,
          [userId, true, null, null] // available = true, lat/lng = null (sẽ cập nhật sau)
        );
        console.log('✅ Đã tạo record trong bảng shippers');
      } else {
        const shipper = shipperResult.rows[0];
        console.log(`✅ Đã có record trong bảng shippers:`);
        console.log(`   - Available: ${shipper.available}`);
        console.log(`   - Lat: ${shipper.lat || 'null'}`);
        console.log(`   - Lng: ${shipper.lng || 'null'}`);
        
        // Đảm bảo available = true
        if (!shipper.available) {
          console.log('📝 Cập nhật available = true...');
          await client.query('UPDATE shippers SET available = $1 WHERE id = $2', [true, userId]);
          console.log('✅ Đã cập nhật available');
        }
      }

      await client.query('COMMIT');
      console.log('\n✅ Transaction committed successfully\n');

      // Hiển thị thông tin đăng nhập
      console.log('📋 Thông tin đăng nhập:');
      console.log(`   Email: ${email}`);
      console.log(`   Password: ${password}`);
      console.log(`   Role: SHIPPER`);
      console.log(`   User ID: ${userId}`);
      console.log(`   Available: true\n`);

    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('❌ Error:', err.message);
    console.error('Stack:', err.stack);
    await pool.end();
    process.exit(1);
  }
}

createShipperAccount();

