/**
 * Script để seed data mẫu vào database (APP backend)
 * Chạy: node seed_data.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const DB_HOST = process.env.DB_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || 'fastfood';
const DB_USER = process.env.DB_USER || 'app';
const DB_PASSWORD = process.env.DB_PASSWORD || '123456';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASSWORD,
});

async function seedData() {
  const client = await pool.connect();
  
  try {
    await client.query('BEGIN');
    console.log('Starting seed data...\n');

    // 1. Create Users
    console.log('1. Creating users...');
    const hashedPassword = await bcrypt.hash('123456', 10);
    const adminPassword = await bcrypt.hash('admin123', 10);

    const users = [
      { username: 'admin', email: 'admin@fastfood.com', password: adminPassword, role: 'ADMIN', name: 'Admin User' },
      { username: 'customer1', email: 'customer1@test.com', password: hashedPassword, role: 'USER', name: 'Nguyễn Văn A' },
      { username: 'customer2', email: 'customer2@test.com', password: hashedPassword, role: 'USER', name: 'Trần Thị B' },
      { username: 'merchant1', email: 'merchant1@test.com', password: hashedPassword, role: 'MERCHANT', name: 'Lê Văn C' },
      { username: 'merchant2', email: 'merchant2@test.com', password: hashedPassword, role: 'MERCHANT', name: 'Phạm Thị D' },
      { username: 'shipper1', email: 'shipper1@test.com', password: hashedPassword, role: 'SHIPPER', name: 'Hoàng Văn E' },
      { username: 'shipper2', email: 'shipper2@test.com', password: hashedPassword, role: 'SHIPPER', name: 'Vũ Thị F' }
    ];

    const userIds = {};
    for (const user of users) {
      const result = await client.query(
        `INSERT INTO users (username, email, password, role, name, verified, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, true, NOW(), NOW())
         ON CONFLICT (username) DO UPDATE SET role = $4
         RETURNING id, username`,
        [user.username, user.email, user.password, user.role, user.name]
      );
      userIds[user.username] = result.rows[0].id;
      console.log(`   Created/Updated user: ${user.username} (ID: ${result.rows[0].id})`);
    }

    // 2. Create Restaurants
    console.log('\n2. Creating restaurants...');
    const restaurants = [
      { name: 'Pizza Hut', address: '123 Nguyễn Huệ, Q1, TP.HCM', lat: 10.7769, lng: 106.7009, rating: 4.5 },
      { name: 'KFC', address: '456 Lê Lợi, Q1, TP.HCM', lat: 10.7719, lng: 106.6989, rating: 4.3 },
      { name: 'McDonald\'s', address: '789 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM', lat: 10.8019, lng: 106.7149, rating: 4.7 }
    ];

    const restaurantIds = {};
    for (const restaurant of restaurants) {
      const result = await client.query(
        `INSERT INTO restaurants (name, address, lat, lng, rating, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
         ON CONFLICT DO NOTHING
         RETURNING id, name`,
        [restaurant.name, restaurant.address, restaurant.lat, restaurant.lng, restaurant.rating]
      );
      if (result.rows.length > 0) {
        restaurantIds[restaurant.name] = result.rows[0].id;
        console.log(`   Created restaurant: ${restaurant.name} (ID: ${result.rows[0].id})`);
      } else {
        // Get existing restaurant
        const existing = await client.query('SELECT id FROM restaurants WHERE name = $1', [restaurant.name]);
        if (existing.rows.length > 0) {
          restaurantIds[restaurant.name] = existing.rows[0].id;
          console.log(`   Restaurant already exists: ${restaurant.name} (ID: ${existing.rows[0].id})`);
        }
      }
    }

    // Link merchants to restaurants
    if (restaurantIds['Pizza Hut'] && userIds['merchant1']) {
      await client.query(
        `INSERT INTO user_restaurants (user_id, restaurant_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
        [userIds['merchant1'], restaurantIds['Pizza Hut']]
      );
    }
    if (restaurantIds['KFC'] && userIds['merchant2']) {
      await client.query(
        `INSERT INTO user_restaurants (user_id, restaurant_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
        [userIds['merchant2'], restaurantIds['KFC']]
      );
    }

    // 3. Create Menu Items
    console.log('\n3. Creating menu items...');
    const menuItems = [
      { restaurant_id: restaurantIds['Pizza Hut'], name: 'Pizza Hải Sản', description: 'Pizza với tôm, mực, cua', price: 199000, stock: 50 },
      { restaurant_id: restaurantIds['Pizza Hut'], name: 'Pizza Thịt Nướng', description: 'Pizza với thịt nướng thơm ngon', price: 179000, stock: 30 },
      { restaurant_id: restaurantIds['Pizza Hut'], name: 'Pizza Phô Mai', description: 'Pizza phô mai đặc biệt', price: 159000, stock: 40 },
      { restaurant_id: restaurantIds['KFC'], name: 'Gà Rán Giòn', description: '2 miếng gà rán giòn', price: 89000, stock: 100 },
      { restaurant_id: restaurantIds['KFC'], name: 'Combo Gà Rán', description: 'Gà rán + khoai tây + nước ngọt', price: 129000, stock: 80 },
      { restaurant_id: restaurantIds['KFC'], name: 'Burger Gà', description: 'Burger gà giòn', price: 69000, stock: 60 },
      { restaurant_id: restaurantIds['McDonald\'s'], name: 'Big Mac', description: 'Burger Big Mac cỡ lớn', price: 99000, stock: 70 },
      { restaurant_id: restaurantIds['McDonald\'s'], name: 'McChicken', description: 'Burger gà McChicken', price: 79000, stock: 50 },
      { restaurant_id: restaurantIds['McDonald\'s'], name: 'Khoai Tây Chiên', description: 'Khoai tây chiên giòn', price: 49000, stock: 200 }
    ];

    const menuItemIds = {};
    for (const item of menuItems) {
      const result = await client.query(
        `INSERT INTO menu_items (restaurant_id, name, description, price, stock, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
         ON CONFLICT DO NOTHING
         RETURNING id, name`,
        [item.restaurant_id, item.name, item.description, item.price, item.stock]
      );
      if (result.rows.length > 0) {
        menuItemIds[item.name] = result.rows[0].id;
        console.log(`   Created menu item: ${item.name} (ID: ${result.rows[0].id})`);
      } else {
        const existing = await client.query('SELECT id FROM menu_items WHERE name = $1 AND restaurant_id = $2', [item.name, item.restaurant_id]);
        if (existing.rows.length > 0) {
          menuItemIds[item.name] = existing.rows[0].id;
        }
      }
    }

    // 4. Create Orders
    console.log('\n4. Creating orders...');
    const orders = [
      {
        user_id: userIds['customer1'],
        restaurant_id: restaurantIds['Pizza Hut'],
        shipper_id: null,
        status: 'DELIVERED',
        payment_status: 'PAID',
        address: '123 Đường ABC, Q1, TP.HCM',
        total: 557000,
        items: [
          { menu_item_id: menuItemIds['Pizza Hải Sản'], quantity: 1 },
          { menu_item_id: menuItemIds['Pizza Thịt Nướng'], quantity: 2 }
        ]
      },
      {
        user_id: userIds['customer2'],
        restaurant_id: restaurantIds['KFC'],
        shipper_id: userIds['shipper1'],
        status: 'DELIVERING',
        payment_status: 'PAID',
        address: '456 Đường XYZ, Q2, TP.HCM',
        total: 307000,
        items: [
          { menu_item_id: menuItemIds['Gà Rán Giòn'], quantity: 2 },
          { menu_item_id: menuItemIds['Combo Gà Rán'], quantity: 1 }
        ]
      },
      {
        user_id: userIds['customer1'],
        restaurant_id: restaurantIds['McDonald\'s'],
        shipper_id: null,
        status: 'CONFIRMED',
        payment_status: 'PAID',
        address: '789 Đường DEF, Q3, TP.HCM',
        total: 197000,
        items: [
          { menu_item_id: menuItemIds['Big Mac'], quantity: 1 },
          { menu_item_id: menuItemIds['Khoai Tây Chiên'], quantity: 2 }
        ]
      },
      {
        user_id: userIds['customer2'],
        restaurant_id: restaurantIds['Pizza Hut'],
        shipper_id: null,
        status: 'PENDING',
        payment_status: 'UNPAID',
        address: '321 Đường GHI, Q4, TP.HCM',
        total: 159000,
        items: [
          { menu_item_id: menuItemIds['Pizza Phô Mai'], quantity: 1 }
        ]
      }
    ];

    const orderIds = {};
    for (const order of orders) {
      const items = order.items;
      delete order.items;

      const result = await client.query(
        `INSERT INTO orders (user_id, restaurant_id, shipper_id, status, payment_status, address, total, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, NOW() - INTERVAL '5 days', NOW())
         RETURNING id`,
        [order.user_id, order.restaurant_id, order.shipper_id, order.status, order.payment_status, order.address, order.total]
      );
      const orderId = result.rows[0].id;
      orderIds[orderId] = orderId;

      // Create order items
      for (const item of items) {
        const menuItem = await client.query('SELECT name, price FROM menu_items WHERE id = $1', [item.menu_item_id]);
        if (menuItem.rows.length > 0) {
          await client.query(
            `INSERT INTO order_items (order_id, menu_item_id, quantity, name_snapshot, price_snapshot, line_total)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [orderId, item.menu_item_id, item.quantity, menuItem.rows[0].name, menuItem.rows[0].price, menuItem.rows[0].price * item.quantity]
          );
        }
      }
      console.log(`   Created order: #${orderId} - Status: ${order.status}`);
    }

    await client.query('COMMIT');
    console.log('\n' + '='.repeat(50));
    console.log('SEEDING COMPLETED!');
    console.log('='.repeat(50));
    console.log('\nTest accounts:');
    console.log('  - Admin: admin / admin123');
    console.log('  - Customer: customer1 / 123456');
    console.log('  - Merchant: merchant1 / 123456');
    console.log('  - Shipper: shipper1 / 123456');

  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error seeding data:', error);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

seedData().catch(console.error);

