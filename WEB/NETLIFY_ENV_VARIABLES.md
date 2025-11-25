# 🔧 HƯỚNG DẪN CẤU HÌNH ENVIRONMENT VARIABLES TRÊN NETLIFY

## 📋 **1. FUNCTIONS DIRECTORY**

### ❓ **Có cần thiết không?**
**KHÔNG!** Project này không dùng Netlify Functions.

### ✅ **Làm gì:**
- **Để trống** hoặc **xóa** nội dung trong ô "Functions directory"
- Hoặc để nguyên `netlify/functions` (không ảnh hưởng gì)

---

## 📋 **2. ENVIRONMENT VARIABLES - CẦN THIẾT!**

### ✅ **BẮT BUỘC PHẢI THÊM:**

#### **Biến 1: VITE_API_BASE** (QUAN TRỌNG NHẤT!)

**Cách thêm:**
1. Click **"Add environment variables"** → **"Add key/value pairs"**
2. Thêm:
   ```
   Key:   VITE_API_BASE
   Value: https://your-backend-url.com/api
   ```

**Ví dụ giá trị:**
- Nếu backend chạy trên VPS: `https://103.75.182.180:8000/api`
- Hoặc nếu có domain: `https://api.yourdomain.com/api`
- Nếu backend chạy trên Render: `https://your-backend.onrender.com/api`

**⚠️ LƯU Ý:**
- URL phải có `/api` ở cuối
- Phải là HTTPS (không dùng HTTP)
- Không có `/` ở cuối URL (ví dụ: `/api` chứ không phải `/api/`)

---

### 🎯 **TÙY CHỌN (chỉ nếu cần):**

#### **Biến 2: VITE_MAPBOX_TOKEN** (Chỉ cần nếu dùng Mapbox)

Nếu bạn muốn dùng Mapbox thay vì OpenStreetMap:
1. Đăng ký tài khoản Mapbox tại: https://www.mapbox.com
2. Lấy Access Token
3. Thêm biến:
   ```
   Key:   VITE_MAPBOX_TOKEN
   Value: pk.your_mapbox_token_here
   ```

**Nếu không thêm:** App sẽ dùng OpenStreetMap (miễn phí) ✅

---

## 📝 **CÁCH THÊM TRONG NETLIFY UI:**

### **Bước 1: Click "Add environment variables"**
- Bạn đang thấy dropdown với 2 options:
  - "Add key/value pairs" ← **CHỌN CÁI NÀY**
  - "Import from a .env file" (không cần vì không có file .env)

### **Bước 2: Thêm key/value pair**
- Click **"Add key/value pairs"**
- Một form sẽ hiện ra với 2 ô:
  - **Key:** Gõ `VITE_API_BASE`
  - **Value:** Gõ URL backend của bạn (ví dụ: `https://103.75.182.180:8000/api`)

### **Bước 3: Lưu**
- Click nút **"Save"** hoặc **"Add"**

### **Bước 4: Redeploy**
- Sau khi thêm, **BẮT BUỘC** phải redeploy:
  - Vào tab **"Deploys"**
  - Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## 🔍 **KIỂM TRA:**

Sau khi deploy, kiểm tra trong browser:

1. Mở trang web trên Netlify
2. Mở **Developer Tools** (F12)
3. Vào tab **Console**
4. Gõ: `console.log(import.meta.env.VITE_API_BASE)`
5. Phải thấy URL backend của bạn

Hoặc trong **Network** tab:
- Khi app gọi API, phải thấy request đến URL backend đúng

---

## ❌ **LỖI THƯỜNG GẶP:**

### **Lỗi: API calls thất bại**
- ✅ Kiểm tra `VITE_API_BASE` đã được thêm chưa
- ✅ Kiểm tra URL có đúng không (có `/api` ở cuối)
- ✅ Kiểm tra backend có chạy và accessible không
- ✅ Kiểm tra CORS trên backend (phải cho phép domain Netlify)

### **Lỗi: Biến môi trường không hoạt động**
- ✅ Đảm bảo tên biến bắt đầu bằng `VITE_` (Vite requirement)
- ✅ Đã redeploy sau khi thêm biến chưa?
- ✅ Kiểm tra trong build logs xem biến có được inject không

---

## ✅ **TÓM TẮT:**

| Cấu hình | Cần thiết? | Giá trị |
|----------|-----------|---------|
| **Functions directory** | ❌ KHÔNG | Để trống hoặc xóa |
| **VITE_API_BASE** | ✅ **CÓ** | `https://your-backend-url.com/api` |
| **VITE_MAPBOX_TOKEN** | ⚠️ Tùy chọn | Chỉ nếu dùng Mapbox |

---

**⚠️ QUAN TRỌNG: Sau khi thêm biến môi trường, NHỚ REDEPLOY!**

