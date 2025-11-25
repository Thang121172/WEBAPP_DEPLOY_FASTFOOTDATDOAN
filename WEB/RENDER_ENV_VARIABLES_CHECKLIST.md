# 📝 CHECKLIST: ENVIRONMENT VARIABLES CHO RENDER

## ✅ **CÁC BIẾN ĐÃ CÓ SẴN TRONG render.yaml (TỰ ĐỘNG):**

Những biến này đã được cấu hình trong `render.yaml`, Render sẽ tự động tạo:

| Key | Value | Ghi chú |
|-----|-------|---------|
| `SECRET_KEY` | *(Auto-generated)* | Render tự động generate ✅ |
| `ENVIRONMENT` | `Production` | Đã set sẵn ✅ |
| `DJANGO_SETTINGS_MODULE` | `core.settings.prod` | Đã set sẵn ✅ |
| `RENDER_EXTERNAL_HOSTNAME` | *(Auto)* | Render tự động lấy từ service ✅ |
| `CELERY_BROKER_URL` | `""` (empty) | Đã set sẵn (để trống) ✅ |

---

## ⚠️ **CÁC BIẾN CẦN ĐIỀN THỦ CÔNG:**

Bạn cần vào **Render Dashboard** → **Service "fastfood-backend"** → **Tab "Environment"** → **"Add Environment Variable"** và thêm các biến sau:

---

### **1. DATABASE_URL** ⭐ **QUAN TRỌNG NHẤT!**

**Cách lấy giá trị:**
1. Vào Render Dashboard
2. Vào **PostgreSQL Database** service (`fastfood-db`)
3. Vào tab **"Connections"** hoặc **"Info"**
4. Copy **"Internal Database URL"**
   - Format: `postgresql://user:password@host:port/dbname`
   - ⚠️ **QUAN TRỌNG:** Phải dùng **Internal URL**, không phải External!

**Thêm vào Render:**
```
Key: DATABASE_URL
Value: [Paste Internal Database URL ở đây]
```

**Ví dụ:**
```
postgresql://fastfood_user:abc123@dpg-xxxxx-a/fastfood_db
```

---

### **2. CORS_ORIGINS** ⭐ **CẦN THIẾT!**

**Cách lấy giá trị:**
1. Vào **Netlify Dashboard**
2. Vào site của bạn
3. Copy URL (ví dụ: `https://your-site-name.netlify.app`)

**Thêm vào Render:**
```
Key: CORS_ORIGINS
Value: https://your-site-name.netlify.app
```

**Nếu có nhiều domains (Netlify + custom domain):**
```
Key: CORS_ORIGINS
Value: https://your-site-name.netlify.app,https://your-custom-domain.com
```
- Phân cách bằng dấu phẩy `,`

**Ví dụ:**
```
https://fastfood-app.netlify.app
```

---

### **3. ALLOWED_HOSTS** (Tùy chọn - có thể không cần)

**Giá trị:**
- Render tự động lấy từ `RENDER_EXTERNAL_HOSTNAME`
- Nếu muốn set thủ công:

**Cách lấy:**
1. Vào Render Dashboard → Service `fastfood-backend`
2. Copy URL hiển thị (ví dụ: `fastfood-backend-xxxx.onrender.com`)

**Thêm vào Render:**
```
Key: ALLOWED_HOSTS
Value: fastfood-backend-xxxx.onrender.com
```

**Nếu có nhiều domains:**
```
Key: ALLOWED_HOSTS
Value: fastfood-backend-xxxx.onrender.com,your-custom-domain.com
```

**⚠️ LƯU Ý:** Thường không cần vì `RENDER_EXTERNAL_HOSTNAME` đã tự động set.

---

## 📋 **CHECKLIST ĐIỀN BIẾN:**

### **Bước 1: Tạo PostgreSQL Database**
- [ ] Tạo database trên Render
- [ ] Copy Internal Database URL

### **Bước 2: Thêm Environment Variables**
- [ ] Thêm `DATABASE_URL` (từ Internal Database URL)
- [ ] Thêm `CORS_ORIGINS` (URL Netlify của bạn)
- [ ] (Tùy chọn) Thêm `ALLOWED_HOSTS`

### **Bước 3: Lưu và Deploy**
- [ ] Click "Save Changes"
- [ ] Chờ Render redeploy tự động

---

## 📝 **MẪU COPY-PASTE:**

Copy các dòng sau và thay giá trị của bạn:

```bash
# 1. DATABASE_URL - Thay bằng Internal Database URL từ Render
DATABASE_URL=postgresql://fastfood_user:password@dpg-xxxxx-a/fastfood_db

# 2. CORS_ORIGINS - Thay bằng URL Netlify của bạn
CORS_ORIGINS=https://your-site-name.netlify.app

# 3. ALLOWED_HOSTS - Thay bằng URL Render service (tùy chọn)
ALLOWED_HOSTS=fastfood-backend-xxxx.onrender.com
```

---

## 🔍 **CÁCH KIỂM TRA SAU KHI ĐIỀN:**

1. Vào Render → Service `fastfood-backend` → Tab "Environment"
2. Kiểm tra xem các biến đã có chưa:
   - ✅ `DATABASE_URL` - Có giá trị
   - ✅ `CORS_ORIGINS` - Có URL Netlify
   - ✅ `SECRET_KEY` - Tự động có (từ render.yaml)
   - ✅ `RENDER_EXTERNAL_HOSTNAME` - Tự động có

---

## ⚠️ **LƯU Ý QUAN TRỌNG:**

1. **DATABASE_URL:**
   - ⚠️ **PHẢI** dùng **Internal Database URL** (không phải External)
   - Internal URL chỉ hoạt động giữa các services trong cùng Render

2. **CORS_ORIGINS:**
   - ⚠️ **PHẢI** có `https://` ở đầu
   - ⚠️ **KHÔNG** có dấu `/` ở cuối (trừ khi có subpath)

3. **Sau khi thêm biến:**
   - Render sẽ tự động **redeploy** service
   - Chờ deploy xong (khoảng 5-10 phút)
   - Kiểm tra logs xem có lỗi không

---

## 🎯 **TÓM TẮT CÁC BIẾN BẮT BUỘC:**

| # | Key | Bắt buộc? | Cách lấy |
|---|-----|-----------|----------|
| 1 | `DATABASE_URL` | ✅ **CÓ** | Internal URL từ PostgreSQL service |
| 2 | `CORS_ORIGINS` | ✅ **CÓ** | URL từ Netlify site |
| 3 | `ALLOWED_HOSTS` | ⚠️ Tùy chọn | URL Render service (auto nếu có RENDER_EXTERNAL_HOSTNAME) |
| 4 | `SECRET_KEY` | ✅ Tự động | Render generate từ render.yaml |
| 5 | `RENDER_EXTERNAL_HOSTNAME` | ✅ Tự động | Render auto-set |

---

**Sau khi điền xong, Render sẽ tự động redeploy!** 🚀

