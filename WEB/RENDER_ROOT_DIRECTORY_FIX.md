# Sửa Root Directory trên Render

## 🔴 Vấn đề:
Repo GitHub có cấu trúc:
```
WEBAPP_DEPLOY_FASTFOOTDATDOAN/
  └── WEB/
      ├── Dockerfile
      ├── start.sh
      ├── render.yaml
      ├── backend/
      └── frontend/
```

Nhưng Render đang tìm Dockerfile ở root → Không tìm thấy!

---

## ✅ Giải pháp:

### Cách 1: Set Root Directory = `WEB` (Khuyến nghị)

1. Vào Render Dashboard → Service `fastfood-backend`
2. Vào **Settings**
3. Tìm **Root Directory**
4. Click **Edit**
5. Nhập: `WEB` (chữ hoa)
6. Click **Save**

### Cách 2: Sửa Dockerfile Path

Nếu không muốn đổi Root Directory:
1. **Root Directory**: Để trống
2. **Dockerfile Path**: `WEB/Dockerfile`
3. **Docker Build Context Directory**: `WEB`

---

## 📋 Cấu hình đúng (Cách 1 - Khuyến nghị):

| Field | Giá trị |
|-------|---------|
| **Root Directory** | `WEB` |
| **Dockerfile Path** | `./Dockerfile` |
| **Docker Build Context Directory** | `.` |
| **Pre-Deploy Command** | (trống) |

---

## 📋 Cấu hình đúng (Cách 2):

| Field | Giá trị |
|-------|---------|
| **Root Directory** | (trống) |
| **Dockerfile Path** | `WEB/Dockerfile` |
| **Docker Build Context Directory** | `WEB` |
| **Pre-Deploy Command** | (trống) |

---

## 🚀 Sau khi sửa:

1. **Lưu tất cả thay đổi**
2. Vào **Manual Deploy**
3. Click **"Clear build cache & deploy"**
4. Render sẽ tìm thấy Dockerfile trong thư mục `WEB/`

---

## ✅ Kết quả mong đợi:

- Render clone repo về root
- Render chuyển vào thư mục `WEB/` (Root Directory)
- Tìm thấy `Dockerfile` ở `./Dockerfile` (tương đương `WEB/Dockerfile`)
- Build thành công!

