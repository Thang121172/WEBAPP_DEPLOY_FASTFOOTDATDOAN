# Hướng dẫn hoàn thiện Implementation

## ✅ Đã hoàn thành

### Backend (WEB - Django)
1. ✅ **Models mới**: Review, MenuItemReview, Complaint
2. ✅ **Endpoints mới**:
   - `POST /api/orders/{id}/cancel/` - Hủy đơn (UC-10)
   - `POST /api/reviews/` - Tạo đánh giá (UC-11)
   - `GET /api/reviews/{id}/` - Xem đánh giá
   - `POST /api/complaints/` - Tạo khiếu nại (UC-13)
   - `GET /api/complaints/` - Danh sách khiếu nại
   - `POST /api/complaints/{id}/respond/` - Phản hồi khiếu nại
   - `POST /api/inventory/{id}/adjust_stock/` - Quản lý kho (UC-04)
   - `POST /api/merchant-orders/{id}/handle_out_of_stock/` - Xử lý thiếu kho (UC-12)
   - `POST /api/merchant-orders/{id}/refund/` - Refund (UC-14)
   - `POST /api/shipper/{id}/report_issue/` - Báo cáo vấn đề
   - `GET /api/admin/users/` - Danh sách users (UC-09)
   - `PATCH /api/admin/users/{id}/update_role/` - Cập nhật role

### Backend (APP - Node.js)
1. ✅ **Endpoints mới** (tương tự WEB backend):
   - Cancel order (đã có sẵn)
   - Review endpoints
   - Complaint endpoints
   - Inventory management
   - Out of stock handling
   - Refund
   - Shipper issue reporting
   - Admin user management

## 🔄 Cần làm tiếp

### 1. Database Migrations

#### WEB Backend (Django)
```bash
cd WEB/backend
python manage.py makemigrations orders
python manage.py migrate
```

#### APP Backend (Node.js)
Cần thêm các bảng vào database schema trong `APP/backend/migrate.js`:

```sql
-- Reviews table
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

-- Menu item reviews
CREATE TABLE IF NOT EXISTS menu_item_reviews (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
  order_item_id INTEGER NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
  rating INTEGER NOT NULL DEFAULT 5 CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE(review_id, order_item_id)
);

-- Complaints table
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
```

### 2. UI Components - WEB Frontend

#### Customer Features
1. **Cancel Order Button** (`WEB/frontend/src/pages/OrderDetail.tsx`)
   - Thêm nút "Hủy đơn" khi status là PENDING hoặc CONFIRMED
   - Confirmation dialog
   - Call API: `POST /api/orders/{id}/cancel/`

2. **Review Form** (`WEB/frontend/src/pages/ReviewOrder.tsx` - mới)
   - Form đánh giá đơn hàng, merchant, shipper
   - Đánh giá từng món
   - Call API: `POST /api/reviews/`

3. **Complaint Form** (`WEB/frontend/src/pages/ComplaintForm.tsx` - mới)
   - Form gửi khiếu nại
   - Call API: `POST /api/complaints/`

#### Merchant Features
1. **Inventory Management** (`WEB/frontend/src/pages/Merchant/Inventory.tsx` - mới)
   - Danh sách món với tồn kho
   - Form nhập/xuất/điều chỉnh kho
   - Call API: `POST /api/inventory/{id}/adjust_stock/`

2. **Out of Stock Handling** (`WEB/frontend/src/pages/Merchant/HandleOutOfStock.tsx` - mới)
   - Modal xử lý thiếu kho
   - Options: Đổi món, Giảm số lượng, Hủy đơn
   - Call API: `POST /api/merchant-orders/{id}/handle_out_of_stock/`

3. **Complaints Management** (`WEB/frontend/src/pages/Merchant/Complaints.tsx` - mới)
   - Danh sách khiếu nại
   - Form phản hồi
   - Call API: `GET /api/complaints/`, `POST /api/complaints/{id}/respond/`

4. **Refund Management** (`WEB/frontend/src/pages/Merchant/Refund.tsx` - mới)
   - Form hoàn tiền
   - Call API: `POST /api/merchant-orders/{id}/refund/`

#### Shipper Features
1. **Report Issue** (`WEB/frontend/src/pages/ShipperApp.tsx`)
   - Thêm nút "Báo cáo vấn đề"
   - Form báo cáo
   - Call API: `POST /api/shipper/{id}/report_issue/`

#### Admin Features
1. **User Management** (`WEB/frontend/src/pages/Admin/Users.tsx` - mới)
   - Danh sách users
   - Form thay đổi role
   - Call API: `GET /api/admin/users/`, `PATCH /api/admin/users/{id}/update_role/`

### 3. UI Components - APP (Android)

#### Customer Features
1. **Cancel Order** (`APP/app/src/main/java/com/example/app/OrderDetailFragment.java`)
   - Thêm button hủy đơn
   - Call API: `POST /orders/{id}/cancel`

2. **Review** (`APP/app/src/main/java/com/example/app/ReviewFragment.java` - mới)
   - Form đánh giá
   - Call API: `POST /reviews`

3. **Complaint** (`APP/app/src/main/java/com/example/app/ComplaintFragment.java` - mới)
   - Form khiếu nại
   - Call API: `POST /complaints`

#### Merchant Features
1. **Inventory** (`APP/app/src/main/java/com/example/app/MerchantInventoryFragment.java` - mới)
   - Quản lý kho
   - Call API: `POST /inventory/{id}/adjust_stock`

2. **Handle Out of Stock** (`APP/app/src/main/java/com/example/app/MerchantHomeFragment.java`)
   - Xử lý thiếu kho
   - Call API: `POST /merchant/orders/{id}/handle_out_of_stock`

3. **Complaints** (`APP/app/src/main/java/com/example/app/MerchantComplaintsFragment.java` - mới)
   - Quản lý khiếu nại
   - Call API: `GET /complaints`, `POST /complaints/{id}/respond`

#### Shipper Features
1. **Report Issue** (`APP/app/src/main/java/com/example/app/ShipperDashboardFragment.java`)
   - Báo cáo vấn đề
   - Call API: `POST /shipper/orders/{id}/report_issue`

## 📝 Files đã thay đổi

### WEB Backend
- `WEB/backend/orders/models.py` - Thêm Review, MenuItemReview, Complaint models
- `WEB/backend/orders/views.py` - Thêm tất cả ViewSets mới
- `WEB/backend/core/urls.py` - Register các ViewSet mới

### APP Backend
- `APP/backend/index.js` - Thêm tất cả endpoints mới

## 🚀 Bước tiếp theo

1. **Chạy migrations** cho WEB backend
2. **Cập nhật database schema** cho APP backend
3. **Tạo UI components** theo danh sách trên
4. **Test các tính năng** mới
5. **Cập nhật documentation** nếu cần

## 📌 Lưu ý

- Cần kiểm tra permissions cho từng endpoint
- Cần thêm validation cho input
- Cần thêm error handling
- Cần test với các role khác nhau
- Cần cập nhật API documentation

