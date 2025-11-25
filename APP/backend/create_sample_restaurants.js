/**
 * Script để tạo tài khoản merchant và các cửa hàng mẫu gần Khu công nghiệp Hố Nai
 * Usage: node create_sample_restaurants.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

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

// Tọa độ Khu công nghiệp Hố Nai, Đồng Nai (khoảng)
const BASE_LAT = 10.9500; // Vĩ độ
const BASE_LNG = 106.8500; // Kinh độ

// Danh sách cửa hàng mẫu (mỗi cửa hàng sẽ có 1 tài khoản merchant riêng)
const restaurants = [
  {
    name: "Cơm Tấm Hố Nai",
    email: "merchant.comtam@gmail.com",
    password: "123456",
    address: "123 Đường Quốc Lộ 1A, Khu công nghiệp Hố Nai, Đồng Nai",
    lat: BASE_LAT + 0.001, // Cách khoảng 100m
    lng: BASE_LNG + 0.001,
    rating: 4.5,
    image_url: "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800",
    menu: [
      { name: "Cơm tấm sườn nướng", description: "Cơm tấm với sườn nướng thơm ngon", price: 45000 },
      { name: "Cơm tấm bì chả", description: "Cơm tấm với bì và chả trứng", price: 40000 },
      { name: "Cơm tấm đặc biệt", description: "Cơm tấm đầy đủ: sườn, bì, chả, trứng", price: 55000 },
      { name: "Cơm tấm gà nướng", description: "Cơm tấm với gà nướng mật ong", price: 50000 }
    ]
  },
  {
    name: "Bún Bò Huế Hố Nai",
    email: "merchant.bunbo@gmail.com",
    password: "123456",
    address: "456 Đường Nguyễn Ái Quốc, Khu công nghiệp Hố Nai, Đồng Nai",
    lat: BASE_LAT - 0.002, // Cách khoảng 200m
    lng: BASE_LNG + 0.0015,
    rating: 4.7,
    image_url: "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800",
    menu: [
      { name: "Bún bò Huế đặc biệt", description: "Bún bò Huế với đầy đủ thịt bò, giò heo, chả cua", price: 60000 },
      { name: "Bún bò Huế thường", description: "Bún bò Huế với thịt bò và giò heo", price: 50000 },
      { name: "Bún bò Huế chay", description: "Bún bò Huế chay thơm ngon", price: 40000 },
      { name: "Bún bò Huế tái", description: "Bún bò Huế với thịt bò tái", price: 55000 }
    ]
  },
  {
    name: "Phở Gà Hố Nai",
    email: "merchant.phoga@gmail.com",
    password: "123456",
    address: "789 Đường Trần Phú, Khu công nghiệp Hố Nai, Đồng Nai",
    lat: BASE_LAT + 0.0015,
    lng: BASE_LNG - 0.001, // Cách khoảng 150m
    rating: 4.6,
    image_url: "https://images.unsplash.com/photo-1529042410759-befb1204b468?w=800",
    menu: [
      { name: "Phở gà đặc biệt", description: "Phở gà với đầy đủ thịt gà, trứng, gan", price: 55000 },
      { name: "Phở gà thường", description: "Phở gà với thịt gà", price: 45000 },
      { name: "Phở gà tái", description: "Phở gà với thịt gà tái", price: 50000 },
      { name: "Phở gà nạc", description: "Phở gà với thịt gà nạc", price: 48000 }
    ]
  },
  {
    name: "Bánh Mì Hố Nai",
    email: "merchant.banhmi@gmail.com",
    password: "123456",
    address: "321 Đường Lê Lợi, Khu công nghiệp Hố Nai, Đồng Nai",
    lat: BASE_LAT - 0.001,
    lng: BASE_LNG - 0.002, // Cách khoảng 200m
    rating: 4.4,
    image_url: "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=800",
    menu: [
      { name: "Bánh mì thịt nướng", description: "Bánh mì với thịt nướng thơm ngon", price: 25000 },
      { name: "Bánh mì pate", description: "Bánh mì với pate và thịt nguội", price: 20000 },
      { name: "Bánh mì đặc biệt", description: "Bánh mì đầy đủ: thịt nướng, pate, chả lụa", price: 30000 },
      { name: "Bánh mì chả cá", description: "Bánh mì với chả cá", price: 28000 }
    ]
  }
];

async function createSampleRestaurants() {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    
    console.log('=== Tạo tài khoản Merchant và Cửa hàng mẫu ===\n');
    console.log(`Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    // Tạo bảng user_restaurants nếu chưa có
    try {
      await client.query(`
        CREATE TABLE IF NOT EXISTS user_restaurants (
          id SERIAL PRIMARY KEY,
          user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
          restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
          UNIQUE(user_id, restaurant_id)
        )
      `);
    } catch (err) {
      // Bảng có thể đã tồn tại
    }

    // Tạo các cửa hàng và tài khoản merchant riêng cho mỗi cửa hàng
    console.log('Tạo các cửa hàng và tài khoản merchant...\n');
    
    const createdAccounts = [];
    
    for (let i = 0; i < restaurants.length; i++) {
      const restaurant = restaurants[i];
      console.log(`\n${i + 1}. ${restaurant.name}`);
      console.log(`   Địa chỉ: ${restaurant.address}`);
      console.log(`   Tọa độ: ${restaurant.lat}, ${restaurant.lng}`);
      console.log(`   Rating: ${restaurant.rating}`);

      // 1. Tạo tài khoản merchant riêng cho cửa hàng này
      const merchantEmail = restaurant.email;
      const merchantPassword = restaurant.password;
      const hashedPassword = bcrypt.hashSync(merchantPassword, 10);

      console.log(`   📧 Email: ${merchantEmail}`);
      console.log(`   🔑 Password: ${merchantPassword}`);

      // Kiểm tra xem tài khoản đã tồn tại chưa
      const existingUser = await client.query(
        'SELECT id, username, email, role FROM users WHERE email = $1',
        [merchantEmail]
      );

      let merchantUserId;
      if (existingUser.rows.length > 0) {
        merchantUserId = existingUser.rows[0].id;
        console.log(`   ✅ Tài khoản đã tồn tại: ID ${merchantUserId}`);
      } else {
        const userResult = await client.query(
          `INSERT INTO users (username, email, password, role, verified)
           VALUES ($1, $2, $3, $4, $5)
           RETURNING id, username, email, role`,
          [merchantEmail, merchantEmail, hashedPassword, 'MERCHANT', true]
        );
        merchantUserId = userResult.rows[0].id;
        console.log(`   ✅ Tạo tài khoản mới: ID ${merchantUserId}`);
      }

      // 2. Tạo hoặc cập nhật cửa hàng
      const existingRestaurant = await client.query(
        'SELECT id, name FROM restaurants WHERE name = $1',
        [restaurant.name]
      );

      let restaurantId;
      if (existingRestaurant.rows.length > 0) {
        restaurantId = existingRestaurant.rows[0].id;
        // Cập nhật thông tin cửa hàng
        await client.query(
          `UPDATE restaurants 
           SET address = $1, lat = $2, lng = $3, rating = $4, image_url = $5
           WHERE id = $6`,
          [restaurant.address, restaurant.lat, restaurant.lng, restaurant.rating, restaurant.image_url, restaurantId]
        );
        console.log(`   ✅ Cập nhật cửa hàng: ID ${restaurantId}`);
      } else {
        // Tạo cửa hàng mới
        const restaurantResult = await client.query(
          `INSERT INTO restaurants (name, address, lat, lng, rating, image_url)
           VALUES ($1, $2, $3, $4, $5, $6)
           RETURNING id, name`,
          [restaurant.name, restaurant.address, restaurant.lat, restaurant.lng, restaurant.rating, restaurant.image_url]
        );
        restaurantId = restaurantResult.rows[0].id;
        console.log(`   ✅ Tạo cửa hàng mới: ID ${restaurantId}`);
      }

      // 3. Liên kết cửa hàng với merchant (xóa liên kết cũ nếu có, tạo mới)
      // Xóa tất cả liên kết cũ của cửa hàng này
      await client.query(
        'DELETE FROM user_restaurants WHERE restaurant_id = $1',
        [restaurantId]
      );

      // Tạo liên kết mới với merchant của cửa hàng này
      await client.query(
        'INSERT INTO user_restaurants (user_id, restaurant_id) VALUES ($1, $2) ON CONFLICT (user_id, restaurant_id) DO NOTHING',
        [merchantUserId, restaurantId]
      );
      console.log(`   ✅ Liên kết cửa hàng với tài khoản merchant`);

      // Lưu thông tin tài khoản để in ra cuối cùng
      createdAccounts.push({
        restaurant: restaurant.name,
        email: merchantEmail,
        password: merchantPassword
      });

      // 4. Tạo menu items
      console.log(`   📋 Thêm ${restaurant.menu.length} món ăn:`);
      
      for (const menuItem of restaurant.menu) {
        // Kiểm tra xem món đã tồn tại chưa (trong menu_items)
        const existingItem = await client.query(
          'SELECT id FROM menu_items WHERE restaurant_id = $1 AND title = $2',
          [restaurantId, menuItem.name]
        );

        if (existingItem.rows.length > 0) {
          // Cập nhật món
          await client.query(
            `UPDATE menu_items 
             SET description = $2, price = $3
             WHERE restaurant_id = $1 AND title = $4`,
            [restaurantId, menuItem.description, menuItem.price, menuItem.name]
          );
          console.log(`      - ✅ Cập nhật: ${menuItem.name} (${menuItem.price.toLocaleString('vi-VN')} đ)`);
        } else {
          // Tạo món mới
          await client.query(
            `INSERT INTO menu_items (restaurant_id, title, description, price)
             VALUES ($1, $2, $3, $4)`,
            [restaurantId, menuItem.name, menuItem.description, menuItem.price]
          );
          console.log(`      - ✅ Thêm: ${menuItem.name} (${menuItem.price.toLocaleString('vi-VN')} đ)`);
        }
      }
    }

    await client.query('COMMIT');
    console.log('\n✅ Hoàn thành! Tất cả cửa hàng đã được tạo.\n');
    console.log('📝 Thông tin đăng nhập các tài khoản merchant:\n');
    createdAccounts.forEach((acc, idx) => {
      console.log(`${idx + 1}. ${acc.restaurant}`);
      console.log(`   📧 Email: ${acc.email}`);
      console.log(`   🔑 Password: ${acc.password}`);
      console.log(`   Role: MERCHANT\n`);
    });
    console.log(`📍 Vị trí: Khu công nghiệp Hố Nai, Đồng Nai`);
    console.log(`   Tọa độ gốc: ${BASE_LAT}, ${BASE_LNG}\n`);

  } catch (err) {
    await client.query('ROLLBACK');
    console.error('\n❌ Lỗi:', err.message);
    console.error('Stack:', err.stack);
    throw err;
  } finally {
    client.release();
    await pool.end();
  }
}

createSampleRestaurants().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});

