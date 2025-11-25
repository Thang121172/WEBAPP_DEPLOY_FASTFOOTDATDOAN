# Hướng dẫn chạy Migrations và Seed Data

## WEB Backend (Django)

### 1. Activate Virtual Environment
```bash
cd WEB/backend
# Windows
.\venv\Scripts\activate
# Linux/Mac
source venv/bin/activate
```

### 2. Install Dependencies (nếu chưa có)
```bash
pip install -r requirements.txt
```

### 3. Chạy Migrations
```bash
python manage.py makemigrations orders
python manage.py migrate
```

### 4. Chạy Seed Data
```bash
python manage.py shell < scripts/seed_data.py
```

Hoặc:
```bash
python scripts/seed_data.py
```

## APP Backend (Node.js)

### 1. Cập nhật Database Schema

Cần thêm các bảng mới vào `APP/backend/migrate.js`. Thêm vào phần tạo bảng:

```javascript
// Reviews table
await client.query(`
CREATE TABLE IF NOT EXISTS reviews (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  customer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  order_rating INTEGER NOT NULL DEFAULT 5 CHECK (order_rating >= 1 AND order_rating <= 5),
  merchant_rating INTEGER CHECK (merchant_rating >= 1 AND merchant_rating <= 5),
  shipper_rating INTEGER CHECK (shipper_rating >= 1 AND shipper_rating <= 5),
  comment TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE(order_id, customer_id)
);
`);

// Menu item reviews
await client.query(`
CREATE TABLE IF NOT EXISTS menu_item_reviews (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
  order_item_id INTEGER NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
  rating INTEGER NOT NULL DEFAULT 5 CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE(review_id, order_item_id)
);
`);

// Complaints table
await client.query(`
CREATE TABLE IF NOT EXISTS complaints (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
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
`);

// Indexes
await client.query('CREATE INDEX IF NOT EXISTS idx_reviews_order ON reviews(order_id);');
await client.query('CREATE INDEX IF NOT EXISTS idx_reviews_customer ON reviews(customer_id);');
await client.query('CREATE INDEX IF NOT EXISTS idx_complaints_order ON complaints(order_id);');
await client.query('CREATE INDEX IF NOT EXISTS idx_complaints_customer ON complaints(customer_id);');
await client.query('CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);');
```

### 2. Chạy Migration
```bash
cd APP/backend
node migrate.js
```

### 3. Chạy Seed Data
```bash
node seed_data.js
```

## Kiểm tra

Sau khi chạy migrations và seed data, kiểm tra:

1. **WEB Backend**: Truy cập Django admin để xem data
2. **APP Backend**: Kiểm tra database bằng pgAdmin hoặc psql
3. **Test API**: Sử dụng Postman hoặc curl để test các endpoints mới

## Troubleshooting

### Lỗi "ModuleNotFoundError: No module named 'django'"
- Activate virtual environment trước
- Cài đặt dependencies: `pip install -r requirements.txt`

### Lỗi "relation does not exist"
- Chạy migrations: `python manage.py migrate`
- Hoặc chạy migrate.js cho APP backend

### Lỗi duplicate key
- Seed data script đã có logic skip nếu data đã tồn tại
- Có thể chạy lại nhiều lần

