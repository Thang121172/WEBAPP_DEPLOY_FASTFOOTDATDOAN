# 🔧 Hướng dẫn sửa lỗi kết nối: Set VITE_API_BASE trên Netlify

## ✅ Backend đã chạy OK
Backend URL: `https://fastfood-backend-t8jz.onrender.com/`

## 🔴 Vấn đề: Netlify có thể bị PAUSE

Từ hình ảnh trước, Netlify có banner đỏ: **"This team has exceeded the credit limit. All projects and deploys have been paused"**

### ⚠️ Nếu Netlify bị pause:
- Frontend sẽ KHÔNG thể deploy lại
- Environment variables mới sẽ KHÔNG có hiệu lực
- Cần **upgrade team** hoặc **đợi đến tháng sau**

## 📝 Bước 1: Kiểm tra Netlify Status

1. Vào https://app.netlify.com/
2. Chọn site **`fastfooddatdoan`**
3. Xem có banner đỏ không:
   - **Có banner đỏ** → Netlify bị pause → Cần upgrade hoặc đợi
   - **Không có banner đỏ** → Tiếp tục bước 2

## 📝 Bước 2: Set Environment Variable trên Netlify

**Nếu Netlify KHÔNG bị pause:**

1. Vào https://app.netlify.com/
2. Chọn site **`fastfooddatdoan`**
3. Vào **Site settings** → **Environment variables**
4. Tìm biến `VITE_API_BASE`:
   - **Nếu chưa có**: Click **"Add a variable"**
   - **Nếu đã có**: Click vào để sửa

5. Set giá trị:
   ```
   Key: VITE_API_BASE
   Value: https://fastfood-backend-t8jz.onrender.com/api
   ```
   **Lưu ý:** KHÔNG có dấu `/` ở cuối!

6. Click **"Save"**

## 📝 Bước 3: Redeploy Netlify (QUAN TRỌNG!)

**SAU KHI SET ENV VAR, PHẢI REDEPLOY:**

1. Vào tab **"Deploys"** trên Netlify
2. Click **"Trigger deploy"** → **"Clear cache and deploy site"**
3. Đợi deploy xong (1-2 phút)

## 📝 Bước 4: Kiểm tra CORS trên Render

1. Vào https://dashboard.render.com/
2. Chọn service **`fastfood-backend-t8jz`**
3. Vào tab **"Environment"**
4. Kiểm tra biến `CORS_ORIGINS`:
   ```
   CORS_ORIGINS = https://fastfooddatdoan.netlify.app
   ```
   **Lưu ý:** KHÔNG có dấu `/` ở cuối!

5. Nếu chưa có hoặc sai, thêm/sửa:
   - Key: `CORS_ORIGINS`
   - Value: `https://fastfooddatdoan.netlify.app`
   - Click **"Save Changes"**

## 📝 Bước 5: Test lại

1. Mở browser ở **chế độ incognito** (Ctrl + Shift + N)
2. Truy cập: `https://fastfooddatdoan.netlify.app/register`
3. Thử đăng ký lại
4. Mở **Console** (F12) → **Network** tab để xem request

## ✅ Checklist cuối cùng

- [ ] Netlify KHÔNG bị pause (không có banner đỏ)
- [ ] `VITE_API_BASE` trên Netlify = `https://fastfood-backend-t8jz.onrender.com/api` (KHÔNG có `/` cuối)
- [ ] Đã redeploy Netlify sau khi set env var
- [ ] `CORS_ORIGINS` trên Render = `https://fastfooddatdoan.netlify.app` (KHÔNG có `/` cuối)
- [ ] Test backend: `https://fastfood-backend-t8jz.onrender.com/api/` trả về JSON

## 🆘 Nếu Netlify bị pause

**Option 1: Upgrade Netlify Team**
- Click nút **"Upgrade team"** trong banner đỏ
- Chọn plan phù hợp (có thể free tier mới)

**Option 2: Đợi đến tháng sau**
- Netlify sẽ reset credit limit
- Projects sẽ tự động được restore

**Option 3: Tạo tài khoản Netlify mới**
- Đăng ký email mới
- Deploy lại frontend
- Set lại env vars

