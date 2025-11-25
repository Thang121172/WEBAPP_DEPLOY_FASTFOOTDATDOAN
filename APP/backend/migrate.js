#!/usr/bin/env node
const { Pool } = require('pg');
require('dotenv').config();

// Mật khẩu và user cần phải khớp
const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = parseInt(process.env.DB_PORT || '5432', 10);
const DB_USER = process.env.DB_USER || 'app';
const DB_PASSWORD = process.env.DB_PASSWORD || '123456';
const TARGET_DB_NAME = process.env.DB_NAME || 'fastfood';

// 1. TẠO POOL ĐỂ KẾT NỐI TỚI DATABASE MẶC ĐỊNH ('postgres')
const initialPool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: 'postgres', // KẾT NỐI TỚI DB MẶC ĐỊNH ĐỂ KIỂM TRA SẴN SÀNG VÀ TẠO DB
  user: DB_USER,
  password: DB_PASSWORD,
});

/**
 * Hàm trợ giúp để thêm cột nếu nó chưa tồn tại.
 * Sử dụng information_schema để kiểm tra, giúp tránh lỗi DDL bên trong transaction.
 */
async function addColumnIfNotExists(client, table, column, type) {
  // Kiểm tra xem cột đã tồn tại chưa bằng cách truy vấn information_schema
  const checkQuery = `
    SELECT 1 FROM information_schema.columns
    WHERE table_name = $1 AND column_name = $2;
  `;
  // Lưu ý: Tên bảng và cột trong information_schema thường là chữ thường
  const res = await client.query(checkQuery, [table.toLowerCase(), column.toLowerCase()]);

  if (res.rowCount === 0) {
    // Cột chưa tồn tại, thực hiện ALTER TABLE
    await client.query(`ALTER TABLE ${table} ADD COLUMN ${column} ${type};`);
    console.log(`Added column ${column} to ${table}`);
  } else {
    // Cột đã tồn tại, không làm gì
    console.log(`Column ${column} already exists in ${table}. Skipping.`);
  }
}

