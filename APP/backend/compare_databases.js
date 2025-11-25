/**
 * Script để so sánh database giữa user app và postgres
 * Usage: node compare_databases.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.PGDATABASE || process.env.POSTGRES_DB || 'fastfood';

async function compareDatabases() {
  // Kết nối với user app (như backend)
  const poolApp = new Pool({
    host: DB_HOST,
    port: DB_PORT,
    database: DB_NAME,
    user: 'app',
    password: '123456',
  });

  console.log('=== So sánh Database ===\n');
  
  try {
    // 1. Kiểm tra với user app
    console.log('1. Với user APP (như backend):');
    console.log('   Connection: app@fastfood@127.0.0.1:5432\n');
    
    const dbApp = await poolApp.query('SELECT current_database() as db, current_user as user, version() as version');
    console.log(`   Database: ${dbApp.rows[0].db}`);
    console.log(`   User: ${dbApp.rows[0].user}`);
    console.log(`   PostgreSQL Version: ${dbApp.rows[0].version.split(',')[0]}\n`);
    
    const countApp = await poolApp.query('SELECT COUNT(*) as count FROM users');
    console.log(`   Tổng số users: ${countApp.rows[0].count}`);
    
    const maxIdApp = await poolApp.query('SELECT MAX(id) as max_id FROM users');
    console.log(`   ID lớn nhất: ${maxIdApp.rows[0].max_id || 'N/A'}\n`);
    
    const recentApp = await poolApp.query(`
      SELECT id, username, email, role, created_at 
      FROM users 
      ORDER BY id DESC 
      LIMIT 5
    `);
    console.log('   Top 5 users gần nhất:');
    recentApp.rows.forEach((u, i) => {
      console.log(`     ${i + 1}. ID: ${u.id}, Email: ${u.email}, Created: ${u.created_at}`);
    });
    console.log('');

    // 2. Kiểm tra tất cả các database
    console.log('2. Danh sách tất cả databases:');
    const allDbs = await poolApp.query(`
      SELECT datname 
      FROM pg_database 
      WHERE datistemplate = false 
      ORDER BY datname
    `);
    allDbs.rows.forEach((db, i) => {
      console.log(`   ${i + 1}. ${db.datname}`);
    });
    console.log('');

    // 3. Kiểm tra tất cả các schema trong database fastfood
    console.log('3. Danh sách tất cả schemas trong database fastfood:');
    const allSchemas = await poolApp.query(`
      SELECT schema_name 
      FROM information_schema.schemata 
      WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
      ORDER BY schema_name
    `);
    allSchemas.rows.forEach((schema, i) => {
      console.log(`   ${i + 1}. ${schema.schema_name}`);
    });
    console.log('');

    // 4. Kiểm tra tất cả các bảng users trong tất cả schemas
    console.log('4. Tất cả các bảng "users" trong tất cả schemas:');
    const allUsersTables = await poolApp.query(`
      SELECT table_schema, table_name 
      FROM information_schema.tables 
      WHERE table_name = 'users'
      ORDER BY table_schema, table_name
    `);
    for (const table of allUsersTables.rows) {
      try {
        const count = await poolApp.query(`SELECT COUNT(*) as count FROM "${table.table_schema}"."${table.table_name}"`);
        const maxId = await poolApp.query(`SELECT MAX(id) as max_id FROM "${table.table_schema}"."${table.table_name}"`);
        console.log(`   - ${table.table_schema}.${table.table_name}:`);
        console.log(`     + Số rows: ${count.rows[0].count}`);
        console.log(`     + ID lớn nhất: ${maxId.rows[0].max_id || 'N/A'}`);
      } catch (err) {
        console.log(`   - ${table.table_schema}.${table.table_name}: Error - ${err.message}`);
      }
    }
    console.log('');

    // 5. Kiểm tra xem có user nào có email hoangminhthang12a15@gmail.com không
    console.log('5. Tìm user "hoangminhthang12a15@gmail.com":');
    const findUser = await poolApp.query(`
      SELECT id, username, email, role, created_at 
      FROM users 
      WHERE email = 'hoangminhthang12a15@gmail.com' OR username = 'hoangminhthang12a15@gmail.com'
    `);
    if (findUser.rows.length > 0) {
      console.log('   ✅ Tìm thấy:');
      findUser.rows.forEach(u => {
        console.log(`     - ID: ${u.id}, Email: ${u.email}, Created: ${u.created_at}`);
      });
    } else {
      console.log('   ❌ Không tìm thấy user này trong database với user app');
    }
    console.log('');

    // 6. Kiểm tra ID 49
    console.log('6. Kiểm tra user có ID = 49:');
    const user49 = await poolApp.query('SELECT id, username, email, role, created_at FROM users WHERE id = 49');
    if (user49.rows.length > 0) {
      console.log('   ✅ Tìm thấy:');
      user49.rows.forEach(u => {
        console.log(`     - ID: ${u.id}, Email: ${u.email}, Created: ${u.created_at}`);
      });
    } else {
      console.log('   ❌ Không có user với ID = 49 trong database với user app');
    }

    await poolApp.end();
    
  } catch (err) {
    console.error('Error:', err.message);
    console.error('Code:', err.code);
    await poolApp.end();
    process.exit(1);
  }
}

compareDatabases();

