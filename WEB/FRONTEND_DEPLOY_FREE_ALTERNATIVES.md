# 🆓 Các Platform Miễn Phí để Deploy Frontend

## 🥇 Top Recommendations

### 1. **Vercel** ⭐⭐⭐⭐⭐ (Khuyên dùng nhất!)

**Ưu điểm:**
- ✅ Free tier rộng rãi (100GB bandwidth/tháng)
- ✅ Deploy cực nhanh, tự động từ GitHub
- ✅ Hỗ trợ React/Vite tốt
- ✅ CDN global, performance cao
- ✅ Hỗ trợ environment variables
- ✅ Custom domain miễn phí

**Hạn chế:**
- ⚠️ Có thể sleep sau 30 phút không traffic (nhưng wake up nhanh)

**Link:** https://vercel.com/

**Cách deploy:**
1. Đăng ký bằng GitHub
2. Click "New Project"
3. Import repo GitHub của bạn
4. Set environment variables
5. Deploy tự động!

---

### 2. **Cloudflare Pages** ⭐⭐⭐⭐⭐

**Ưu điểm:**
- ✅ Free tier KHÔNG giới hạn bandwidth
- ✅ Không bao giờ sleep (unlimited builds)
- ✅ CDN cực nhanh (Cloudflare network)
- ✅ Deploy tự động từ GitHub/GitLab
- ✅ Custom domain miễn phí

**Hạn chế:**
- ⚠️ Build time giới hạn (mỗi build tối đa 20 phút)

**Link:** https://pages.cloudflare.com/

**Cách deploy:**
1. Đăng ký Cloudflare account
2. Vào Pages → Create a project
3. Connect GitHub repo
4. Set build settings:
   - Build command: `cd frontend && npm install && npm run build`
   - Build output directory: `frontend/dist`
5. Deploy!

---

### 3. **GitHub Pages** ⭐⭐⭐⭐

**Ưu điểm:**
- ✅ Hoàn toàn miễn phí (nếu repo public)
- ✅ Tích hợp sẵn với GitHub
- ✅ Không giới hạn bandwidth
- ✅ Custom domain miễn phí

**Hạn chế:**
- ⚠️ Chỉ hỗ trợ static sites
- ⚠️ Phải dùng GitHub Actions để build
- ⚠️ URL mặc định là `username.github.io/repo-name`

**Link:** Đã có sẵn trong GitHub repo của bạn!

**Cách deploy:**
1. Tạo file `.github/workflows/deploy.yml`
2. Push code lên GitHub
3. Enable GitHub Pages trong repo settings
4. Done!

---

### 4. **Netlify Drop** ⭐⭐⭐⭐

**Ưu điểm:**
- ✅ Deploy cực nhanh (drag & drop folder `dist`)
- ✅ Không cần GitHub
- ✅ Free tier 100GB bandwidth/tháng

**Hạn chế:**
- ⚠️ Phải build local trước
- ⚠️ Không auto-deploy từ Git

**Link:** https://app.netlify.com/drop

**Cách deploy:**
1. Build local: `cd frontend && npm run build`
2. Vào https://app.netlify.com/drop
3. Drag folder `frontend/dist` vào
4. Done!

---

### 5. **Surge.sh** ⭐⭐⭐

**Ưu điểm:**
- ✅ Đơn giản, nhanh
- ✅ CLI tool dễ dùng
- ✅ Free tier không giới hạn projects

**Hạn chế:**
- ⚠️ Phải build local trước
- ⚠️ Không auto-deploy
- ⚠️ URL: `project-name.surge.sh`

**Link:** https://surge.sh/

**Cách deploy:**
```bash
npm install -g surge
cd frontend
npm run build
cd dist
surge
# Nhập email, password, chọn domain
```

---

### 6. **Firebase Hosting** ⭐⭐⭐⭐

**Ưu điểm:**
- ✅ Free tier: 10GB storage, 360MB/day transfer
- ✅ CDN global
- ✅ Custom domain miễn phí
- ✅ Tích hợp với Firebase services

**Hạn chế:**
- ⚠️ Phải cài Firebase CLI
- ⚠️ Bandwidth hạn chế hơn