(async () => {
  // --- Wait for Postgres ready (retry) ---
  const maxAttempts = 30;
  const delayMs = 2000;
  let attempt = 0;
  let initialClient;

  while (attempt < maxAttempts) {
    try {
      initialClient = await initialPool.connect();
      await initialClient.query('SELECT 1');
      console.log('Postgres is ready.');
      break;
    } catch (err) {
      attempt++;
      console.log(`Postgres not ready (attempt ${attempt}/${maxAttempts}), retrying in ${delayMs}ms...`);
      if (attempt > 5) {
          console.error("DEBUG ERROR DETAIL:", err.message);
      }
      if (initialClient) initialClient.release();
      await new Promise((res) => setTimeout(res, delayMs));
    }
  }

  if (attempt === maxAttempts) {
    console.error('Postgres did not become ready in time; aborting migrations.');
    process.exit(1);
  }

  // --- 1. TẠO DATABASE 'fastfood' NẾU CHƯA TỒN TẠI ---
  try {
    const res = await initialClient.query(`SELECT 1 FROM pg_database WHERE datname = '${TARGET_DB_NAME}'`);
    if (res.rowCount === 0) {
      await initialClient.query(`CREATE DATABASE ${TARGET_DB_NAME} OWNER ${DB_USER};`);
      console.log(`Database '${TARGET_DB_NAME}' created.`);
    } else {
      console.log(`Database '${TARGET_DB_NAME}' already exists.`);
    }
  } catch (err) {
    console.error('Error creating database:', err.message);
    process.exit(1);
  } finally {
    initialClient.release();
    await initialPool.end();
  }


  // --- 2. KẾT NỐI LẠI VỚI DATABASE 'fastfood' ĐÃ TẠO VÀ CHẠY MIGRATION ---
  const finalPool = new Pool({
    host: DB_HOST,
    port: DB_PORT,
    database: TARGET_DB_NAME,
    user: DB_USER,
    password: DB_PASSWORD,
  });

  // Helper function to add column if not exists
  const addColumnIfNotExists = async (client, tableName, columnName, columnType) => {
    try {
      const checkQuery = `
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = $1 AND column_name = $2
      `;
      const result = await client.query(checkQuery, [tableName.toLowerCase(), columnName.toLowerCase()]);
      if (result.rows.length === 0) {
        // Nếu column type có NOT NULL, thêm DEFAULT trước, sau đó mới set NOT NULL
        const columnTypeStr = columnType.toString();
        if (columnTypeStr.includes('NOT NULL') && !columnTypeStr.includes('DEFAULT')) {
          // Thêm column với DEFAULT trước
          const typeWithoutNotNull = columnTypeStr.replace(/\s*NOT NULL\s*/gi, '');
          await client.query(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${typeWithoutNotNull}`);
          // Set default value nếu cần
          if (typeWithoutNotNull.includes('INTEGER')) {
            await client.query(`UPDATE ${tableName} SET ${columnName} = 0 WHERE ${columnName} IS NULL`);
          } else if (typeWithoutNotNull.includes('VARCHAR') || typeWithoutNotNull.includes('TEXT')) {
            await client.query(`UPDATE ${tableName} SET ${columnName} = '' WHERE ${columnName} IS NULL`);
          }
          // Sau đó mới set NOT NULL
          await client.query(`ALTER TABLE ${tableName} ALTER COLUMN ${columnName} SET NOT NULL`);
        } else {
          await client.query(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${columnType}`);
        }
        console.log(`✅ Added column ${columnName} to ${tableName}`);
      } else {
        console.log(`Column ${columnName} already exists in ${tableName}. Skipping.`);
      }
    } catch (err) {
      // Nếu lỗi là column đã tồn tại, bỏ qua
      if (err.message.includes('already exists') || err.message.includes('duplicate')) {
        console.log(`Column ${columnName} already exists in ${tableName}. Skipping.`);
      } else {
        console.warn(`⚠️ Could not add column ${columnName} to ${tableName}:`, err.message);
      }
    }
  };

  const client = await finalPool.connect();

  try {
    // 1. CHẠY CREATE EXTENSION RIÊNG BIỆT (KHÔNG CÓ TRANSACTION)
    await client.query(`CREATE EXTENSION IF NOT EXISTS "uuid-ossp";`);
    console.log('Extensions checked/created.');

    // 2. BẮT ĐẦU TRANSACTION CHO VIỆC TẠO BẢNG
    await client.query('BEGIN');

    // USERS
    await client.query(`
CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, username VARCHAR(255) UNIQUE NOT NULL, email VARCHAR(255) UNIQUE, phone VARCHAR(50), password TEXT NOT NULL DEFAULT '', name VARCHAR(255) DEFAULT '', role VARCHAR(16) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN','SHOP','MERCHANT','SHIPPER')), verified BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW());
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_users_verified ON users(verified);`);

    // REVOKED TOKENS
    await client.query(`
CREATE TABLE IF NOT EXISTS revoked_tokens (id BIGSERIAL PRIMARY KEY, jti VARCHAR(255) UNIQUE NOT NULL, revoked_at TIMESTAMP NOT NULL DEFAULT NOW());
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_revoked_tokens_revoked_at ON revoked_tokens(revoked_at);`);

    // REFRESH TOKENS
    await client.query(`
CREATE TABLE IF NOT EXISTS refresh_tokens (id BIGSERIAL PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, token TEXT UNIQUE NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), expires_at TIMESTAMP NOT NULL);
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at);`);

    // OTP CODES
    await client.query(`
CREATE TABLE IF NOT EXISTS otp_codes (id BIGSERIAL PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, code VARCHAR(12), expires_at TIMESTAMP NOT NULL, used BOOLEAN NOT NULL DEFAULT false, attempts INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT NOW());
    `.trim());
    // Thêm cột code_hash
    await addColumnIfNotExists(client, 'otp_codes', 'code_hash', 'TEXT');

    await client.query(`CREATE INDEX IF NOT EXISTS idx_otp_codes_user ON otp_codes(user_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_otp_codes_expires ON otp_codes(expires_at);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_otp_codes_used ON otp_codes(used);`);


    // RESTAURANTS
    await client.query(`
CREATE TABLE IF NOT EXISTS restaurants (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, address TEXT, lat NUMERIC(10,6), lng NUMERIC(10,6), rating NUMERIC(4,2) NOT NULL DEFAULT 0, image_url TEXT, created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW());
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_restaurants_coords ON restaurants(lat,lng);`);
    
    // ✅ FIX: Thêm cột image_url nếu chưa có (migration)
    try {
      await client.query(`
        ALTER TABLE restaurants 
        ADD COLUMN IF NOT EXISTS image_url TEXT;
      `);
      console.log('✅ Added image_url column to restaurants table');
    } catch (err) {
      console.log('Note: image_url column may already exist:', err.message);
    }

    // MENU ITEMS
    await client.query(`
CREATE TABLE IF NOT EXISTS menu_items (id SERIAL PRIMARY KEY, restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL, description TEXT, price NUMERIC(12,2) NOT NULL CHECK (price >= 0), created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW());
    `.trim());
    // Thêm cột is_active
    await addColumnIfNotExists(client, 'menu_items', 'is_active', 'BOOLEAN NOT NULL DEFAULT true');

    await client.query(`CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant ON menu_items(restaurant_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_menu_items_active ON menu_items(is_active);`);

    // SHIPPERS
    await client.query(`
CREATE TABLE IF NOT EXISTS shippers (id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE, available BOOLEAN NOT NULL DEFAULT true, lat NUMERIC(10,6), lng NUMERIC(10,6), vehicle_plate VARCHAR(20), updated_at TIMESTAMP NOT NULL DEFAULT NOW());
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_shippers_available ON shippers(available);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_shippers_coords ON shippers(lat,lng);`);
    
    // ✅ FIX: Thêm cột vehicle_plate nếu chưa có
    await addColumnIfNotExists(client, 'shippers', 'vehicle_plate', 'VARCHAR(20)');
    
    // ✅ FIX: Thêm cột phone vào users table nếu chưa có
    await addColumnIfNotExists(client, 'users', 'phone', 'VARCHAR(20)');

    // ORDERS
    await client.query(`
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) UNIQUE NOT NULL,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    restaurant_id INTEGER REFERENCES restaurants(id) ON DELETE SET NULL,
    shipper_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    total NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    shipping_fee NUMERIC(8,2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    delivery_address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PREPARING', 'SHIPPING', 'DELIVERED', 'CANCELED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
    `.trim());

    await client.query(`CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_orders_restaurant ON orders(restaurant_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_orders_shipper ON orders(shipper_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);`);

    // ORDER ITEMS
    await client.query(`
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id INTEGER REFERENCES menu_items(id) ON DELETE SET NULL, -- Nếu menu_item bị xóa, vẫn giữ thông tin sản phẩm đã đặt
    item_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)
);
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_order_items_menu_item ON order_items(menu_item_id);`);

    // ORDER STATUS HISTORY
    await client.query(`
CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    note TEXT
);
    `.trim());
    // Thêm cột created_at nếu chưa có
    await addColumnIfNotExists(client, 'order_status_history', 'created_at', 'TIMESTAMP NOT NULL DEFAULT NOW()');
    await client.query(`CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON order_status_history(order_id);`);
    try {
      await client.query(`CREATE INDEX IF NOT EXISTS idx_order_status_history_created ON order_status_history(created_at);`);
    } catch (e) {
      // Index có thể đã tồn tại hoặc cột chưa có, bỏ qua
      console.log('Note: Could not create index on created_at:', e.message);
    }

    // REVIEWS (UC-11)
    await client.query(`
CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_rating INTEGER NOT NULL DEFAULT 5 CHECK (order_rating >= 1 AND order_rating <= 5),
    merchant_rating INTEGER CHECK (merchant_rating >= 1 AND merchant_rating <= 5),
    shipper_rating INTEGER CHECK (shipper_rating >= 1 AND shipper_rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(order_id, customer_id)
);
    `.trim());
    // Thêm các cột nếu bảng đã tồn tại nhưng thiếu cột
    // Lưu ý: Không thể thêm FOREIGN KEY constraint trong ALTER TABLE ADD COLUMN, cần thêm riêng
    try {
      const checkCol = await client.query(`
        SELECT column_name FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'reviews' AND column_name = 'customer_id'
      `);
      if (checkCol.rows.length === 0) {
        await client.query(`ALTER TABLE reviews ADD COLUMN customer_id INTEGER`);
        await client.query(`UPDATE reviews SET customer_id = (SELECT user_id FROM orders WHERE orders.id = reviews.order_id LIMIT 1) WHERE customer_id IS NULL`);
        await client.query(`ALTER TABLE reviews ALTER COLUMN customer_id SET NOT NULL`);
        await client.query(`ALTER TABLE reviews ADD CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE`);
        console.log('✅ Added customer_id column to reviews table');
      }
    } catch (e) {
      console.log('Note: customer_id column handling:', e.message);
    }
    await addColumnIfNotExists(client, 'reviews', 'order_rating', 'INTEGER NOT NULL DEFAULT 5 CHECK (order_rating >= 1 AND order_rating <= 5)');
    await addColumnIfNotExists(client, 'reviews', 'merchant_rating', 'INTEGER CHECK (merchant_rating >= 1 AND merchant_rating <= 5)');
    await addColumnIfNotExists(client, 'reviews', 'shipper_rating', 'INTEGER CHECK (shipper_rating >= 1 AND shipper_rating <= 5)');
    await addColumnIfNotExists(client, 'reviews', 'comment', 'TEXT');
    await addColumnIfNotExists(client, 'reviews', 'created_at', 'TIMESTAMP NOT NULL DEFAULT NOW()');
    await addColumnIfNotExists(client, 'reviews', 'updated_at', 'TIMESTAMP NOT NULL DEFAULT NOW()');
    // Tạo indexes (sẽ bỏ qua nếu đã tồn tại)
    try {
      await client.query(`CREATE INDEX IF NOT EXISTS idx_reviews_order ON reviews(order_id);`);
      await client.query(`CREATE INDEX IF NOT EXISTS idx_reviews_customer ON reviews(customer_id);`);
    } catch (e) {
      console.log('Note: Could not create reviews indexes:', e.message);
    }

    // MENU ITEM REVIEWS
    await client.query(`
CREATE TABLE IF NOT EXISTS menu_item_reviews (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    order_item_id BIGINT NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL DEFAULT 5 CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(review_id, order_item_id)
);
    `.trim());
    await client.query(`CREATE INDEX IF NOT EXISTS idx_menu_item_reviews_review ON menu_item_reviews(review_id);`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_menu_item_reviews_order_item ON menu_item_reviews(order_item_id);`);

    // COMPLAINTS (UC-13)
    await client.query(`
CREATE TABLE IF NOT EXISTS complaints (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    complaint_type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    response TEXT,
    handled_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
    `.trim());
    // Thêm các cột nếu bảng đã tồn tại nhưng thiếu cột
    // Lưu ý: Không thể thêm FOREIGN KEY constraint trong ALTER TABLE ADD COLUMN, cần thêm riêng
    try {
      const checkCol = await client.query(`
        SELECT column_name FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'complaints' AND column_name = 'customer_id'
      `);
      if (checkCol.rows.length === 0) {
        await client.query(`ALTER TABLE complaints ADD COLUMN customer_id INTEGER`);
        await client.query(`UPDATE complaints SET customer_id = (SELECT user_id FROM orders WHERE orders.id = complaints.order_id LIMIT 1) WHERE customer_id IS NULL`);
        await client.query(`ALTER TABLE complaints ALTER COLUMN customer_id SET NOT NULL`);
        await client.query(`ALTER TABLE complaints ADD CONSTRAINT fk_complaints_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE`);
        console.log('✅ Added customer_id column to complaints table');
      }
    } catch (e) {
      console.log('Note: customer_id column handling:', e.message);
    }
    await addColumnIfNotExists(client, 'complaints', 'complaint_type', 'VARCHAR(32) NOT NULL DEFAULT \'OTHER\'');
    await addColumnIfNotExists(client, 'complaints', 'title', 'VARCHAR(255) NOT NULL');
    await addColumnIfNotExists(client, 'complaints', 'description', 'TEXT NOT NULL');
    await addColumnIfNotExists(client, 'complaints', 'status', 'VARCHAR(32) NOT NULL DEFAULT \'PENDING\'');
    await addColumnIfNotExists(client, 'complaints', 'response', 'TEXT');
    await addColumnIfNotExists(client, 'complaints', 'handled_by', 'INTEGER REFERENCES users(id) ON DELETE SET NULL');
    await addColumnIfNotExists(client, 'complaints', 'resolved_at', 'TIMESTAMP');
    await addColumnIfNotExists(client, 'complaints', 'created_at', 'TIMESTAMP NOT NULL DEFAULT NOW()');
    await addColumnIfNotExists(client, 'complaints', 'updated_at', 'TIMESTAMP NOT NULL DEFAULT NOW()');
    // Tạo indexes (sẽ bỏ qua nếu đã tồn tại)
    try {
      await client.query(`CREATE INDEX IF NOT EXISTS idx_complaints_order ON complaints(order_id);`);
      await client.query(`CREATE INDEX IF NOT EXISTS idx_complaints_customer ON complaints(customer_id);`);
      await client.query(`CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);`);
    } catch (e) {
      console.log('Note: Could not create complaints indexes:', e.message);
    }

    // Thêm cột stock vào menu_items nếu chưa có
    await addColumnIfNotExists(client, 'menu_items', 'stock', 'INTEGER NOT NULL DEFAULT 0');
    await addColumnIfNotExists(client, 'menu_items', 'name', 'VARCHAR(255)');
    // Nếu có title thì copy sang name
    try {
      await client.query(`UPDATE menu_items SET name = title WHERE name IS NULL OR name = '';`);
    } catch (e) {
      console.log('Note: Could not update menu_items name:', e.message);
    }

    // Thêm cột payment_status vào orders nếu chưa có
    await addColumnIfNotExists(client, 'orders', 'payment_status', 'VARCHAR(16) NOT NULL DEFAULT \'UNPAID\'');
    await addColumnIfNotExists(client, 'orders', 'total_amount', 'NUMERIC(12,2)');
    // Copy total sang total_amount nếu chưa có
    try {
      await client.query(`UPDATE orders SET total_amount = total WHERE total_amount IS NULL;`);
    } catch (e) {
      console.log('Note: Could not update orders total_amount:', e.message);
    }

    // Thêm cột note vào orders nếu chưa có
    await addColumnIfNotExists(client, 'orders', 'note', 'TEXT');

    // COMMIT TRANSACTION
    await client.query('COMMIT');
    console.log('All migrations completed successfully.');
  } catch (err) {
    // Nếu có lỗi, ROLLBACK
    console.error('Migration failed. Rolling back changes:', err.message);
    await client.query('ROLLBACK');
    process.exit(1);
  } finally {
    // Luôn luôn giải phóng client
    client.release();
    await finalPool.end();
  }
})();
