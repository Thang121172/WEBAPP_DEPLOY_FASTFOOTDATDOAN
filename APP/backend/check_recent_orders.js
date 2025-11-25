/**
 * Script để kiểm tra đơn hàng gần đây
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || 'fastfood';
const DB_USER = process.env.DB_USER || 'app';
const DB_PASS = process.env.DB_PASSWORD || '123456';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASS,
});

async function checkRecentOrders() {
  try {
    console.log('========================================');
    console.log('  KIỂM TRA ĐƠN HÀNG GẦN ĐÂY');
    console.log('========================================\n');

    const client = await pool.connect();
    
    try {
      // Lấy 10 đơn hàng gần nhất
      console.log('📋 10 ĐƠN HÀNG GẦN NHẤT:\n');
      const recentOrdersQuery = `
        SELECT 
          o.id,
          o.code,
          o.status,
          o.total,
          o.restaurant_id,
          o.created_at,
          o.updated_at,
          r.name as restaurant_name
        FROM orders o
        LEFT JOIN restaurants r ON r.id = o.restaurant_id
        ORDER BY o.created_at DESC
        LIMIT 10
      `;
      
      const recentResult = await client.query(recentOrdersQuery);
      
      if (recentResult.rows.length > 0) {
        recentResult.rows.forEach((order, index) => {
          console.log(`${index + 1}. Đơn #${order.code || order.id}`);
          console.log(`   ID: ${order.id}`);
          console.log(`   Status: ${order.status}`);
          console.log(`   Restaurant: ${order.restaurant_name || order.restaurant_id}`);
          console.log(`   Total: ${order.total || 0}₫`);
          console.log(`   Created: ${order.created_at}`);
          console.log(`   Updated: ${order.updated_at}`);
          console.log('');
        });
      }

      // Đếm đơn theo status
      console.log('📊 THỐNG KÊ ĐƠN HÀNG THEO STATUS:\n');
      const statusCountQuery = `
        SELECT status, COUNT(*) as count
        FROM orders
        GROUP BY status
        ORDER BY count DESC
      `;
      
      const statusResult = await client.query(statusCountQuery);
      
      if (statusResult.rows.length > 0) {
        statusResult.rows.forEach((row) => {
          console.log(`   ${row.status}: ${row.count} đơn`);
        });
        console.log('');
      }

      // Tìm đơn có status READY hoặc DELIVERED
      console.log('✅ ĐƠN HÀNG Ở TRẠNG THÁI READY HOẶC DELIVERED:\n');
      const readyDeliveredQuery = `
        SELECT 
          o.id,
          o.code,
          o.status,
          o.total,
          o.restaurant_id,
          o.created_at,
          o.updated_at,
          r.name as restaurant_name
        FROM orders o
        LEFT JOIN restaurants r ON r.id = o.restaurant_id
        WHERE o.status IN ('READY', 'DELIVERED')
        ORDER BY o.updated_at DESC
        LIMIT 10
      `;
      
      const readyDeliveredResult = await client.query(readyDeliveredQuery);
      
      if (readyDeliveredResult.rows.length > 0) {
        readyDeliveredResult.rows.forEach((order, index) => {
          console.log(`${index + 1}. Đơn #${order.code || order.id} - Status: ${order.status}`);
          console.log(`   Restaurant: ${order.restaurant_name || order.restaurant_id}`);
          console.log(`   Updated: ${order.updated_at}`);
          console.log('');
        });
      } else {
        console.log('   Không có đơn hàng nào ở trạng thái READY hoặc DELIVERED.\n');
      }

    } finally {
      client.release();
    }

  } catch (error) {
    console.error('\n❌ Lỗi:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

// Chạy script
checkRecentOrders();
