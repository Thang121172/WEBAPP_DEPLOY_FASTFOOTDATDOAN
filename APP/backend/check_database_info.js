/**
 * Script để kiểm tra thông tin database
 * Usage: node check_database_info.js
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

async function checkDatabaseInfo() {
  try {
    console.log('=== Database Information ===\n');
    console.log(`Connection: ${DB_USER}@${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    // Current database
    const dbResult = await pool.query('SELECT current_database() as db, current_user as user, current_schema() as schema');
    console.log('Current Database Info:');
    console.log(`  - Database: ${dbResult.rows[0].db}`);
    console.log(`  - User: ${dbResult.rows[0].user}`);
    console.log(`  - Schema: ${dbResult.rows[0].schema}\n`);

    // Count users
    const countResult = await pool.query('SELECT COUNT(*) as count FROM users');
    console.log(`Total users in 'users' table: ${countResult.rows[0].count}\n`);

    // Check all schemas
    const schemasResult = await pool.query(`
      SELECT schema_name 
      FROM information_schema.schemata 
      WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
      ORDER BY schema_name
    `);
    console.log('Available schemas:');
    schemasResult.rows.forEach(row => {
      console.log(`  - ${row.schema_name}`);
    });
    console.log('');

    // Check users table in all schemas
    const tablesResult = await pool.query(`
      SELECT table_schema, table_name 
      FROM information_schema.tables 
      WHERE table_name = 'users'
      ORDER BY table_schema, table_name
    `);
    console.log("Tables named 'users' in all schemas:");
    for (const row of tablesResult.rows) {
      const countResult = await pool.query(`SELECT COUNT(*) as count FROM "${row.table_schema}"."${row.table_name}"`);
      console.log(`  - ${row.table_schema}.${row.table_name}: ${countResult.rows[0].count} rows`);
    }
    console.log('');

    // Recent users
    const recentUsers = await pool.query(`
      SELECT id, username, email, role, created_at 
      FROM users 
      ORDER BY created_at DESC 
      LIMIT 5
    `);
    console.log('Recent 5 users:');
    recentUsers.rows.forEach((u, i) => {
      console.log(`  ${i + 1}. ID: ${u.id}, Email: ${u.email}, Role: ${u.role}, Created: ${u.created_at}`);
    });

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err);
    await pool.end();
    process.exit(1);
  }
}

checkDatabaseInfo();

