/**
 * Script để kiểm tra users trong database
 * Usage: node check_users.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

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

async function checkUsers() {
  try {
    console.log('=== Checking Users in Database ===\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}`);
    console.log(`User: ${DB_USER}\n`);

    // Kiểm tra xem bảng users có tồn tại không
    const tableCheck = await pool.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'users'
      );
    `);
    
    if (!tableCheck.rows[0].exists) {
      console.log('❌ Bảng "users" không tồn tại trong database!');
      await pool.end();
      process.exit(1);
    }
    
    console.log('✅ Bảng "users" tồn tại\n');

    // Đếm số lượng users
    const countResult = await pool.query('SELECT COUNT(*) as count FROM users');
    console.log(`Tổng số users: ${countResult.rows[0].count}\n`);

    // Lấy danh sách users
    const usersResult = await pool.query(`
      SELECT id, username, email, role, verified, created_at 
      FROM users 
      ORDER BY created_at DESC 
      LIMIT 20
    `);

    if (usersResult.rows.length === 0) {
      console.log('⚠️  Không có user nào trong database!');
    } else {
      console.log(`Danh sách ${usersResult.rows.length} users gần nhất:\n`);
      console.log('ID\tUsername\t\t\tEmail\t\t\t\tRole\t\tVerified\tCreated At');
      console.log('-'.repeat(120));
      usersResult.rows.forEach(user => {
        console.log(`${user.id}\t${user.username}\t\t${user.email || 'N/A'}\t\t${user.role}\t\t${user.verified}\t\t${user.created_at}`);
      });
    }

    // Kiểm tra các bảng khác
    console.log('\n=== Checking Other Tables ===\n');
    const tablesResult = await pool.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' 
      AND table_type = 'BASE TABLE'
      ORDER BY table_name
    `);
    
    console.log('Các bảng trong database:');
    tablesResult.rows.forEach(row => {
      console.log(`  - ${row.table_name}`);
    });

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err);
    await pool.end();
    process.exit(1);
  }
}

checkUsers();

