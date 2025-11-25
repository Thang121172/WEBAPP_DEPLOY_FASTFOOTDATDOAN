/**
 * Script để kiểm tra views và connections
 * Usage: node check_views_and_connections.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.PGDATABASE || process.env.POSTGRES_DB || 'fastfood';

const poolApp = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: 'app',
  password: '123456',
});

async function checkViewsAndConnections() {
  try {
    console.log('=== Kiểm tra Views và Connections ===\n');
    
    // 1. Kiểm tra views
    console.log('1. Tất cả các views trong schema public:');
    const views = await poolApp.query(`
      SELECT table_name, view_definition 
      FROM information_schema.views 
      WHERE table_schema = 'public'
      ORDER BY table_name
    `);
    if (views.rows.length > 0) {
      views.rows.forEach(v => {
        console.log(`   - ${v.table_name}`);
        if (v.table_name.toLowerCase().includes('user')) {
          console.log(`     Definition: ${v.view_definition.substring(0, 200)}...`);
        }
      });
    } else {
      console.log('   Không có views nào');
    }
    console.log('');

    // 2. Kiểm tra materialized views
    console.log('2. Tất cả các materialized views:');
    const matViews = await poolApp.query(`
      SELECT schemaname, matviewname 
      FROM pg_matviews 
      WHERE schemaname = 'public'
      ORDER BY matviewname
    `);
    if (matViews.rows.length > 0) {
      matViews.rows.forEach(mv => {
        console.log(`   - ${mv.schemaname}.${mv.matviewname}`);
      });
    } else {
      console.log('   Không có materialized views nào');
    }
    console.log('');

    // 3. Kiểm tra tất cả các bảng và views có tên chứa "user"
    console.log('3. Tất cả các bảng/views có tên chứa "user":');
    const allUserTables = await poolApp.query(`
      SELECT table_schema, table_name, table_type 
      FROM information_schema.tables 
      WHERE table_schema = 'public' 
      AND table_name ILIKE '%user%'
      ORDER BY table_type, table_name
    `);
    allUserTables.rows.forEach(t => {
      console.log(`   - ${t.table_schema}.${t.table_name} (${t.table_type})`);
    });
    console.log('');

    // 4. Kiểm tra sequence của users table
    console.log('4. Sequence của bảng users:');
    const sequence = await poolApp.query(`
      SELECT column_name, column_default 
      FROM information_schema.columns 
      WHERE table_schema = 'public' 
      AND table_name = 'users' 
      AND column_name = 'id'
    `);
    if (sequence.rows.length > 0) {
      console.log(`   Default: ${sequence.rows[0].column_default}`);
    }
    
    // Lấy giá trị hiện tại của sequence
    const seqValue = await poolApp.query("SELECT last_value FROM users_id_seq");
    console.log(`   Sequence last_value: ${seqValue.rows[0].last_value}`);
    console.log('');

    // 5. Kiểm tra xem có connection nào đang active không
    console.log('5. Active connections:');
    const connections = await poolApp.query(`
      SELECT pid, usename, datname, application_name, client_addr, state 
      FROM pg_stat_activity 
      WHERE datname = 'fastfood'
      ORDER BY pid
    `);
    connections.rows.forEach(conn => {
      console.log(`   - PID: ${conn.pid}, User: ${conn.usename}, DB: ${conn.datname}, State: ${conn.state}`);
    });
    console.log('');

    // 6. Kiểm tra xem có database nào khác tên "fastfood" không
    console.log('6. Tất cả databases (để kiểm tra duplicate):');
    const allDbs = await poolApp.query(`
      SELECT datname, datdba::regrole as owner 
      FROM pg_database 
      WHERE datistemplate = false 
      ORDER BY datname
    `);
    allDbs.rows.forEach(db => {
      console.log(`   - ${db.datname} (owner: ${db.owner})`);
    });

    await poolApp.end();
    
  } catch (err) {
    console.error('Error:', err.message);
    await poolApp.end();
    process.exit(1);
  }
}

checkViewsAndConnections();