**Link:** https://firebase.google.com/products/hosting

**Cách deploy:**
```bash
npm install -g firebase-tools
firebase login
firebase init hosting
cd frontend && npm run build
firebase deploy
```

---

## 🎯 So sánh nhanh

| Platform | Bandwidth | Auto-deploy | Sleep? | Dễ dùng |
|----------|-----------|-------------|--------|---------|
| **Vercel** | 100GB/tháng | ✅ | ⚠️ Có | ⭐⭐⭐⭐⭐ |
| **Cloudflare Pages** | Unlimited | ✅ | ❌ Không | ⭐⭐⭐⭐⭐ |
| **GitHub Pages** | Unlimited | ✅ (Actions) | ❌ Không | ⭐⭐⭐⭐ |
| **Netlify** | 100GB/tháng | ✅ | ⚠️ Có | ⭐⭐⭐⭐ |
| **Surge** | Unlimited | ❌ | ❌ Không | ⭐⭐⭐ |
| **Firebase** | 360MB/ngày | ⚠️ (CI/CD) | ❌ Không | ⭐⭐⭐ |

---

## 💡 Khuyến nghị cho bạn

### Option 1: **Vercel** (Tốt nhất cho React/Vite)
- Dễ deploy nhất
- Performance tốt
- Tích hợp GitHub tự động
- **Link:** https://vercel.com/

### Option 2: **Cloudflare Pages** (Nếu muốn unlimited)
- Không giới hạn bandwidth
- Không bao giờ sleep
- **Link:** https://pages.cloudflare.com/

---

## 📝 Hướng dẫn deploy nhanh lên Vercel

### Bước 1: Đăng ký
1. Vào https://vercel.com/
2. Click **"Sign Up"**
3. Chọn **"Continue with GitHub"**
4. Authorize Vercel

### Bước 2: Deploy Project
1. Click **"Add New..."** → **"Project"**
2. Chọn repo GitHub của bạn (`TEST_WEB_DEPLOY`)
3. Vercel tự động detect:
   - Framework: Vite
   - Build Command: `cd frontend && npm install && npm run build`
   - Output Directory: `frontend/dist`

### Bước 3: Set Environment Variables
1. Trong quá trình deploy, click **"Environment Variables"**
2. Thêm:
   - Key: `VITE_API_BASE`
   - Value: `https://fastfood-backend-t8jz.onrender.com/api`
3. Click **"Save"**

### Bước 4: Deploy
1. Click **"Deploy"**
2. Đợi 1-2 phút
3. Done! URL sẽ là: `https://your-project.vercel.app`

### Bước 5: Set Custom Domain (Optional)
1. Vào Project Settings → Domains
2. Add domain: `fastfooddatdoan.netlify.app` (nếu muốn)
3. Hoặc thêm domain mới

---

## 📝 Hướng dẫn deploy lên Cloudflare Pages

### Bước 1: Đăng ký
1. Vào https://pages.cloudflare.com/
2. Click **"Sign up"**
3. Đăng ký bằng email hoặc GitHub

### Bước 2: Deploy Project
1. Click **"Create a project"**
2. Click **"Connect to Git"**
3. Chọn GitHub và authorize
4. Chọn repo `TEST_WEB_DEPLOY`
5. Set build settings:
   - **Framework preset:** Vite
   - **Build command:** `cd frontend && npm install && npm run build`
   - **Build output directory:** `frontend/dist`
6. Click **"Save and Deploy"**

### Bước 3: Set Environment Variables
1. Sau khi deploy, vào **Settings** → **Environment variables**
2. Add:
   - Key: `VITE_API_BASE`
   - Value: `https://fastfood-backend-t8jz.onrender.com/api`
3. **Redeploy** để env vars có hiệu lực

---

## 🎯 Kết luận

**Nếu muốn nhanh và dễ:** → **Vercel**
**Nếu muốn unlimited bandwidth:** → **Cloudflare Pages**
**Nếu muốn đơn giản nhất:** → **GitHub Pages**

Tất cả đều FREE và tốt hơn Netlify khi bị pause!

