require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.POSTGRES_DB || process.env.PGDATABASE || 'fastfood';
const DB_USER = process.env.DB_USER || process.env.PGUSER || process.env.POSTGRES_USER || 'app';
const DB_PASSWORD = process.env.DB_PASSWORD || process.env.PGPASSWORD || process.env.POSTGRES_PASSWORD || '123456';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASSWORD,
});

(async () => {
  try {
    const email = 'hoangminhthang12a15@gmail.com';
    console.log(`Checking user: ${email}`);
    
    const r = await pool.query(
      `SELECT id, username, email, role, 
       CASE WHEN password IS NULL OR password = '' THEN false ELSE true END as has_password, 
       verified 
       FROM users WHERE email = $1`,
      [email]
    );
    
    if (r.rows.length === 0) {
      console.log('❌ User not found in database');
    } else {
      console.log('✅ User found:');
      console.log(JSON.stringify(r.rows[0], null, 2));
      
      if (!r.rows[0].has_password) {
        console.log('\n⚠️  User has no password set. User needs to register with password or reset password.');
      }
    }
    
    await pool.end();
  } catch (e) {
    console.error('Error:', e);
    process.exit(1);
  }
})();

