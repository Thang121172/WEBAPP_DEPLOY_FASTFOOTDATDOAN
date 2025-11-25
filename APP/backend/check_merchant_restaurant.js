/**
 * Script để kiểm tra restaurant_id của merchant
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

async function checkMerchantRestaurant() {
  try {
    const merchantEmail = process.argv[2] || 'merchant.bunbo@gmail.com';
    
    console.log('========================================');
    console.log('  KIỂM TRA RESTAURANT CỦA MERCHANT');
    console.log('========================================\n');
    console.log(`Merchant email: ${merchantEmail}\n`);

    const client = await pool.connect();
    
    try {
      // Tìm merchant
      const userResult = await client.query(
        'SELECT id, username, email, role FROM users WHERE email = $1',
        [merchantEmail]
      );
      
      if (userResult.rows.length === 0) {
        console.log('❌ Không tìm thấy merchant với email này!');
        return;
      }
      
      const merchant = userResult.rows[0];
      console.log(`✅ Tìm thấy merchant:`);
      console.log(`   ID: ${merchant.id}`);
      console.log(`   Email: ${merchant.email}`);
      console.log(`   Role: ${merchant.role}\n`);
      
      // Tìm restaurant của merchant
      console.log('🔍 Tìm restaurant của merchant:\n');
      
      // Cách 1: Restaurant có id = user_id
      const rest1Result = await client.query(
        'SELECT id, name, address FROM restaurants WHERE id = $1',
        [merchant.id]
      );
      
      if (rest1Result.rows.length > 0) {
        const rest = rest1Result.rows[0];
        console.log(`✅ Restaurant (id = user_id):`);
        console.log(`   ID: ${rest.id}`);
        console.log(`   Tên: ${rest.name}`);
        console.log(`   Địa chỉ: ${rest.address || 'N/A'}\n`);
      }
      
      // Cách 2: Tìm trong bảng user_restaurants
      try {
        const userRestResult = await client.query(
          'SELECT restaurant_id FROM user_restaurants WHERE user_id = $1',
          [merchant.id]
        );
        
        if (userRestResult.rows.length > 0) {
          const restIds = userRestResult.rows.map(r => r.restaurant_id);
          console.log(`✅ Restaurant từ user_restaurants: ${restIds.join(', ')}\n`);
          
          for (const restId of restIds) {
            const restResult = await client.query(
              'SELECT id, name, address FROM restaurants WHERE id = $1',
              [restId]
            );
            if (restResult.rows.length > 0) {
              const rest = restResult.rows[0];
              console.log(`   - ID: ${rest.id}, Tên: ${rest.name}`);
            }
          }
          console.log('');
        }
      } catch (e) {
        console.log('⚠️  Bảng user_restaurants không tồn tại.\n');
      }
      
      // Kiểm tra đơn hàng của restaurant "Bún Bò Huế Hố Nai"
      console.log('📋 ĐƠN HÀNG CỦA RESTAURANT "BÚN BÒ HUẾ HỐ NAI":\n');
      const restaurantQuery = await client.query(
        'SELECT id, name FROM restaurants WHERE LOWER(name) LIKE $1',
        ['%bún bò%']
      );
      
      if (restaurantQuery.rows.length > 0) {
        const restaurant = restaurantQuery.rows[0];
        console.log(`Restaurant ID: ${restaurant.id}, Tên: ${restaurant.name}\n`);
        
        // Lấy đơn hàng READY hoặc DELIVERED của restaurant này
        const ordersQuery = `
          SELECT id, code, status, total, created_at, updated_at
          FROM orders
          WHERE restaurant_id = $1
            AND status IN ('READY', 'DELIVERED')
          ORDER BY updated_at DESC
          LIMIT 10
        `;
        
        const ordersResult = await client.query(ordersQuery, [restaurant.id]);
        
        if (ordersResult.rows.length > 0) {
          console.log(`Tìm thấy ${ordersResult.rows.length} đơn hàng:\n`);
          ordersResult.rows.forEach((order, index) => {
            console.log(`${index + 1}. Đơn #${order.code || order.id}`);
            console.log(`   Status: ${order.status}`);
            console.log(`   Total: ${order.total || 0}₫`);
            console.log(`   Updated: ${order.updated_at}`);
            console.log('');
          });
        } else {
          console.log('❌ Không có đơn hàng nào ở trạng thái READY hoặc DELIVERED.\n');
        }
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
checkMerchantRestaurant();

