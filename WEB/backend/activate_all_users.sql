-- Script SQL để kích hoạt tất cả tài khoản user
-- Chạy: psql -U postgres -d fastfood_db -f activate_all_users.sql

-- Hiển thị số lượng user chưa kích hoạt trước khi update
SELECT 
    COUNT(*) as inactive_count,
    'Tài khoản chưa kích hoạt' as status
FROM auth_user 
WHERE is_active = false;

-- Kích hoạt tất cả tài khoản
UPDATE auth_user 
SET is_active = true 
WHERE is_active = false;

-- Hiển thị kết quả
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN is_active THEN 1 ELSE 0 END) as active_users,
    SUM(CASE WHEN NOT is_active THEN 1 ELSE 0 END) as inactive_users
FROM auth_user;

-- Hiển thị danh sách 10 user gần đây nhất
SELECT 
    id,
    username,
    email,
    CASE WHEN is_active THEN 'ACTIVE' ELSE 'INACTIVE' END as status,
    date_joined
FROM auth_user
ORDER BY date_joined DESC
LIMIT 10;

