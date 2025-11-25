# ✅ CHECKLIST: SAU KHI DEPLOY BACKEND LÊN RENDER

## 🎉 **BẠN ĐÃ HOÀN THÀNH:**
- ✅ Backend đã được deploy lên Render
- ✅ Service đã Live: `https://fastfood-backend-t8jz.onrender.com`

## 📋 **CÁC BƯỚC TIẾP THEO:**

---

## ✅ **BƯỚC 1: KIỂM TRA BACKEND HOẠT ĐỘNG**

### **1.1. Test URL backend:**
1. Mở browser, truy cập: `https://fastfood-backend-t8jz.onrender.com`
2. Hoặc test API: `https://fastfood-backend-t8jz.onrender.com/api/`
3. ✅ Nếu thấy response (JSON hoặc HTML) → **Backend OK!**
4. ❌ Nếu lỗi 400/500 → Xem logs trong Render

### **1.2. Kiểm tra logs:**
- Vào Render → Service `fastfood-backend` → Tab **"Logs"**
- Xem có lỗi gì không (nếu có lỗi ALLOWED_HOSTS, xem Bước 2)

---

## ✅ **BƯỚC 2: SỬA LỖI ALLOWED_HOSTS (Nếu có)**

Nếu thấy lỗi:
```
Invalid HTTP_HOST header: 'fastfood-backend-t8jz.onrender.com'
```

**Cách sửa:**
1. Vào Render → Service `fastfood-backend` → Tab **"Environment"**
2. Click **"Add Environment Variable"**
3. Thêm:
   ```
   Key: ALLOWED_HOSTS
   Value: fastfood-backend-t8jz.onrender.com
   ```
   ⚠️ **Thay `fastfood-backend-t8jz` bằng URL thực tế của bạn!**
4. Click **"Save Changes"**
5. Chờ Render redeploy (2-3 phút)

---

## ✅ **BƯỚC 3: TẠO DATABASE (Nếu chưa có)**

### **3.1. Tạo PostgreSQL Database:**
1. Render Dashboard → **"New +"** → **"PostgreSQL"**
2. Điền:
   - **Name:** `fastfood-db`
   - **Database:** `fastfood_db`
   - **User:** `fastfood_user`
   - **Region:** Cùng region với web service
   - **Plan:** **Free**
3. Click **"Create Database"**
4. Chờ database được tạo (1-2 phút)

### **3.2. Lấy Database URL:**
1. Vào database service `fastfood-db`
2. Tab **"Connections"** hoặc **"Info"**
3. Copy **"Internal Database URL"**
   - Format: `postgresql://user:password@host:port/dbname`
   - ⚠️ **QUAN TRỌNG:** Dùng **Internal URL**, không phải External!

### **3.3. Thêm DATABASE_URL vào Web Service:**
1. Vào service `fastfood-backend` → Tab **"Environment"**
2. Click **"Add Environment Variable"**
3. Thêm:
   ```
   Key: DATABASE_URL
   Value: [Paste Internal Database URL ở đây]
   ```
4. Click **"Save Changes"**
5. Render sẽ tự động redeploy

---

## ✅ **BƯỚC 4: CHẠY MIGRATIONS**

Sau khi có DATABASE_URL:

### **Cách 1: Qua Shell (KHUYÊN DÙNG)**
1. Vào Render → Service `fastfood-backend` → Tab **"Shell"**
2. Click **"Connect"** để mở terminal
3. Chạy lệnh:
   ```bash
   cd backend
   python manage.py migrate
   ```
4. Nếu thành công → ✅ **Migrations đã chạy!**

### **Cách 2: Qua Manual Deploy (Nếu Shell không hoạt động)**
1. Vào service → Tab **"Manual Deploy"**
2. Tạm thời sửa build command thành:
   ```bash
   cd backend && pip install -r requirements.txt && python manage.py migrate && python manage.py collectstatic --noinput
   ```
3. Deploy, sau đó sửa lại build command về như cũ

### **Tạo Superuser (nếu cần):**
Trong Shell:
```bash
cd backend
python manage.py createsuperuser
```
- Nhập username, email, password khi được hỏi

---

## ✅ **BƯỚC 5: THÊM CORS_ORIGINS**

Để frontend (Netlify) có thể gọi API:

