# Hướng dẫn chạy Seed Data

## WEB Backend (Django)

### Cách 1: Sử dụng Django shell
```bash
cd WEB/backend
python manage.py shell < scripts/seed_data.py
```

### Cách 2: Chạy trực tiếp
```bash
cd WEB/backend
python scripts/seed_data.py
```

### Cách 3: Sử dụng manage.py runscript (nếu có django-extensions)
```bash
cd WEB/backend
python manage.py runscript seed_data
```

## APP Backend (Node.js)

```bash
cd APP/backend
node seed_data.js
```

## Data được tạo

### Users
- **admin** / admin123 (Admin)
- **customer1** / 123456 (Customer)
- **customer2** / 123456 (Customer)
- **merchant1** / 123456 (Merchant)
- **merchant2** / 123456 (Merchant)
- **shipper1** / 123456 (Shipper)
- **shipper2** / 123456 (Shipper)

### Restaurants/Merchants
- Pizza Hut (merchant1)
- KFC (merchant2)
- McDonald's (merchant1)

### Menu Items
- 9 món ăn mẫu (3 món/restaurant)

### Orders
- 4 đơn hàng mẫu với các trạng thái khác nhau:
  - DELIVERED (có thể review)
  - DELIVERING
  - CONFIRMED (có thể hủy)
  - PENDING (có thể hủy)

### Reviews & Complaints
- 1 review mẫu
- 1 complaint mẫu

## Lưu ý

- Đảm bảo database đã được migrate trước khi chạy seed
- Script sẽ tự động skip nếu data đã tồn tại (dựa trên username, name, etc.)
- Có thể chạy lại nhiều lần mà không bị duplicate

