# 🔧 Hướng dẫn sửa lỗi "Lỗi kết nối máy chủ"

## ✅ Backend đã OK
- URL: `https://fastfood-backend-t8jz.onrender.com/`
- API endpoint: `https://fastfood-backend-t8jz.onrender.com/api/` ✅ Đang chạy

## 🔴 Vấn đề chính: Netlify có thể bị PAUSE

### ⚠️ Bước 0: Kiểm tra Netlify có bị pause không

1. Vào https://app.netlify.com/
2. Chọn site **`fastfooddatdoan`**
3. Xem có **banner đỏ** ở đầu trang không:
   ```
   "This team has exceeded the credit limit. 
   All projects and deploys have been paused..."
   ```

**Nếu CÓ banner đỏ:**
- Netlify bị pause → Frontend KHÔNG thể update
- **Giải pháp:** 
  - Click **"Upgrade team"** trong banner (nếu có budget)
  - HOẶC đợi đến tháng sau (Netlify sẽ reset)
  - HOẶC tạo tài khoản Netlify mới và deploy lại

**Nếu KHÔNG có banner đỏ:**
- Tiếp tục bước 1

---

## 📝 Bước 1: Set VITE_API_BASE trên Netlify

1. Vào https://app.netlify.com/
2. Chọn site **`fastfooddatdoan`**
3. Vào **Site settings** (icon bánh răng ở menu bên trái)
4. Scroll xuống, tìm **"Environment variables"**
5. Kiểm tra có biến `VITE_API_BASE` chưa:
   - **Nếu chưa có**: Click **"Add a variable"**
   - **Nếu đã có**: Click để sửa
6. Set giá trị:
   ```
   Key: VITE_API_BASE
   Value: https://fastfood-backend-t8jz.onrender.com/api
   ```
   ⚠️ **QUAN TRỌNG:** KHÔNG có dấu `/` ở cuối!

7. Click **"Save"**

---

## 📝 Bước 2: Redeploy Netlify (BẮT BUỘC!)

**SAU KHI SET ENV VAR, PHẢI REDEPLOY:**

1. Vào tab **"Deploys"** (ở menu bên trái)
2. Ở góc trên bên phải, click **"Trigger deploy"**
3. Chọn **"Clear cache and deploy site"**
4. Đợi deploy xong (1-2 phút)
5. Xem status: Phải là **"Published"** (màu xanh)

---

## 📝 Bước 3: Kiểm tra CORS trên Render

1. Vào https://dashboard.render.com/
2. Chọn service **`fastfood-backend-t8jz`**
3. Vào tab **"Environment"**
4. Kiểm tra biến `CORS_ORIGINS`:
   - **Phải có giá trị**: `https://fastfooddatdoan.netlify.app`
   - **KHÔNG có dấu `/` ở cuối!**

5. Nếu chưa có hoặc sai:
   - Click **"Add Environment Variable"** hoặc sửa biến có sẵn
   - Key: `CORS_ORIGINS`
   - Value: `https://fastfooddatdoan.netlify.app`
   - Click **"Save Changes"**

---

## 📝 Bước 4: Test lại

1. Mở browser ở **chế độ incognito** (Ctrl + Shift + N)
2. Truy cập: `https://fastfooddatdoan.netlify.app/register`
3. Mở **Developer Tools** (F12)
4. Vào tab **Console**
5. Chạy lệnh này để kiểm tra env var:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
   - Nếu hiển thị: `https://fastfood-backend-t8jz.onrender.com/api` → ✅ OK
   - Nếu hiển thị: `undefined` → ❌ Env var chưa được set đúng

6. Thử đăng ký lại
7. Xem tab **Network** (F12) khi click đăng ký:
   - Request URL phải là: `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`
   - Nếu là `/api/...` (relative) → Env var chưa có hiệu lực

---

## ✅ Checklist

Trước khi test lại, đảm bảo:

- [ ] Netlify **KHÔNG bị pause** (không có banner đỏ)
- [ ] `VITE_API_BASE` trên Netlify = `https://fastfood-backend-t8jz.onrender.com/api` (KHÔNG có `/` cuối)
- [ ] Đã **redeploy Netlify** sau khi set env var (Clear cache)
- [ ] `CORS_ORIGINS` trên Render = `https://fastfooddatdoan.netlify.app` (KHÔNG có `/` cuối)
- [ ] Test backend: `https://fastfood-backend-t8jz.onrender.com/api/` trả về JSON ✅

---

## 🆘 Nếu vẫn lỗi

**Debug trong Console:**

1. Mở Console (F12)
2. Kiểm tra error message:
   - `CORS policy` → CORS chưa đúng
   - `Failed to fetch` → Backend không reachable
   - `404 Not Found` → URL sai

3. Gửi cho tôi:
   - Screenshot của Console tab
   - Screenshot của Network tab
   - Giá trị `VITE_API_BASE` trên Netlify