1. Vào Render → Service `fastfood-backend` → Tab **"Environment"**
2. Click **"Add Environment Variable"**
3. Thêm:
   ```
   Key: CORS_ORIGINS
   Value: https://your-netlify-site.netlify.app
   ```
   ⚠️ **Thay `your-netlify-site` bằng URL Netlify thực tế của bạn!**

4. Nếu có nhiều domains (Netlify + custom domain):
   ```
   Key: CORS_ORIGINS
   Value: https://your-site.netlify.app,https://your-custom-domain.com
   ```
   (Phân cách bằng dấu phẩy `,`)

5. Click **"Save Changes"**
6. Render sẽ tự động redeploy

---

## ✅ **BƯỚC 6: CẬP NHẬT NETLIFY**

Kết nối frontend với backend:

### **6.1. Thêm Environment Variable:**
1. Vào **Netlify** → Site của bạn → **"Site settings"**
2. Tab **"Environment variables"**
3. Sửa hoặc thêm biến:
   ```
   Key: VITE_API_BASE
   Value: https://fastfood-backend-t8jz.onrender.com/api
   ```
   ⚠️ **Thay `fastfood-backend-t8jz` bằng URL Render thực tế của bạn!**

### **6.2. Redeploy Netlify:**
1. Vào tab **"Deploys"**
2. Click **"Trigger deploy"** → **"Clear cache and deploy site"**
3. Chờ deploy xong

---

## ✅ **BƯỚC 7: KIỂM TRA KẾT NỐI FRONTEND - BACKEND**

### **7.1. Test từ Frontend:**
1. Mở website Netlify của bạn
2. Mở **Developer Tools** (F12) → Tab **Network**
3. Thử một hành động gọi API (ví dụ: đăng nhập, load danh sách)
4. Kiểm tra:
   - ✅ Request có đến đúng URL backend không?
   - ✅ Response có thành công không?
   - ✅ Có bị CORS block không?

### **7.2. Test API trực tiếp:**
1. Mở browser, truy cập: `https://fastfood-backend-t8jz.onrender.com/api/`
2. Hoặc test endpoint cụ thể:
   - `/api/accounts/` - Accounts API
   - `/api/menus/` - Menus API
   - `/api/orders/` - Orders API

---

## 📝 **CHECKLIST TỔNG KẾT:**

| # | Bước | Status | Ghi chú |
|---|------|--------|---------|
| 1 | Kiểm tra backend hoạt động | ⬜ | Test URL backend |
| 2 | Sửa lỗi ALLOWED_HOSTS (nếu có) | ⬜ | Thêm env var ALLOWED_HOSTS |
| 3 | Tạo PostgreSQL Database | ⬜ | Tạo thủ công trên Render |
| 4 | Thêm DATABASE_URL | ⬜ | Copy Internal Database URL |
| 5 | Chạy Migrations | ⬜ | Qua Shell hoặc Manual Deploy |
| 6 | Thêm CORS_ORIGINS | ⬜ | URL Netlify của bạn |
| 7 | Cập nhật Netlify (VITE_API_BASE) | ⬜ | URL Render backend |
| 8 | Test kết nối Frontend-Backend | ⬜ | Kiểm tra API calls |

---

## 🎯 **THỨ TỰ ƯU TIÊN:**

1. **Quan trọng nhất:**
   - ✅ Tạo Database + Thêm DATABASE_URL
   - ✅ Chạy Migrations
   - ✅ Thêm CORS_ORIGINS

2. **Sau đó:**
   - ✅ Cập nhật Netlify
   - ✅ Test kết nối

---

## 🔗 **CÁC URL QUAN TRỌNG:**

Sau khi hoàn tất, bạn sẽ có:

| Service | URL | Mục đích |
|---------|-----|----------|
| **Backend API** | `https://fastfood-backend-t8jz.onrender.com` | API endpoint |
| **API Base** | `https://fastfood-backend-t8jz.onrender.com/api` | Dùng cho VITE_API_BASE |
| **Frontend** | `https://your-site.netlify.app` | Website người dùng |
| **Admin Panel** | `https://fastfood-backend-t8jz.onrender.com/admin/` | Django admin |

---

## ❓ **CẦN GIÚP?**

Nếu gặp lỗi ở bước nào, cho tôi biết và tôi sẽ hướng dẫn cụ thể!

---

**Chúc bạn hoàn thành các bước còn lại! 🚀**

