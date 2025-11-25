/**
 * Script để tìm tài khoản cửa hàng/nhà hàng
 * Usage: node find_restaurant_account.js [search_term]
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

async function findRestaurantAccount() {
  try {
    const searchTerm = process.argv[2] || 'bún bò';
    
    console.log('========================================');
    console.log('  TÌM TÀI KHOẢN CỬA HÀNG');
    console.log('========================================\n');
    console.log(`Từ khóa tìm kiếm: "${searchTerm}"\n`);
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    const client = await pool.connect();
    
    try {
      // Tìm trong bảng restaurants
      console.log('📋 Đang tìm trong bảng restaurants...\n');
      const restaurantQuery = `
        SELECT id, name, address, rating, image_url, lat, lng, created_at
        FROM restaurants
        WHERE LOWER(name) LIKE $1 OR LOWER(address) LIKE $1
        ORDER BY id
      `;
      const restaurantResult = await client.query(restaurantQuery, [`%${searchTerm.toLowerCase()}%`]);
      
      if (restaurantResult.rows.length > 0) {
        console.log(`✅ Tìm thấy ${restaurantResult.rows.length} cửa hàng:\n`);
        restaurantResult.rows.forEach((rest, index) => {
          console.log(`${index + 1}. ID: ${rest.id}`);
          console.log(`   Tên: ${rest.name}`);
          console.log(`   Địa chỉ: ${rest.address || 'N/A'}`);
          console.log(`   Rating: ${rest.rating || 'N/A'}`);
          console.log(`   Created: ${rest.created_at || 'N/A'}`);
          console.log('');
        });
      } else {
        console.log('❌ Không tìm thấy cửa hàng nào trong bảng restaurants.\n');
      }

      // Tìm trong bảng users với role MERCHANT
      console.log('📋 Đang tìm trong tài khoản Merchant...\n');
      const userQuery = `
        SELECT id, username, email, role, verified, created_at
        FROM users
        WHERE role = 'MERCHANT' AND (LOWER(email) LIKE $1 OR LOWER(username) LIKE $1)
        ORDER BY id
      `;
      const userResult = await client.query(userQuery, [`%${searchTerm.toLowerCase()}%`]);
      
      if (userResult.rows.length > 0) {
        console.log(`✅ Tìm thấy ${userResult.rows.length} tài khoản Merchant:\n`);
        userResult.rows.forEach((user, index) => {
          console.log(`${index + 1}. ID: ${user.id}`);
          console.log(`   Email: ${user.email}`);
          console.log(`   Username: ${user.username}`);
          console.log(`   Role: ${user.role}`);
          console.log(`   Verified: ${user.verified}`);
          console.log(`   Created: ${user.created_at || 'N/A'}`);
          console.log('');
        });
      } else {
        console.log('❌ Không tìm thấy tài khoản Merchant nào.\n');
      }

      // Tìm tất cả Merchant để xem danh sách
      console.log('📋 Danh sách tất cả tài khoản Merchant:\n');
      const allMerchantsQuery = `
        SELECT id, username, email, role, verified, created_at
        FROM users
        WHERE role = 'MERCHANT'
        ORDER BY id
      `;
      const allMerchantsResult = await client.query(allMerchantsQuery);
      
      if (allMerchantsResult.rows.length > 0) {
        console.log(`Tổng cộng ${allMerchantsResult.rows.length} tài khoản Merchant:\n`);
        allMerchantsResult.rows.forEach((user, index) => {
          console.log(`${index + 1}. ID: ${user.id} | Email: ${user.email} | Username: ${user.username}`);
        });
        console.log('');
      }

      // Tìm tất cả restaurants
      console.log('📋 Danh sách tất cả cửa hàng:\n');
      const allRestaurantsQuery = `
        SELECT id, name, address, rating, created_at
        FROM restaurants
        ORDER BY id
      `;
      const allRestaurantsResult = await client.query(allRestaurantsQuery);
      
      if (allRestaurantsResult.rows.length > 0) {
        console.log(`Tổng cộng ${allRestaurantsResult.rows.length} cửa hàng:\n`);
        allRestaurantsResult.rows.forEach((rest, index) => {
          console.log(`${index + 1}. ID: ${rest.id} | Tên: ${rest.name} | Địa chỉ: ${rest.address || 'N/A'}`);
        });
        console.log('');
      }

    } finally {
      client.release();
    }

  } catch (error) {
    console.error('\n❌ Lỗi khi tìm kiếm:', error.message);
    console.error(error.stack);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

// Chạy script
findRestaurantAccount();

