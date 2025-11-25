# 🔐 TÀI KHOẢN MERCHANT ĐỂ TEST

## 📋 **TÀI KHOẢN MẶC ĐỊNH (Dùng cho nhiều cửa hàng)**

### **Tài khoản chung:**
- **Username:** `merchant_default` hoặc `merchant@fastfood.com`
- **Password:** `Merchant123`
- **Email:** `merchant@fastfood.com`

> ⚠️ **Lưu ý:** Nhiều cửa hàng dùng chung tài khoản này. Khi đăng nhập, bạn sẽ thấy tất cả cửa hàng mà tài khoản này sở hữu.

---

## 📍 **TÀI KHOẢN THEO KHU VỰC**

### **1. TP.HCM (33 cửa hàng)**

**Tài khoản mặc định:**
- **Username:** `merchant_default`
- **Email:** `merchant@fastfood.com`
- **Password:** `Merchant123`

**Ví dụ một số cửa hàng:**
- Quán Bún Bò Huế 2 - TP.HCM
- Cơm Tấm Cali 2 - TP.HCM
- Quán Bánh Xèo 2 - TP.HCM
- Nhà Hàng Hàn Quốc - TP.HCM
- ... (xem file `backend/tai_khoan_cua_hang_tphcm.txt`)

---

### **2. Bình Dương (33 cửa hàng)**

**Tài khoản mặc định:**
- **Username:** `merchant_default`
- **Email:** `merchant@fastfood.com`
- **Password:** `Merchant123`

**Tài khoản riêng (ví dụ):**
- **Username:** `merchant_35_quanbanhcuốnbin`
- **Email:** `merchant_35_quanbanhcuốnbin@fastfood.local`
- **Password:** `Merchant123`
- **Cửa hàng:** Quán Bánh Cuốn - Bình Dương

- **Username:** `merchant_36_nhahangdimsumbi`
- **Email:** `merchant_36_nhahangdimsumbi@fastfood.local`
- **Password:** `Merchant123`
- **Cửa hàng:** Nhà Hàng Dimsum - Bình Dương

... (xem file `backend/tai_khoan_cua_hang_binh_duong.txt`)

---

### **3. Đồng Nai - Biên Hòa (34 cửa hàng)**

**Tài khoản riêng (ví dụ):**
- **Username:** `merchant_1_nhahanghaisanbi`
- **Email:** `merchant_1_nhahanghaisanbi@fastfood.local`
- **Password:** `Merchant123`
- **Cửa hàng:** Nhà Hàng Hải Sản - Biên Hòa

- **Username:** `merchant_2_quanbunthitnướn`
- **Email:** `merchant_2_quanbunthitnướn@fastfood.local`
- **Password:** `Merchant123`
- **Cửa hàng:** Quán Bún Thịt Nướng - Biên Hòa

- **Username:** `merchant_3_cơmtấmcali1biên`
- **Email:** `merchant_3_cơmtấmcali1biên@fastfood.local`
- **Password:** `Merchant123`
- **Cửa hàng:** Cơm Tấm Cali 1 - Biên Hòa

... (xem file `backend/tai_khoan_cua_hang_dong_nai.txt`)

---

## 🎯 **TÀI KHOẢN TỪ SCRIPT SEED (Gần vị trí của bạn)**

Nếu bạn đã chạy script `seed_merchants_simple.py`, có các tài khoản sau:

1. **Quán Cơm Gia Đình**
   - **Username:** `quancom_bienhoa`
   - **Email:** `quancom@example.com`
   - **Password:** `Password123`

2. **Pizza & Pasta House**
   - **Username:** `pizza_bienhoa`
   - **Email:** `pizza@example.com`
   - **Password:** `Password123`

3. **Bún Thịt Nướng Cô Ba**
   - **Username:** `bunthitnuong_bienhoa`
   - **Email:** `bunthitnuong@example.com`
   - **Password:** `Password123`

4. **Phở Gia Truyền**
   - **Username:** `pho_bienhoa`
   - **Email:** `pho@example.com`
   - **Password:** `Password123`

5. **Cà Phê Sảng**
   - **Username:** `cafesang_bienhoa`
   - **Email:** `cafesang@example.com`
   - **Password:** `Password123`

---

## 🚀 **CÁCH SỬ DỤNG**

### **Bước 1: Đăng nhập**
1. Mở trang đăng nhập: `http://localhost:5173/login`
2. Nhập **Username** hoặc **Email** và **Password**
3. Click "Đăng nhập"

### **Bước 2: Chọn vai trò**
- Sau khi đăng nhập, hệ thống sẽ tự động chuyển đến trang tương ứng với role:
  - **Merchant** → Trang quản lý cửa hàng
  - **Customer** → Trang khách hàng
  - **Shipper** → Trang shipper

### **Bước 3: Test chức năng Merchant**
- Xem danh sách đơn hàng
- Xác nhận/hủy đơn hàng
- Quản lý menu
- Xem thống kê

---

## 💡 **GỢI Ý**

**Để test nhanh nhất:**
1. Dùng tài khoản: `merchant_default` / `Merchant123`
2. Hoặc dùng: `quancom_bienhoa` / `Password123` (nếu đã seed data)

**Để test với cửa hàng cụ thể:**
- Dùng username riêng của cửa hàng đó (ví dụ: `merchant_1_nhahanghaisanbi`)

---

## 📝 **LƯU Ý**

- Tất cả password mặc định: **`Merchant123`** hoặc **`Password123`**
- Nếu không đăng nhập được, có thể tài khoản chưa được tạo trong database
- Có thể chạy script seed để tạo tài khoản: `python backend/scripts/seed_merchants_simple.py`

