# ✅ Cấu hình Vercel hoàn chỉnh

## 📋 Kiểm tra Build Settings trên Vercel

Dựa vào hình ảnh, bạn đã có:

### ✅ Đã đúng:
- **Root Directory:** `frontend` ✅
- **Build Command:** `npm install && npm run build` ✅
- **Output Directory:** `dist` ✅ (relative to `frontend/`)

### ⚠️ Cần kiểm tra:

1. **Build Command có Override bật:** ✅ OK
2. **Output Directory có Override bật:** ✅ OK
3. **Root Directory:** `frontend` ✅ OK

---

## 🔧 Bước tiếp theo: Set Environment Variables

### 1. Click "Environment Variables" (ở menu bên trái hoặc trên cùng)

### 2. Thêm biến mới:

Click **"Add New"** hoặc **"Add Environment Variable"**

**Thêm biến 1:**
```
Name: VITE_API_BASE
Value: https://fastfood-backend-t8jz.onrender.com/api
```
⚠️ **KHÔNG có dấu `/` ở cuối!**

Chọn environments: ✅ Production, ✅ Preview, ✅ Development

**Thêm biến 2 (nếu có Mapbox token):**
```
Name: VITE_MAPBOX_TOKEN
Value: (token của bạn)
```

### 3. Click "Save" hoặc "Add"

---

## 📝 Sau khi set Environment Variables

### 1. Save Build Settings (nếu có nút Save)

### 2. Deploy lại (nếu đã deploy rồi):

1. Vào tab **"Deployments"**
2. Click **"..."** ở deployment mới nhất
3. Click **"Redeploy"**
4. Đợi 1-2 phút

### 3. Hoặc Deploy lần đầu:

1. Scroll xuống dưới
2. Click nút **"Deploy"** (màu đen)
3. Đợi deploy xong

---

## ✅ Checklist hoàn chỉnh

Sau khi setup xong, đảm bảo:

- [ ] **Root Directory:** `frontend`
- [ ] **Build Command:** `npm install && npm run build` (Override: ON)
- [ ] **Output Directory:** `dist` (Override: ON)
- [ ] **Environment Variable:** `VITE_API_BASE` = `https://fastfood-backend-t8jz.onrender.com/api` (KHÔNG có `/` cuối)
- [ ] Đã Deploy/Redeploy
- [ ] URL Vercel: `https://your-project.vercel.app`

---

## 🧪 Test sau khi deploy

1. Truy cập URL Vercel của bạn
2. Mở **Console** (F12)
3. Chạy:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
   - Phải hiển thị: `https://fastfood-backend-t8jz.onrender.com/api`
   - Nếu `undefined` → Chưa set env var hoặc chưa redeploy

4. Vào trang đăng ký: `/register`
5. Mở **Network** tab (F12)
6. Thử đăng ký
7. Request URL phải là: `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`
   - ✅ Đúng
   - ❌ Không phải `localhost:8000` hoặc `/api/...` (relative)

---

## 🔄 Update CORS trên Render

Sau khi có URL Vercel:

1. Vào https://dashboard.render.com/
2. Chọn service `fastfood-backend-t8jz`
3. Vào tab **"Environment"**
4. Tìm `CORS_ORIGINS`:
   - Thêm URL Vercel: `https://your-project.vercel.app`
   - Có thể giữ Netlify nếu muốn: `https://your-project.vercel.app,https://fastfooddatdoan.netlify.app`
5. Click **"Save Changes"**

---

## 🆘 Nếu vẫn lỗi

### Lỗi: Build failed
- Kiểm tra lại Build Command và Output Directory
- Xem logs trong Vercel Deployments để biết lỗi cụ thể

### Lỗi: Environment variable không có hiệu lực
- Đảm bảo biến bắt đầu bằng `VITE_`
- Redeploy lại project
- Hard refresh browser (Ctrl + Shift + R)

### Lỗi: CORS error
- Kiểm tra `CORS_ORIGINS` trên Render
- Đảm bảo có URL Vercel
- Save và restart service trên Render

