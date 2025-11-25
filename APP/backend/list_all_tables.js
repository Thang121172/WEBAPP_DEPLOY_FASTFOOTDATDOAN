require('dotenv').config();
const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.DB_HOST || '127.0.0.1',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'fastfood',
  user: 'app',
  password: '123456'
});

(async () => {
  try {
    const r = await pool.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' 
      AND table_type = 'BASE TABLE' 
      ORDER BY table_name
    `);
    console.log('Danh sách tất cả các bảng trong database fastfood:');
    console.log(`Tổng số: ${r.rows.length} bảng\n`);
    r.rows.forEach((t, i) => {
      const marker = t.table_name === 'users' ? ' <-- BẢNG NÀY!' : '';
      console.log(`${(i + 1).toString().padStart(2, ' ')}. ${t.table_name}${marker}`);
    });
    await pool.end();
  } catch (err) {
    console.error('Error:', err.message);
    await pool.end();
    process.exit(1);
  }
})();

