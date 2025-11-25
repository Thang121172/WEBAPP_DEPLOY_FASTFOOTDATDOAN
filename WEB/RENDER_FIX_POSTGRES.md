# 🔧 SỬA LỖI: unknown type "postgres"

## ⚠️ **VẤN ĐỀ:**

Render báo lỗi:
```
unknown type "postgres"
```

## ✅ **NGUYÊN NHÂN:**

Render yêu cầu type phải là **`postgresql`** (đầy đủ), không phải **`postgres`** (viết tắt).

## ✅ **GIẢI PHÁP:**

Đã sửa file `render.yaml`:
- ❌ `type: postgres` (SAI)
- ✅ `type: postgresql` (ĐÚNG)

---

## 📝 **CÁC BƯỚC TIẾP THEO:**

1. **File đã được sửa:** `render.yaml` đã được cập nhật
2. **Commit và push:**
   ```powershell
   git add render.yaml
   git commit -m "Fix render.yaml: change postgres to postgresql"
   git push origin main
   ```

3. **Quay lại Render:**
   - Click **"Retry"** trên trang Blueprint
   - Render sẽ đọc lại file `render.yaml` với type đúng

---

## ✅ **SAU KHI SỬA:**

File `render.yaml` bây giờ có:
```yaml
services:
  - type: postgresql  # ✅ ĐÚNG
    name: fastfood-db
    plan: free
    ...
```

---

**Sau khi push, quay lại Render và click "Retry" nhé!** 🚀

