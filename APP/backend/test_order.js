/**
 * Test script để test API đặt đơn
 * Usage: node test_order.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.PGDATABASE || process.env.POSTGRES_DB || 'fastfood';
const DB_USER = process.env.DB_USER || process.env.PGUSER || process.env.POSTGRES_USER || 'postgres';
const DB_PASS = process.env.DB_PASSWORD || process.env.PGPASSWORD || process.env.POSTGRES_PASSWORD || 'postgres';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASS,
});

const BASE_URL = process.env.BASE_URL || 'http://localhost:8000';

async function testOrder() {
  try {
    console.log('=== Testing Order Creation ===\n');

    // 1. Tìm hoặc tạo customer user
    console.log('1. Finding or creating customer user...');
    let customerResult = await pool.query(
      "SELECT id, username, email FROM users WHERE role = 'USER' OR role = 'CUSTOMER' LIMIT 1"
    );
    
    let customerId;
    if (customerResult.rows.length === 0) {
      // Tạo customer mới
      const bcrypt = require('bcryptjs');
      const hashed = bcrypt.hashSync('123456', 10);
      const newCustomer = await pool.query(
        `INSERT INTO users (username, email, password, role, verified) 
         VALUES ($1, $2, $3, $4, $5) RETURNING id, username, email`,
        ['testcustomer@gmail.com', 'testcustomer@gmail.com', hashed, 'USER', true]
      );
      customerId = newCustomer.rows[0].id;
      console.log(`   Created customer: ${newCustomer.rows[0].username} (id: ${customerId})`);
    } else {
      customerId = customerResult.rows[0].id;
      console.log(`   Found customer: ${customerResult.rows[0].username} (id: ${customerId})`);
    }

    // 2. Tìm hoặc tạo restaurant
    console.log('\n2. Finding or creating restaurant...');
    let restaurantResult = await pool.query('SELECT id, name FROM restaurants LIMIT 1');
    
    let restaurantId;
    if (restaurantResult.rows.length === 0) {
      const newRestaurant = await pool.query(
        `INSERT INTO restaurants (name, address, rating) 
         VALUES ($1, $2, $3) RETURNING id, name`,
        ['Test Restaurant', '123 Test Street', 4.5]
      );
      restaurantId = newRestaurant.rows[0].id;
      console.log(`   Created restaurant: ${newRestaurant.rows[0].name} (id: ${restaurantId})`);
    } else {
      restaurantId = restaurantResult.rows[0].id;
      console.log(`   Found restaurant: ${restaurantResult.rows[0].name} (id: ${restaurantId})`);
    }

    // 3. Tìm hoặc tạo products
    console.log('\n3. Finding or creating products...');
    let productsResult = await pool.query(
      'SELECT id, name, price FROM products WHERE restaurant_id = $1 LIMIT 2',
      [restaurantId]
    );
    
    let products = [];
    if (productsResult.rows.length === 0) {
      // Tạo 2 products mới
      const product1 = await pool.query(
        `INSERT INTO products (restaurant_id, name, description, price) 
         VALUES ($1, $2, $3, $4) RETURNING id, name, price`,
        [restaurantId, 'Test Product 1', 'Description 1', 50000]
      );
      const product2 = await pool.query(
        `INSERT INTO products (restaurant_id, name, description, price) 
         VALUES ($1, $2, $3, $4) RETURNING id, name, price`,
        [restaurantId, 'Test Product 2', 'Description 2', 30000]
      );
      products = [product1.rows[0], product2.rows[0]];
      console.log(`   Created products: ${products.map(p => `${p.name} (${p.price}đ)`).join(', ')}`);
    } else {
      products = productsResult.rows;
      console.log(`   Found products: ${products.map(p => `${p.name} (${p.price}đ)`).join(', ')}`);
    }

    // 4. Login để lấy token
    console.log('\n4. Logging in to get access token...');
    const jwt = require('jsonwebtoken');
    const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
    
    const userResult = await pool.query('SELECT id, username, role FROM users WHERE id = $1', [customerId]);
    const user = userResult.rows[0];
    
    const accessToken = jwt.sign(
      { id: user.id, username: user.username, role: user.role },
      JWT_SECRET,
      { expiresIn: '24h' }
    );
    console.log(`   Got access token: ${accessToken.substring(0, 20)}...`);

    // 5. Tạo order
    console.log('\n5. Creating order via API...');
    const fetch = require('node-fetch');
    
    const orderPayload = {
      restaurant_id: restaurantId,
      items: [
        { product_id: products[0].id, quantity: 2 },
        { product_id: products[1].id, quantity: 1 }
      ],
      address: '123 Test Address, Test City',
      payment_method: 'COD'
    };

    console.log('   Order payload:', JSON.stringify(orderPayload, null, 2));

    const response = await fetch(`${BASE_URL}/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify(orderPayload)
    });

    const responseData = await response.json();
    
    if (response.ok) {
      console.log('\n✅ Order created successfully!');
      console.log('   Order ID:', responseData.orderId);
      console.log('   Status:', responseData.status);
      console.log('   Total Amount:', responseData.totalAmount);
      console.log('   Created at:', responseData.created_at);
      
      // 6. Kiểm tra order trong database
      console.log('\n6. Verifying order in database...');
      const orderCheck = await pool.query(
        `SELECT o.id, o.status, o.total_amount, o.address, o.payment_method,
                COUNT(oi.id) as item_count
         FROM orders o
         LEFT JOIN order_items oi ON oi.order_id = o.id
         WHERE o.id = $1
         GROUP BY o.id`,
        [responseData.orderId]
      );
      
      if (orderCheck.rows.length > 0) {
        const order = orderCheck.rows[0];
        console.log(`   Order ${order.id}: ${order.status}, ${order.total_amount}đ, ${order.item_count} items`);
        
        // Lấy chi tiết items
        const itemsCheck = await pool.query(
          `SELECT oi.product_id, oi.quantity, oi.price, p.name
           FROM order_items oi
           LEFT JOIN products p ON p.id = oi.product_id
           WHERE oi.order_id = $1`,
          [responseData.orderId]
        );
        console.log('   Items:');
        itemsCheck.rows.forEach(item => {
          console.log(`     - ${item.name}: ${item.quantity} x ${item.price}đ = ${item.quantity * item.price}đ`);
        });
      }
    } else {
      console.log('\n❌ Order creation failed!');
      console.log('   Status:', response.status);
      console.log('   Error:', responseData);
    }

    console.log('\n=== Test completed ===');
    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Test error:', err);
    await pool.end();
    process.exit(1);
  }
}

testOrder();

