# 🔧 Sửa lỗi CORS: Thêm URL Vercel vào Render

## ✅ Tin tốt!
- Frontend đã kết nối đúng backend URL ✅
- Environment variable `VITE_API_BASE` đã có hiệu lực ✅
- Backend đang chạy ✅

## ❌ Vấn đề: CORS Error

**Lỗi:**
```
Access to XMLHttpRequest at 'https://fastfood-backend-t8jz.onrender.com/api/...' 
from origin 'https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app' 
has been blocked by CORS policy
```

**Nguyên nhân:** Backend Render chưa cho phép origin Vercel của bạn.

## 🔧 Giải pháp: Thêm URL Vercel vào CORS_ORIGINS

### Bước 1: Vào Render Dashboard

1. Truy cập: https://dashboard.render.com/
2. Đăng nhập
3. Chọn service **`fastfood-backend-t8jz`**

### Bước 2: Vào Environment Variables

1. Click vào tab **"Environment"** (ở menu trên cùng)
2. Tìm biến **`CORS_ORIGINS`**

### Bước 3: Sửa CORS_ORIGINS

**URL Vercel của bạn:** `https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app`

**Có 2 trường hợp:**

#### Trường hợp 1: Chưa có biến CORS_ORIGINS

1. Click **"Add Environment Variable"** hoặc **"Add New"**
2. Điền:
   ```
   Key: CORS_ORIGINS
   Value: https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app
   ```
   ⚠️ **KHÔNG có dấu `/` ở cuối!**

3. Click **"Save Changes"**

#### Trường hợp 2: Đã có biến CORS_ORIGINS

1. Click vào biến `CORS_ORIGINS` để sửa
2. Thêm URL Vercel vào (phân cách bằng dấu phẩy nếu đã có URL khác):

   **Nếu chỉ có Netlify:**
   ```
   CORS_ORIGINS = https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app,https://fastfooddatdoan.netlify.app
   ```

   **Nếu chưa có gì hoặc muốn chỉ dùng Vercel:**
   ```
   CORS_ORIGINS = https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app
   ```

   ⚠️ **KHÔNG có dấu `/` ở cuối!**
   ⚠️ **KHÔNG có khoảng trắng sau dấu phẩy!**

3. Click **"Save Changes"**

### Bước 4: Restart Service (QUAN TRỌNG!)

Sau khi save, Render có thể tự động restart, nhưng nếu không:

1. Vào tab **"Events"** hoặc **"Logs"**
2. Hoặc click **"Manual Deploy"** → **"Deploy latest commit"**
3. Đợi service restart (30-60 giây)

### Bước 5: Test lại

1. Mở lại URL Vercel: `https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app`
2. Mở **Console** (F12) để clear cache cũ
3. Hard refresh: **Ctrl + Shift + R** (Windows) hoặc **Cmd + Shift + R** (Mac)
4. Vào trang đăng ký: `/register`
5. Thử đăng ký lại
6. **Lỗi CORS phải biến mất!** ✅

---

## ✅ Checklist

- [ ] Đã vào Render Dashboard → service `fastfood-backend-t8jz`
- [ ] Đã vào tab **"Environment"**
- [ ] Đã tìm/sửa biến `CORS_ORIGINS`
- [ ] Đã thêm URL: `https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app` (KHÔNG có `/` cuối)
- [ ] Đã Save Changes
- [ ] Service đã restart (check logs)
- [ ] Đã test lại trên Vercel

---

## 🆘 Nếu vẫn lỗi CORS

### Kiểm tra lại:

1. **URL trong CORS_ORIGINS có đúng không?**
   - Phải trùng chính xác với origin trong error message
   - Không có dấu `/` ở cuối
   - Không có khoảng trắng thừa

2. **Service đã restart chưa?**
   - Xem logs trên Render
   - Đảm bảo service đang chạy (status: Live)

3. **Clear browser cache:**
   - Hard refresh: Ctrl + Shift + R
   - Hoặc mở incognito mode

4. **Kiểm tra CORS_ORIGINS có được load đúng không:**
   - Xem logs trên Render khi service start
   - Tìm dòng có `CORS_ORIGINS` hoặc `CORS_ALLOWED_ORIGINS`

---

## 📝 Lưu ý về URL Vercel

URL hiện tại của bạn là preview URL:
```
https://test-web-deploy-9a9ly7tv9-thangs-projects-c5afd53f.vercel.app
```

**Sau khi merge vào production, URL sẽ đổi thành:**
```
https://test-web-deploy.vercel.app
```

**Hoặc bạn có thể set custom domain trên Vercel.**

Khi URL thay đổi, nhớ update lại `CORS_ORIGINS` trên Render!

