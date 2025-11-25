#!/bin/bash
# Script để kích hoạt tất cả tài khoản user trong database
# Sử dụng Docker để chạy script SQL

echo "=========================================="
echo "KÍCH HOẠT TẤT CẢ TÀI KHOẢN USER"
echo "=========================================="
echo ""

# Chạy script SQL trong container database
docker-compose exec -T db psql -U postgres -d fastfood_db <<EOF

-- Hiển thị số lượng user chưa kích hoạt trước khi update
SELECT 
    COUNT(*) as inactive_count
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

EOF

echo ""
echo "=========================================="
echo "HOÀN TẤT!"
echo "=========================================="

