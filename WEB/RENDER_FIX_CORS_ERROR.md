# 🔧 SỬA LỖI: CORS_ALLOWED_ORIGINS should not have path

## ⚠️ **LỖI:**

```
SystemCheckError: 
?: (corsheaders.E014) Origin 'https://fastfooddatdoan.netlify.app/' in CORS_ALLOWED_ORIGINS should not have path
```

## ✅ **NGUYÊN NHÂN:**

URL trong `CORS_ORIGINS` có dấu `/` ở cuối. Django CORS headers chỉ chấp nhận domain, **KHÔNG được có path** (dấu `/`).

- ❌ **SAI:** `https://fastfooddatdoan.netlify.app/`
- ✅ **ĐÚNG:** `https://fastfooddatdoan.netlify.app`

---

## ✅ **CÁCH SỬA:**

### **Bước 1: Vào Environment Variables**

1. Vào Render Dashboard → Service **"fastfood-backend"**
2. Vào tab **"Environment"**
3. Tìm biến **`CORS_ORIGINS`**

### **Bước 2: Sửa giá trị**

1. Click vào biến `CORS_ORIGINS` để chỉnh sửa
2. Xóa dấu `/` ở cuối URL

**Sửa từ:**
```
https://fastfooddatdoan.netlify.app/
```

**Thành:**
```
https://fastfooddatdoan.netlify.app
```

### **Bước 3: Lưu**

1. Click **"Save Changes"**
2. Render sẽ tự động redeploy

---

## ✅ **NẾU CÓ NHIỀU DOMAINS:**

Nếu bạn có nhiều domains, phân cách bằng dấu phẩy và đảm bảo không có dấu `/`:

**ĐÚNG:**
```
https://fastfooddatdoan.netlify.app,https://your-custom-domain.com
```

**SAI:**
```
https://fastfooddatdoan.netlify.app/,https://your-custom-domain.com/
```

---

## 🔍 **KIỂM TRA SAU KHI SỬA:**

1. Xem build logs trong Render
2. Không còn lỗi `should not have path`
3. Build thành công
4. Service Live và hoạt động

---

## ✅ **TÓM TẮT:**

| Trước | Sau | Kết quả |
|-------|-----|---------|
| `https://fastfooddatdoan.netlify.app/` | `https://fastfooddatdoan.netlify.app` | ✅ Đúng |
| Có dấu `/` ở cuối | Không có dấu `/` | ✅ Đúng |

---

**Sửa xong và save, Render sẽ tự động redeploy!** 🚀

