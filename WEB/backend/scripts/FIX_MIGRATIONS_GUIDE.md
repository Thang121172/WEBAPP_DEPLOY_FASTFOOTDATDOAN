# Hướng dẫn xử lý lỗi Migration

## Tình huống
Bạn đang gặp lỗi khi chạy migrations và cần xóa migration records trực tiếp từ database.

## Cách 1: Sử dụng dbshell (Đang mở)

Nếu bạn đã mở `dbshell` với lệnh:
```bash
py -3.13 manage.py dbshell
```

Hãy chạy các lệnh SQL sau trong dbshell:

```sql
-- 1. Xem migration records hiện tại của orders app
SELECT id, app, name FROM django_migrations WHERE app = 'orders' ORDER BY id;

-- 2. Xóa migration records cho orders app
DELETE FROM django_migrations WHERE app = 'orders';

-- 3. Xác nhận đã xóa (sẽ trả về 0)
SELECT COUNT(*) as remaining_records FROM django_migrations WHERE app = 'orders';

-- 4. Thoát dbshell
\q
```

Sau đó chạy lại migrations:
```bash
cd WEB/backend
py -3.13 manage.py migrate orders
```

## Cách 2: Sử dụng script Python (nếu database đã kết nối)

```bash
cd WEB/backend
py -3.13 scripts/fix_migrations.py
```

Sau đó:
```bash
py -3.13 manage.py migrate orders
```

## Cách 3: Fake migration (nếu bảng đã tồn tại)

Nếu các bảng (reviews, menu_item_reviews, complaints) đã tồn tại trong database nhưng migration chưa được ghi nhận:

```bash
cd WEB/backend
py -3.13 manage.py migrate orders --fake
```

## Kiểm tra sau khi chạy

```bash
# Xem migration status
py -3.13 manage.py showmigrations orders

# Hoặc xem trong database
py -3.13 manage.py dbshell
# Trong dbshell:
SELECT id, app, name FROM django_migrations WHERE app = 'orders' ORDER BY id;
```

## Lưu ý

- Nếu xóa migration records, Django sẽ coi như chưa chạy migrations
- Khi chạy `migrate` lại, Django sẽ tạo lại các bảng nếu chưa tồn tại
- Nếu bảng đã tồn tại, có thể cần dùng `--fake` để đánh dấu migration đã chạy mà không thực thi SQL

