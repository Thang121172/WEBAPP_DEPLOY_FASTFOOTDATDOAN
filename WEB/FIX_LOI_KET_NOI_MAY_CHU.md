# 🔧 SỬA LỖI: "Lỗi kết nối máy chủ"

## ⚠️ **LỖI:**
Khi đăng nhập/nhấn chức năng, hiện thông báo: **"Lỗi kết nối máy chủ"**

## 🔍 **NGUYÊN NHÂN:**

Frontend không biết URL backend. Trong code có:
```typescript
const API_BASE = import.meta.env.VITE_API_BASE || "/api";
```

Nếu `VITE_API_BASE` chưa được set → Frontend sẽ dùng `/api` (không hoạt động trên production!)

---

## ✅ **CÁCH SỬA:**

### **BƯỚC 1: Kiểm tra VITE_API_BASE trên Netlify**

1. Vào **Netlify** → Site của bạn → **"Site settings"**
2. Tab **"Environment variables"**
3. Kiểm tra có biến `VITE_API_BASE` chưa

**Nếu CHƯA CÓ hoặc SAI:**
4. Click **"Add environment variable"** (hoặc Edit nếu có rồi)
5. Thêm/sửa:
   ```
   Key: VITE_API_BASE
   Value: https://fastfood-backend-t8jz.onrender.com/api
   ```
   ⚠️ **Lưu ý:**
   - Phải có `https://`
   - Phải có `/api` ở cuối
   - Thay `fastfood-backend-t8jz` bằng URL Render thực tế của bạn!

6. Click **"Save"**

### **BƯỚC 2: REDEPLOY NETLIFY (QUAN TRỌNG!)**

⚠️ **SAU KHI THÊM/SỬA BIẾN MÔI TRƯỜNG, PHẢI REDEPLOY!**

1. Vào tab **"Deploys"** trên Netlify
2. Click **"Trigger deploy"** → **"Clear cache and deploy site"**
3. Chờ deploy xong (2-3 phút)

**TẠI SAO PHẢI REDEPLOY?**
- Biến môi trường chỉ được inject vào code khi **build**
- Nếu không redeploy, code vẫn dùng giá trị cũ!

---

## 🔍 **KIỂM TRA SAU KHI REDEPLOY:**

### **Cách 1: Xem trong Browser Console**

1. Mở website: `https://fastfooddatdoan.netlify.app`
2. Mở **Developer Tools** (F12) → Tab **"Console"**
3. Gõ lệnh:
   ```javascript
   console.log(import.meta.env.VITE_API_BASE)
   ```
4. Kết quả:
   - ✅ Nếu thấy: `https://fastfood-backend-t8jz.onrender.com/api` → **Đúng!**
   - ❌ Nếu thấy: `undefined` hoặc `/api` → Chưa được set hoặc chưa redeploy

### **Cách 2: Xem Network Tab**

1. Mở Developer Tools (F12) → Tab **"Network"**
2. Thử đăng nhập
3. Xem request:
   - ✅ Nếu request đến: `https://fastfood-backend-t8jz.onrender.com/api/accounts/login/` → **Đúng!**
   - ❌ Nếu request đến: `/api/accounts/login/` → VITE_API_BASE chưa được set

---

## ⚠️ **CÁC LỖI KHÁC CÓ THỂ GẶP:**

### **Lỗi 1: CORS block**

**Triệu chứng:** Console hiện `Access to fetch at '...' has been blocked by CORS policy`

**Giải pháp:**
1. Vào Render → Environment variables
2. Kiểm tra `CORS_ORIGINS` = `https://fastfooddatdoan.netlify.app` (KHÔNG có dấu `/`)

### **Lỗi 2: Backend sleep (Free tier)**

**Triệu chứng:** Request đầu tiên mất rất lâu (30-60 giây)

**Giải pháp:**
- ⏳ Đợi request đầu tiên (backend sẽ wake up)
- Hoặc truy cập URL backend trực tiếp trước để wake up

### **Lỗi 3: Backend 404/500**

**Triệu chứng:** Network tab hiện 404 hoặc 500

**Giải pháp:**
- Xem logs trong Render để tìm lỗi cụ thể

---

## 📋 **CHECKLIST:**

- [ ] `VITE_API_BASE` đã được thêm trên Netlify
- [ ] Giá trị đúng: `https://fastfood-backend-t8jz.onrender.com/api`
- [ ] Netlify đã được **redeploy** sau khi thêm biến
- [ ] Console log cho thấy `VITE_API_BASE` có giá trị đúng
- [ ] Network tab cho thấy request đến đúng URL backend

---

## ✅ **SAU KHI SỬA:**

1. ✅ Frontend biết URL backend
2. ✅ API calls sẽ đến đúng URL
3. ✅ Đăng nhập/đăng ký sẽ hoạt động

---

**Làm Bước 1 và Bước 2, sau đó test lại!** 🚀

