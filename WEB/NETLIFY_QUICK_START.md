# ⚡ HƯỚNG DẪN NHANH - BẠN ĐANG Ở TRANG PROJECTS

## 🎯 **BẠN CẦN LÀM GÌ TIẾP THEO?**

Bạn đang ở trang **Projects** trên Netlify. Có 2 lựa chọn:

---

## ✅ **LỰA CHỌN 1: TẠO PROJECT MỚI (KHUYÊN DÙNG)**

### **Bước 1: Click "Add new project"**
- Ở góc trên bên phải, click nút màu xanh lá **"Add new project"**
- Chọn **"Import an existing project"**

### **Bước 2: Kết nối Git**
- Chọn **GitHub** (hoặc GitLab/Bitbucket nếu bạn dùng)
- Đăng nhập và authorize Netlify truy cập repository
- Chọn repository **fastfood** của bạn

### **Bước 3: Cấu hình Build Settings**
⚠️ **QUAN TRỌNG - Phải cấu hình đúng:**

```
Base directory:    frontend
Build command:     npm run build
Publish directory: dist
```

### **Bước 4: Deploy**
- Click **"Deploy site"**
- Chờ build xong (khoảng 1-2 phút)

---

## 🔄 **LỰA CHỌN 2: DÙNG PROJECT HIỆN TẠI**

Nếu bạn muốn cấu hình lại project **"whimsical-licorice-884129"**:

### **Bước 1: Click vào project**
- Click vào tên project để vào trang chi tiết

### **Bước 2: Vào Site settings**
- Click **"Site settings"** (icon ⚙️ ở menu trên cùng)

### **Bước 3: Cấu hình Build settings**
- Vào **"Build & deploy"** → **"Build settings"**
- Click **"Edit settings"**
- Cập nhật:
  - **Base directory:** `frontend`
  - **Build command:** `npm run build`
  - **Publish directory:** `dist`
- Click **"Save"**

### **Bước 4: Trigger deploy mới**
- Vào tab **"Deploys"**
- Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## 🔧 **SAU KHI DEPLOY XONG:**

### **1. Thêm Environment Variable:**
- Vào **"Site settings"** → **"Environment variables"**
- Click **"Add a variable"**
- Thêm:
  ```
  Key:   VITE_API_BASE
  Value: https://your-backend-url.com/api
  ```
  (Thay `your-backend-url.com` bằng URL backend thực tế của bạn)

### **2. Redeploy để áp dụng biến môi trường:**
- Vào **"Deploys"** → **"Trigger deploy"** → **"Clear cache and deploy site"**

### **3. Kiểm tra:**
- Mở URL site: `https://your-site-name.netlify.app`
- Test xem app có chạy không

---

## ❓ **BẠN MUỐN LÀM GÌ?**

1. **Tạo project mới** → Click "Add new project" (góc trên phải)
2. **Cấu hình project hiện tại** → Click vào project "whimsical-licorice-884129"

---

**📖 Xem hướng dẫn chi tiết đầy đủ trong file `NETLIFY_DEPLOY_GUIDE.md`**

