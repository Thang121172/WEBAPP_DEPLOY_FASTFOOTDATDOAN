const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = parseInt(process.env.DB_PORT || '5432', 10);
const DB_USER = process.env.DB_USER || 'app';
const DB_PASSWORD = process.env.DB_PASSWORD || '123456';
const TARGET_DB_NAME = process.env.DB_NAME || 'fastfood';

// Tạo Pool để tái sử dụng kết nối
const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: TARGET_DB_NAME,
  user: DB_USER,
  password: DB_PASSWORD,
  max: 20, // Số lượng client tối đa trong pool
  idleTimeoutMillis: 30000, // Thời gian timeout cho client không hoạt động
  connectionTimeoutMillis: 2000, // Thời gian chờ kết nối
});

// Kiểm tra kết nối khi khởi tạo
pool.connect()
  .then(client => {
    console.log(`[db] connected OK => { ok: 1, db: '${TARGET_DB_NAME}', usr: '${DB_USER}' }`);
    client.release();
  })
  .catch(err => {
    console.error('[db] connection error', err.message);
  });

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool,
};