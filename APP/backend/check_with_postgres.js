/**
 * Script để kiểm tra với user postgres
 * Usage: node check_with_postgres.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.PGDATABASE || process.env.POSTGRES_DB || 'fastfood';

// Thử với user postgres (cần password)
const DB_USER = 'postgres';
const DB_PASS = process.env.POSTGRES_PASSWORD || 'postgres'; // Thay đổi nếu cần

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASS,
});

async function checkWithPostgres() {
  try {
    console.log('=== Checking with user postgres ===\n');
    console.log(`Connection: ${DB_USER}@${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    // Current database
    const dbResult = await pool.query('SELECT current_database() as db, current_user as user');
    console.log('Current Database Info:');
    console.log(`  - Database: ${dbResult.rows[0].db}`);
    console.log(`  - User: ${dbResult.rows[0].user}\n`);

    // Count users
    const countResult = await pool.query('SELECT COUNT(*) as count FROM users');
    console.log(`Total users in 'users' table: ${countResult.rows[0].count}\n`);

    // Recent users
    const recentUsers = await pool.query(`
      SELECT id, username, email, role, created_at 
      FROM users 
      ORDER BY id DESC 
      LIMIT 10
    `);
    console.log('Last 10 users by ID:');
    recentUsers.rows.forEach((u, i) => {
      console.log(`  ${i + 1}. ID: ${u.id}, Email: ${u.email}, Role: ${u.role}, Created: ${u.created_at}`);
    });

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err.message);
    console.error('Code:', err.code);
    if (err.code === '28P01') {
      console.error('\n⚠️  Password authentication failed. Please update DB_PASS in this script.');
    }
    await pool.end();
    process.exit(1);
  }
}

checkWithPostgres();

