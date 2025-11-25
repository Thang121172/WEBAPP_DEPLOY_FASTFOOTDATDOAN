# 🔧 DEBUG: Lỗi kết nối máy chủ

## ⚠️ **LỖI:**
"Lỗi kết nối máy chủ" khi đăng nhập/nhấn chức năng.

## 🔍 **NGUYÊN NHÂN CÓ THỂ:**

1. ❌ `VITE_API_BASE` chưa được set trên Netlify
2. ❌ `CORS_ORIGINS` chưa được set trên Render
3. ❌ Backend chưa chạy hoặc có lỗi
4. ❌ URL backend sai

---

## ✅ **CÁCH KIỂM TRA VÀ SỬA:**

### **BƯỚC 1: Kiểm tra Backend có đang chạy không**

1. **Mở browser, truy cập:**
   ```
   https://fastfood-backend-t8jz.onrender.com/api/
   ```

2. **Kết quả:**
   - ✅ Thấy response (JSON hoặc HTML) → **Backend OK!**
   - ❌ Lỗi 400/500/404 → Backend có vấn đề
   - ❌ Timeout/Không kết nối được → Backend chưa chạy hoặc sleep

**⚠️ LƯU Ý:** Render Free tier sẽ **sleep sau 15 phút** không có traffic. Khi sleep, request đầu tiên có thể mất 30-60 giây để wake up.

---

### **BƯỚC 2: Kiểm tra VITE_API_BASE trên Netlify**

1. Vào **Netlify** → Site của bạn → **"Site settings"**
2. Tab **"Environment variables"**
3. Kiểm tra có biến `VITE_API_BASE` chưa:
   - ✅ Nếu có → Kiểm tra giá trị có đúng không
   - ❌ Nếu chưa có → Thêm ngay!

**Giá trị đúng:**
```
Key: VITE_API_BASE
Value: https://fastfood-backend-t8jz.onrender.com/api
```
⚠️ **Lưu ý:** Phải có `/api` ở cuối!

4. Nếu đã có nhưng sai, sửa lại
5. **QUAN TRỌNG:** Sau khi sửa/thêm, phải **redeploy Netlify:**
   - Vào tab **"Deploys"**
   - Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

### **BƯỚC 3: Kiểm tra CORS trên Render**

1. Vào **Render** → Service `fastfood-backend` → Tab **"Environment"**
2. Kiểm tra biến `CORS_ORIGINS`:
   - ✅ Phải có: `https://fastfooddatdoan.netlify.app` (KHÔNG có dấu `/`)
   - ❌ Nếu chưa có hoặc sai → Sửa ngay!

**Giá trị đúng:**
```
Key: CORS_ORIGINS
Value: https://fastfooddatdoan.netlify.app
```

3. Click **"Save Changes"** nếu sửa
4. Render sẽ tự động redeploy

---

### **BƯỚC 4: Kiểm tra trong Browser DevTools**

1. Mở website: `https://fastfooddatdoan.netlify.app`
2. Mở **Developer Tools** (F12)
3. Vào tab **"Network"**
4. Thử đăng nhập lại
5. Xem các request:

**Nếu thấy request đến:**
- `https://fastfood-backend-t8jz.onrender.com/api/accounts/login/` → ✅ URL đúng
- `http://localhost:8000/api/...` → ❌ VITE_API_BASE chưa được set

**Nếu request bị lỗi:**
- **CORS error** → CORS_ORIGINS chưa đúng
- **404 Not Found** → URL backend sai
- **500 Internal Server Error** → Backend có lỗi
- **Connection refused** → Backend chưa chạy hoặc sleep

---

### **BƯỚC 5: Kiểm tra Console Logs**

1. Mở Developer Tools (F12) → Tab **"Console"**
2. Thử đăng nhập lại
3. Xem có lỗi gì:
   - `CORS policy` → CORS chưa được cấu hình
   - `Failed to fetch` → Không kết nối được backend
   - `404` → URL sai
   - `NetworkError` → Backend sleep hoặc down

---

## 🔧 **SỬA LỖI:**

### **Lỗi 1: VITE_API_BASE chưa được set**

**Triệu chứng:** Request đến `http://localhost:8000/api/...` hoặc `/api/...`

**Giải pháp:**
1. Vào Netlify → Environment variables
2. Thêm: `VITE_API_BASE` = `https://fastfood-backend-t8jz.onrender.com/api`
3. **Redeploy Netlify** (quan trọng!)

---

### **Lỗi 2: CORS error**

**Triệu chứng:** Console hiện `Access to fetch at '...' has been blocked by CORS policy`

**Giải pháp:**
1. Vào Render → Environment variables
2. Kiểm tra `CORS_ORIGINS` = `https://fastfooddatdoan.netlify.app` (KHÔNG có dấu `/`)
3. Save và chờ redeploy

---

### **Lỗi 3: Backend sleep (Free tier)**

**Triệu chứng:** Request đầu tiên mất rất lâu (30-60 giây) hoặc timeout

**Giải pháp:**
- ⏳ Đợi request đầu tiên (backend sẽ wake up)
- Hoặc dùng service như UptimeRobot để ping định kỳ

---

## 📋 **CHECKLIST KIỂM TRA:**

- [ ] Backend đang Live: `https://fastfood-backend-t8jz.onrender.com/api/` → Có response
- [ ] `VITE_API_BASE` đã được thêm trên Netlify
- [ ] Netlify đã được redeploy sau khi thêm `VITE_API_BASE`
- [ ] `CORS_ORIGINS` đã được thêm trên Render
- [ ] `CORS_ORIGINS` không có dấu `/` ở cuối
- [ ] Network tab trong DevTools cho thấy request đến đúng URL backend

---

## 🧪 **TEST LẠI:**

1. **Đảm bảo đã làm các bước trên**
2. **Clear browser cache** (Ctrl+Shift+Delete)
3. **Refresh trang** (F5)
4. **Thử đăng nhập lại**
5. **Xem Network tab** → Request có thành công không?

---

**Kiểm tra các bước trên và cho tôi biết kết quả!** 🚀

