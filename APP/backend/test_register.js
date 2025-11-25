/**
 * Script để test register trực tiếp vào database
 * Usage: node test_register.js
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

async function testRegister() {
  const client = await pool.connect();
  try {
    console.log('=== Testing Register Directly to Database ===\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}`);
    console.log(`User: ${DB_USER}\n`);

    // Test email
    const testEmail = `test_${Date.now()}@gmail.com`;
    const testPassword = '123456';
    const testRole = 'USER';
    
    console.log(`Test email: ${testEmail}`);
    console.log(`Test role: ${testRole}\n`);

    // Hash password
    const hashed = bcrypt.hashSync(testPassword, 10);
    console.log('Password hashed successfully\n');

    // Check existing
    const existingCheck = await client.query(
      'SELECT id, username, email, role, created_at FROM users WHERE username = $1 OR email = $1',
      [testEmail]
    );
    console.log(`Existing users with this email: ${existingCheck.rows.length}`);
    if (existingCheck.rows.length > 0) {
      console.log('Existing user:', existingCheck.rows[0]);
    }
    console.log('');

    // Insert
    console.log('Attempting INSERT...');
    const insertResult = await client.query(
      `INSERT INTO users (username, email, password, role, verified)
       VALUES ($1,$2,$3,$4,$5)
       ON CONFLICT (username)
       DO UPDATE SET
         email    = EXCLUDED.email,
         password = EXCLUDED.password,
         role     = EXCLUDED.role,
         verified = true
       RETURNING id, username, role, email, created_at`,
      [testEmail, testEmail, hashed, testRole, true]
    );

    console.log(`Insert result: ${insertResult.rowCount} row(s) affected`);
    if (insertResult.rows.length > 0) {
      const user = insertResult.rows[0];
      console.log('✅ User inserted successfully:');
      console.log(`   - ID: ${user.id}`);
      console.log(`   - Username: ${user.username}`);
      console.log(`   - Email: ${user.email}`);
      console.log(`   - Role: ${user.role}`);
      console.log(`   - Created At: ${user.created_at}`);
    }
    console.log('');

    // Verify
    console.log('Verifying user in database...');
    const verifyResult = await client.query(
      'SELECT id, username, email, role, created_at FROM users WHERE id = $1',
      [insertResult.rows[0].id]
    );
    
    if (verifyResult.rows.length > 0) {
      console.log('✅ User verified in database:');
      console.log(verifyResult.rows[0]);
    } else {
      console.log('❌ User NOT found in database after insert!');
    }
    console.log('');

    // Check all recent users
    console.log('Recent users in database (last 5):');
    const recentUsers = await client.query(
      'SELECT id, username, email, role, created_at FROM users ORDER BY created_at DESC LIMIT 5'
    );
    recentUsers.rows.forEach((u, i) => {
      console.log(`${i + 1}. ID: ${u.id}, Email: ${u.email}, Role: ${u.role}, Created: ${u.created_at}`);
    });

    await client.query('COMMIT');
    console.log('\n✅ Transaction committed successfully');
    
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('\n❌ Error occurred:');
    console.error('Message:', err.message);
    console.error('Code:', err.code);
    console.error('Detail:', err.detail);
    console.error('Hint:', err.hint);
    console.error('Constraint:', err.constraint);
    console.error('Table:', err.table);
    console.error('Column:', err.column);
    console.error('Stack:', err.stack);
  } finally {
    client.release();
    await pool.end();
  }
}

testRegister();

