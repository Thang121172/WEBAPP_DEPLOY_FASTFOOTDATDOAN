const { Pool } = require('pg');

const pool = new Pool({
  host: '127.0.0.1',
  port: 5432,
  database: 'fastfood',
  user: 'app',
  password: '123456'
});

async function checkUsersColumns() {
  try {
    const result = await pool.query(`
      SELECT column_name 
      FROM information_schema.columns 
      WHERE table_name = 'users' 
        AND column_name LIKE '%name%'
      ORDER BY column_name
    `);
    
    console.log('Cột name trong bảng users:');
    result.rows.forEach(row => {
      console.log(`  - ${row.column_name}`);
    });
    
    if (result.rows.length === 0) {
      console.log('  (Không có cột nào chứa "name")');
    }
    
  } catch (err) {
    console.error('❌ Lỗi:', err);
  } finally {
    await pool.end();
  }
}

checkUsersColumns();

