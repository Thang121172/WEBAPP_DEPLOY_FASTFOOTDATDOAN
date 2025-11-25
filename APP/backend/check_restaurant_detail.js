/**
 * Script để kiểm tra chi tiết tài khoản cửa hàng và restaurant
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

async function checkRestaurantDetail() {
  try {
    const restaurantId = 4; // ID của Bún Bò Huế Hố Nai
    const userId = 42; // User ID của merchant.bunbo@gmail.com
    
    console.log('========================================');
    console.log('  CHI TIẾT TÀI KHOẢN CỬA HÀNG');
    console.log('  BÚN BÒ HUẾ HỐ NAI');
    console.log('========================================\n');

    const client = await pool.connect();
    
    try {
      // Thông tin user/merchant
      console.log('👤 THÔNG TIN TÀI KHOẢN MERCHANT:\n');
      const userResult = await client.query(
        'SELECT id, username, email, role, verified, created_at FROM users WHERE id = $1',
        [userId]
      );
      
      if (userResult.rows.length > 0) {
        const user = userResult.rows[0];
        console.log(`ID: ${user.id}`);
        console.log(`Email: ${user.email}`);
        console.log(`Username: ${user.username}`);
        console.log(`Role: ${user.role}`);
        console.log(`Verified: ${user.verified}`);
        console.log(`Created: ${user.created_at}`);
        console.log('');
      }

      // Thông tin restaurant
      console.log('🏪 THÔNG TIN CỬA HÀNG:\n');
      const restaurantResult = await client.query(
        'SELECT id, name, address, rating, image_url, lat, lng, created_at FROM restaurants WHERE id = $1',
        [restaurantId]
      );
      
      if (restaurantResult.rows.length > 0) {
        const rest = restaurantResult.rows[0];
        console.log(`ID: ${rest.id}`);
        console.log(`Tên: ${rest.name}`);
        console.log(`Địa chỉ: ${rest.address || 'N/A'}`);
        console.log(`Rating: ${rest.rating || 'N/A'}`);
        console.log(`Lat: ${rest.lat || 'N/A'}`);
        console.log(`Lng: ${rest.lng || 'N/A'}`);
        console.log(`Image URL: ${rest.image_url || 'N/A'}`);
        console.log(`Created: ${rest.created_at || 'N/A'}`);
        console.log('');
      }

      // Kiểm tra liên kết user-restaurant
      console.log('🔗 LIÊN KẾT USER - RESTAURANT:\n');
      try {
        const linkResult = await client.query(
          'SELECT * FROM user_restaurants WHERE user_id = $1 OR restaurant_id = $2',
          [userId, restaurantId]
        );
        
        if (linkResult.rows.length > 0) {
          console.log('Tìm thấy liên kết:');
          linkResult.rows.forEach((link, index) => {
            console.log(`${index + 1}. User ID: ${link.user_id}, Restaurant ID: ${link.restaurant_id}`);
          });
        } else {
          console.log('Không có bảng user_restaurants hoặc chưa có liên kết.');
        }
        console.log('');
      } catch (e) {
        console.log('Bảng user_restaurants không tồn tại (điều này là bình thường).');
        console.log('');
      }

      // Menu items
      console.log('📋 MENU CỦA CỬA HÀNG:\n');
      const menuResult = await client.query(
        'SELECT id, name, description, price, image_url, available FROM menu_items WHERE restaurant_id = $1 ORDER BY id',
        [restaurantId]
      );
      
      if (menuResult.rows.length > 0) {
        console.log(`Tổng cộng ${menuResult.rows.length} món:\n`);
        menuResult.rows.forEach((item, index) => {
          console.log(`${index + 1}. ${item.name}`);
          console.log(`   Mô tả: ${item.description || 'N/A'}`);
          console.log(`   Giá: ${parseInt(item.price) || 0}₫`);
          console.log(`   Available: ${item.available !== false ? 'Có' : 'Không'}`);
          console.log('');
        });
      } else {
        console.log('Chưa có menu items.');
        console.log('');
      }

      // Tóm tắt
      console.log('========================================');
      console.log('  📝 TÓM TẮT ĐĂNG NHẬP');
      console.log('========================================');
      console.log(`Email: merchant.bunbo@gmail.com`);
      console.log(`Password: 123456 (mặc định từ script)`);
      console.log(`Restaurant ID: ${restaurantId}`);
      console.log(`Restaurant Name: Bún Bò Huế Hố Nai`);
      console.log('========================================\n');

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
checkRestaurantDetail();

