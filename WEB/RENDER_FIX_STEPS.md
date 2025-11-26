# Các bước sửa cấu hình Render

## 🔴 Vấn đề hiện tại:
- **Root Directory**: `backend` ❌ (phải để trống)
- **Dockerfile Path**: `backend/ ./Dockerfile` ❌ (phải là `./Dockerfile`)
- **Docker Build Context Directory**: `backend/ .` ❌ (phải là `.`)
- **Pre-Deploy Command**: `backend/ $` ❌ (nên để trống vì đã có trong start.sh)

---

## ✅ Cách sửa:

### 1. **Root Directory** (Quan trọng nhất!)
- Click **Edit** (bút chì icon)
- **XÓA** giá trị `backend`
- **Để TRỐNG** (không nhập gì)
- Click **Save**

### 2. **Dockerfile Path**
- Click **Edit**
- Xóa `backend/ ./Dockerfile`
- Nhập: `./Dockerfile`
- Click **Save**

### 3. **Docker Build Context Directory**
- Click **Edit**
- Xóa `backend/ .`
- Nhập: `.` (chỉ một dấu chấm)
- Click **Save**

### 4. **Pre-Deploy Command**
- Click **Edit**
- Xóa `backend/ $`
- **Để TRỐNG** (không cần vì migrations đã có trong start.sh)
- Click **Save**

### 5. **Docker Command**
- Giữ nguyên (để trống) - Dockerfile đã có CMD

---

## 📋 Tóm tắt giá trị đúng:

| Field | Giá trị đúng |
|-------|--------------|
| Root Directory | **(trống)** |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context Directory | `.` |
| Pre-Deploy Command | **(trống)** |
| Docker Command | **(trống)** |

---

## 🚀 Sau khi sửa:

1. **Lưu tất cả thay đổi**
2. Vào tab **Manual Deploy**
3. Click **"Clear build cache & deploy"**
4. Render sẽ build lại với cấu hình đúng

---

## ✅ Kết quả mong đợi:

- Render sẽ clone repo về root
- Build Docker image từ `Dockerfile` ở root
- Build context là root directory (`.`)
- Không còn lỗi "backend directory missing"

