-- Script SQL để xóa migration records cho orders app
-- Chạy trong dbshell: \i scripts/delete_migration_records.sql
-- Hoặc copy-paste các lệnh này vào dbshell

-- 1. Xem tất cả migration records hiện tại
SELECT id, app, name FROM django_migrations WHERE app = 'orders' ORDER BY id;

-- 2. Xóa migration records cho orders app
DELETE FROM django_migrations WHERE app = 'orders';

-- 3. Xác nhận đã xóa
SELECT COUNT(*) as remaining_records FROM django_migrations WHERE app = 'orders';

-- 4. Xem tất cả migrations còn lại
SELECT id, app, name FROM django_migrations ORDER BY app, id;

