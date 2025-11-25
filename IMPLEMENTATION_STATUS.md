# Trạng thái triển khai các Flow theo yêu cầu

## ✅ Đã hoàn thành

### Customer Flow
- ✅ **UC-10: Hủy đơn** - Đã thêm endpoint `/api/orders/{id}/cancel/` trong WEB backend
  - Chỉ cho phép hủy khi status là PENDING hoặc CONFIRMED
  - Tự động hoàn trả kho nếu đã confirm
  - Cập nhật payment_status sang REFUNDED nếu đã thanh toán

### Models đã thêm
- ✅ **Review & Rating Models** (UC-11)
  - `Review`: Đánh giá đơn hàng, merchant, shipper
  - `MenuItemReview`: Đánh giá chi tiết từng món
- ✅ **Complaint Model** (UC-13)
  - Hỗ trợ các loại khiếu nại: ORDER_ISSUE, FOOD_QUALITY, DELIVERY_ISSUE, PAYMENT_ISSUE, OTHER
  - Trạng thái: PENDING, IN_PROGRESS, RESOLVED, REJECTED

## 🔄 Cần triển khai tiếp

### Customer Flow
1. **UC-11: Đánh giá đơn/món/shipper**
   - [ ] Thêm endpoints API cho review (WEB backend)
   - [ ] Thêm endpoints API cho review (APP backend)
   - [ ] UI form đánh giá trong WEB frontend
   - [ ] UI form đánh giá trong APP
   - [ ] Hiển thị đánh giá trên trang chi tiết đơn

2. **UC-13: Gửi khiếu nại/phản hồi**
   - [ ] Thêm endpoints API cho complaint (WEB backend)
   - [ ] Thêm endpoints API cho complaint (APP backend)
   - [ ] UI form khiếu nại trong WEB frontend
   - [ ] UI form khiếu nại trong APP
   - [ ] Trang quản lý khiếu nại cho merchant/admin

3. **UC-03: Cải thiện theo dõi đơn real-time**
   - [ ] WebSocket integration cho real-time updates
   - [ ] Push notifications cho app
   - [ ] Map tracking cho shipper location

4. **UI Cancel Order**
   - [ ] Thêm nút hủy đơn trong WEB frontend (OrderDetail page)
   - [ ] Thêm nút hủy đơn trong APP (OrderDetailFragment)
   - [ ] Confirmation dialog trước khi hủy

### Merchant Flow
1. **UC-04: Quản lý kho (nhập, xuất, điều chỉnh)**
   - [ ] Thêm model InventoryTransaction
   - [ ] Endpoints: nhập kho, xuất kho, điều chỉnh
   - [ ] UI quản lý kho trong WEB frontend
   - [ ] UI quản lý kho trong APP

2. **UC-12: Xử lý thiếu kho (đổi món, giảm số lượng, hủy)**
   - [ ] Endpoint xử lý thiếu kho
   - [ ] UI thông báo và xử lý thiếu kho
   - [ ] Logic đổi món/giảm số lượng

3. **UC-13: Xử lý khiếu nại (Merchant)**
   - [ ] Endpoints xem và xử lý khiếu nại
   - [ ] UI danh sách khiếu nại
   - [ ] Form phản hồi khiếu nại

4. **UC-14: Xử lý refund**
   - [ ] Endpoint tạo refund request
   - [ ] Logic tính toán refund amount
   - [ ] UI quản lý refund

### Shipper Flow
1. **Xử lý trường hợp vấn đề (RETURNED, FAILED_DELIVERY)**
   - [ ] Thêm status RETURNED, FAILED_DELIVERY vào Order model
   - [ ] Endpoints cập nhật trạng thái vấn đề
   - [ ] UI báo cáo vấn đề trong APP

### Admin Flow
1. **UC-09: Quản lý user & role**
   - [ ] Endpoints CRUD users
   - [ ] Endpoint thay đổi role
   - [ ] UI quản lý users trong WEB frontend

2. **Xem log và theo dõi hoạt động hệ thống**
   - [ ] Tích hợp logging system
   - [ ] Dashboard hiển thị logs
   - [ ] Analytics và metrics

## 📝 Hướng dẫn tiếp theo

### 1. Tạo migrations cho models mới
```bash
cd WEB/backend
python manage.py makemigrations orders
python manage.py migrate
```

### 2. Thêm endpoints cho Review (UC-11)
Cần thêm vào `WEB/backend/orders/views.py`:
- `POST /api/orders/{id}/review/` - Tạo đánh giá
- `GET /api/orders/{id}/review/` - Xem đánh giá
- `GET /api/merchants/{id}/reviews/` - Xem đánh giá của merchant

### 3. Thêm endpoints cho Complaint (UC-13)
Cần thêm vào `WEB/backend/orders/views.py`:
- `POST /api/orders/{id}/complaint/` - Tạo khiếu nại
- `GET /api/complaints/` - Danh sách khiếu nại (merchant/admin)
- `PATCH /api/complaints/{id}/` - Xử lý khiếu nại

### 4. Cập nhật APP backend
Cần thêm các endpoints tương tự trong `APP/backend/index.js`

### 5. Cập nhật UI
- WEB frontend: Thêm components trong `WEB/frontend/src/pages/`
- APP: Thêm fragments trong `APP/app/src/main/java/com/example/app/`

## 🔗 Files đã thay đổi

1. `WEB/backend/orders/models.py` - Thêm Review, MenuItemReview, Complaint models
2. `WEB/backend/orders/views.py` - Thêm cancel order endpoint

## 📌 Lưu ý

- Cần chạy migrations sau khi thêm models
- Cần cập nhật serializers cho các models mới
- Cần thêm permissions cho các endpoints mới
- Cần test các tính năng mới

