# ✅ CHECKLIST: KIỂM TRA TRƯỚC KHI TEST WEB

## 🎯 **MỤC TIÊU:**
Kiểm tra xem tất cả các thành phần đã sẵn sàng để test website chưa.

---

## 📋 **CHECKLIST:**

### **1. BACKEND (Render) - ĐÃ HOÀN THÀNH:**
- [x] Backend đã được deploy lên Render
- [x] Service đang Live: `https://fastfood-backend-t8jz.onrender.com`
- [x] Build thành công
- [ ] ⚠️ **DATABASE_URL đã được thêm chưa?** (QUAN TRỌNG!)
- [ ] ⚠️ **ALLOWED_HOSTS đã được thêm chưa?** (Nếu còn lỗi)
- [ ] ⚠️ **CORS_ORIGINS đã được thêm chưa?** (Để frontend gọi API được)
- [ ] ⚠️ **Migrations đã chạy chưa?** (Nếu có DATABASE_URL)

### **2. FRONTEND (Netlify) - ĐÃ HOÀN THÀNH:**
- [x] Frontend đã được deploy lên Netlify
- [ ] ⚠️ **VITE_API_BASE đã được cập nhật chưa?** (URL Render backend)
- [ ] ⚠️ **Frontend đã được redeploy sau khi thêm VITE_API_BASE chưa?**

### **3. KẾT NỐI FRONTEND - BACKEND:**
- [ ] Frontend đã biết URL backend (VITE_API_BASE)
- [ ] Backend đã cho phép CORS từ frontend (CORS_ORIGINS)

---

## 🔍 **KIỂM TRA CHI TIẾT:**

### **🔹 BƯỚC 1: Kiểm tra Backend**

#### **1.1. Test URL backend:**
1. Mở browser: `https://fastfood-backend-t8jz.onrender.com`
2. Hoặc: `https://fastfood-backend-t8jz.onrender.com/api/`
3. ✅ Nếu thấy response → **Backend OK!**
4. ❌ Nếu lỗi 400/500 → Xem logs trong Render

#### **1.2. Kiểm tra Environment Variables:**
Vào Render → Service `fastfood-backend` → Tab **"Environment"**, kiểm tra có các biến sau:

- [ ] **DATABASE_URL** - Có giá trị (Internal Database URL)
- [ ] **ALLOWED_HOSTS** - Có giá trị: `fastfood-backend-t8jz.onrender.com`
- [ ] **CORS_ORIGINS** - Có giá trị: `https://fastfooddatdoan.netlify.app` (KHÔNG có dấu `/` ở cuối!)
- [ ] **SECRET_KEY** - Tự động có
- [ ] **DJANGO_SETTINGS_MODULE** - Có: `core.settings.prod`

#### **1.3. Kiểm tra Database:**
- [ ] Database đã được tạo trên Render chưa?
- [ ] Migrations đã chạy chưa? (Xem logs trong Render)

---

### **🔹 BƯỚC 2: Kiểm tra Frontend**

#### **2.1. Kiểm tra Environment Variables:**
Vào Netlify → Site → **"Site settings"** → **"Environment variables"**:

- [ ] **VITE_API_BASE** - Có giá trị: `https://fastfood-backend-t8jz.onrender.com/api`
  - ⚠️ **Lưu ý:** Phải có `/api` ở cuối!

#### **2.2. Kiểm tra Frontend đã redeploy chưa:**
- [ ] Frontend đã được redeploy sau khi thêm `VITE_API_BASE` chưa?
- [ ] Vào Netlify → Tab **"Deploys"** → Xem deploy mới nhất

---

## ✅ **CÁC BƯỚC CÒN THIẾU (NẾU CHƯA LÀM):**

### **1. Thêm DATABASE_URL (Nếu chưa có):**
1. Tạo PostgreSQL Database trên Render (nếu chưa có)
2. Copy Internal Database URL
3. Thêm vào Environment Variables của web service

### **2. Chạy Migrations (Nếu chưa chạy):**
- Sửa Build Command tạm thời để chạy migrations (xem file `RENDER_MIGRATE_WITHOUT_SHELL.md`)

### **3. Thêm CORS_ORIGINS (Nếu chưa có):**
- Thêm: `https://fastfooddatdoan.netlify.app` (KHÔNG có dấu `/`)

### **4. Cập nhật VITE_API_BASE trên Netlify:**
- Sửa: `https://fastfood-backend-t8jz.onrender.com/api`
- Redeploy Netlify

---

## 🧪 **TEST WEBSITE:**

### **Bước 1: Test Backend API**
1. Mở browser: `https://fastfood-backend-t8jz.onrender.com/api/`
2. ✅ Nếu thấy response → OK
3. Test endpoint cụ thể: `https://fastfood-backend-t8jz.onrender.com/api/accounts/`

### **Bước 2: Test Frontend**
1. Mở website Netlify: `https://fastfooddatdoan.netlify.app`
2. Mở **Developer Tools** (F12) → Tab **"Network"**
3. Thử một hành động gọi API (ví dụ: đăng nhập)
4. Kiểm tra:
   - ✅ Request có đến đúng URL backend không?
   - ✅ Response có thành công không?
   - ✅ Có bị CORS block không?

### **Bước 3: Kiểm tra lỗi trong Console**
- Mở Developer Tools → Tab **"Console"**
- Xem có lỗi gì không (CORS, 404, 500...)

---

## ❌ **CÁC LỖI THƯỜNG GẶP KHI TEST:**

### **Lỗi 1: CORS block**
```
Access to fetch at '...' from origin '...' has been blocked by CORS policy
```
**Giải pháp:**
- ✅ Kiểm tra `CORS_ORIGINS` đã có URL Netlify chưa
- ✅ Đảm bảo không có dấu `/` ở cuối URL
- ✅ Redeploy backend sau khi sửa

### **Lỗi 2: API 404**
```
GET https://fastfood-backend-t8jz.onrender.com/api/... 404
```
**Giải pháp:**
- ✅ Kiểm tra `VITE_API_BASE` đã đúng chưa (có `/api` ở cuối)
- ✅ Kiểm tra backend đang chạy (truy cập URL backend trực tiếp)

### **Lỗi 3: Database connection failed**
```
django.db.utils.OperationalError: could not connect to server
```
**Giải pháp:**
- ✅ Kiểm tra `DATABASE_URL` đã được thêm chưa
- ✅ Kiểm tra database đang running
- ✅ Đảm bảo dùng Internal Database URL

### **Lỗi 4: 500 Internal Server Error**
**Giải pháp:**
- ✅ Xem logs trong Render để tìm lỗi cụ thể
- ✅ Kiểm tra migrations đã chạy chưa
- ✅ Kiểm tra SECRET_KEY đã có chưa

---

## ✅ **SẴN SÀNG TEST KHI:**

✅ Backend đã Live  
✅ Frontend đã Live  
✅ `VITE_API_BASE` đã được cập nhật  
✅ `CORS_ORIGINS` đã được thêm  
✅ `DATABASE_URL` đã có (nếu cần database)  
✅ Migrations đã chạy (nếu có database)  

---

## 🎯 **QUYẾT ĐỊNH:**

### **Nếu TẤT CẢ đã hoàn thành:**
✅ **Bạn có thể test ngay!**

### **Nếu CÒN THIẾU:**
- ⚠️ Hoàn thành các bước còn thiếu trước
- ⚠️ Xem checklist ở trên để biết còn thiếu gì

---

**Hãy kiểm tra checklist ở trên và cho tôi biết còn thiếu gì!** 🚀

