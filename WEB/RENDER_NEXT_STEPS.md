# 🚀 CÁC BƯỚC TIẾP THEO SAU KHI TẠO BLUEPRINT

## ✅ **BẠN ĐÃ LÀM XONG:**
- ✅ Blueprint đã được tạo: `Fastfood_Backend`
- ✅ File `render.yaml` đã được sync
- ✅ Web Service đã được tạo từ Blueprint

## 📋 **CÁC BƯỚC TIẾP THEO:**

---

## 🔹 **BƯỚC 1: TẠO POSTGRESQL DATABASE (THỦ CÔNG)**

Blueprint không tự tạo database, bạn cần tạo thủ công:

1. **Trên Render Dashboard**, click **"New +"** (góc trên phải)
2. Chọn **"PostgreSQL"**
3. Điền thông tin:
   - **Name:** `fastfood-db`
   - **Database:** `fastfood_db` 
   - **User:** `fastfood_user`
   - **Region:** Chọn cùng region với web service (khuyên dùng **Oregon**)
   - **PostgreSQL Version:** `15` (hoặc mới nhất)
   - **Plan:** **Free** (hoặc Starter nếu cần)
4. Click **"Create Database"**
5. **Chờ database được tạo** (khoảng 1-2 phút)

---

## 🔹 **BƯỚC 2: LẤY DATABASE URL**

Sau khi database được tạo:

1. Vào database service **"fastfood-db"**
2. Vào tab **"Connections"** hoặc **"Info"**
3. **Copy "Internal Database URL"** (dạng: `postgresql://user:pass@host:port/dbname`)
   - ⚠️ **QUAN TRỌNG:** Dùng **Internal URL**, không phải External!
4. **Lưu lại URL này** - sẽ dùng ở bước tiếp theo

---

## 🔹 **BƯỚC 3: CẬP NHẬT ENVIRONMENT VARIABLES CHO WEB SERVICE**

1. Quay lại Blueprint hoặc vào service **"fastfood-backend"**
2. Click vào service **"fastfood-backend"** để vào trang chi tiết
3. Vào tab **"Environment"**
4. Click **"Add Environment Variable"**

### **Thêm các biến sau:**

#### **1. DATABASE_URL (QUAN TRỌNG NHẤT):**
```
Key: DATABASE_URL
Value: [Paste Internal Database URL từ bước 2]
```

#### **2. CORS_ORIGINS (Để frontend kết nối được):**
```
Key: CORS_ORIGINS
Value: https://your-netlify-site.netlify.app
```
⚠️ **Thay `your-netlify-site` bằng URL Netlify thực tế của bạn!**

#### **3. ALLOWED_HOSTS (Nếu cần):**
```
Key: ALLOWED_HOSTS
Value: fastfood-backend-xxxx.onrender.com
```
⚠️ **Thay `fastfood-backend-xxxx` bằng URL thực tế từ Render!**

5. Click **"Save Changes"**
6. Render sẽ tự động **redeploy** service với environment variables mới

---

## 🔹 **BƯỚC 4: CHỜ DEPLOY XONG**

1. Vào service **"fastfood-backend"** → Tab **"Events"** hoặc **"Logs"**
2. Chờ deploy hoàn tất (khoảng 5-10 phút)
3. Kiểm tra logs xem có lỗi gì không

---

## 🔹 **BƯỚC 5: CHẠY MIGRATIONS**

Sau khi deploy xong:

1. Vào service **"fastfood-backend"** → Tab **"Shell"**
2. Click **"Connect"** để mở terminal
3. Chạy lệnh:
   ```bash
   cd backend
   python manage.py migrate
   ```

4. **Tạo superuser (nếu cần):**
   ```bash
   python manage.py createsuperuser
   ```
   - Nhập username, email, password khi được hỏi

---

## 🔹 **BƯỚC 6: KIỂM TRA BACKEND**

1. Mở URL backend: `https://fastfood-backend-xxxx.onrender.com`
2. Hoặc test API: `https://fastfood-backend-xxxx.onrender.com/api/`
3. Nếu thấy response (JSON hoặc HTML) → ✅ **Backend hoạt động!**

---

## 🔹 **BƯỚC 7: CẬP NHẬT NETLIFY**

1. Vào **Netlify** → Site settings → **Environment variables**
2. Sửa `VITE_API_BASE`:
   ```
   https://fastfood-backend-xxxx.onrender.com/api
   ```
   ⚠️ **Thay `fastfood-backend-xxxx` bằng URL thực tế từ Render!**

3. **Redeploy Netlify:**
   - Vào tab **"Deploys"**
   - Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## ✅ **TÓM TẮT:**

| Bước | Việc cần làm | Status |
|------|--------------|--------|
| 1 | Tạo PostgreSQL Database | ⬜ |
| 2 | Lấy Internal Database URL | ⬜ |
| 3 | Thêm DATABASE_URL vào web service | ⬜ |
| 4 | Thêm CORS_ORIGINS | ⬜ |
| 5 | Chờ deploy xong | ⬜ |
| 6 | Chạy migrations | ⬜ |
| 7 | Cập nhật Netlify | ⬜ |

---

## 🎉 **SAU KHI HOÀN TẤT:**

- ✅ Backend: `https://fastfood-backend-xxxx.onrender.com`
- ✅ Frontend: `https://your-site.netlify.app`
- ✅ Database: `fastfood-db` (PostgreSQL)
- ✅ Migrations: Đã chạy
- ✅ CORS: Đã cấu hình

**Website của bạn đã sẵn sàng!** 🚀

---

## ❓ **CẦN GIÚP?**

Nếu gặp lỗi ở bước nào, cho tôi biết và tôi sẽ hướng dẫn cụ thể hơn!

