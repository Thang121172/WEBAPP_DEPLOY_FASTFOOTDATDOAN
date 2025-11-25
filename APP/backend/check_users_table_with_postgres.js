/**
 * Script để kiểm tra bảng users với user postgres
 * Cần password của postgres
 */

require('dotenv').config();
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || '5432');
const DB_NAME = process.env.DB_NAME || 'fastfood';

// Thử kết nối với postgres (cần password)
// Nếu không được, sẽ thử các password phổ biến
const passwords = [
  process.env.POSTGRES_PASSWORD,
  'postgres',
  '123456',
  'admin',
  ''
].filter(Boolean);

async function checkWithPostgres() {
  for (const password of passwords) {
    const pool = new Pool({
      host: DB_HOST,
      port: DB_PORT,
      database: DB_NAME,
      user: 'postgres',
      password: password,
    });

    try {
      console.log(`\n=== Thử kết nối với user postgres (password: ${password ? '***' : 'empty'}) ===\n`);
      
      // Kiểm tra kết nối
      const dbResult = await pool.query('SELECT current_database() as db, current_user as user');
      console.log(`✅ Kết nối thành công!`);
      console.log(`   Database: ${dbResult.rows[0].db}`);
      console.log(`   User: ${dbResult.rows[0].user}\n`);

      // Đếm số bảng
      const tableCount = await pool.query(`
        SELECT COUNT(*) as count 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_type = 'BASE TABLE'
      `);
      console.log(`Tổng số bảng: ${tableCount.rows[0].count}\n`);

      // Kiểm tra bảng users
      const usersTableCheck = await pool.query(`
        SELECT table_name, table_type 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'users'
      `);
      
      if (usersTableCheck.rows.length > 0) {
        console.log('✅ Bảng "users" TỒN TẠI với user postgres');
        
        // Đếm số users
        const userCount = await pool.query('SELECT COUNT(*) as count FROM users');
        console.log(`   Số users: ${userCount.rows[0].count}`);
        
        // Lấy user mới nhất
        const latestUser = await pool.query(`
          SELECT id, username, email, role, created_at 
          FROM users 
          ORDER BY id DESC 
          LIMIT 1
        `);
        if (latestUser.rows.length > 0) {
          const u = latestUser.rows[0];
          console.log(`   User mới nhất: ID ${u.id}, Email: ${u.email}, Created: ${u.created_at}`);
        }
      } else {
        console.log('❌ Bảng "users" KHÔNG TỒN TẠI với user postgres');
      }

      // Liệt kê tất cả các bảng
      const allTables = await pool.query(`
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_type = 'BASE TABLE' 
        ORDER BY table_name
      `);
      console.log(`\nDanh sách tất cả ${allTables.rows.length} bảng:`);
      allTables.rows.forEach((t, i) => {
        const marker = t.table_name === 'users' ? ' <-- BẢNG USERS!' : '';
        console.log(`${(i + 1).toString().padStart(2, ' ')}. ${t.table_name}${marker}`);
      });

      await pool.end();
      return; // Thành công, thoát
      
    } catch (err) {
      if (err.code === '28P01') {
        console.log(`❌ Password sai, thử password khác...`);
        await pool.end();
        continue;
      } else {
        console.error(`Error: ${err.message}`);
        await pool.end();
        throw err;
      }
    }
  }
  
  console.log('\n❌ Không thể kết nối với user postgres. Vui lòng cung cấp password.');
}

checkWithPostgres().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});

