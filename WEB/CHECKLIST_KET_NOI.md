# ✅ Checklist: Kiểm tra kết nối Frontend → Backend

## 🔴 QUAN TRỌNG: Kiểm tra ngay các điểm sau

### 1. Backend URL trên Render là gì?
```
URL: https://_____________________.onrender.com
```

### 2. Trên Netlify, biến `VITE_API_BASE` có giá trị gì?
```
Vào: Netlify → Site Settings → Environment variables
Kiểm tra: VITE_API_BASE = https://_____________________.onrender.com/api
```

### 3. Test Backend có chạy không?
```
Mở browser, truy cập: https://_____________________.onrender.com/api/
Phải thấy JSON response
```

### 4. CORS trên Render có đúng không?
```
Vào: Render → fastfood-backend → Environment
Kiểm tra: CORS_ORIGINS = https://fastfooddatdoan.netlify.app
```

## ⚠️ Lưu ý quan trọng

1. **KHÔNG có dấu `/` ở cuối URL**
   - ✅ `https://backend.onrender.com/api`
   - ❌ `https://backend.onrender.com/api/`

2. **Sau khi thay đổi env vars, PHẢI redeploy Netlify**
   - Vào Netlify → Deploys → Trigger deploy → Clear cache and deploy site

3. **Test bằng incognito mode**
   - Mở browser ở chế độ ẩn danh để tránh cache

