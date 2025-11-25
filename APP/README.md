# FastFood App (local dev)

> NOTE: A curated list of code files and their parent folders (java, json, kts, yml, bat, env, env.example, .gitignore, toml, jar, html, .pro, xml) is available in `CODE_FILES_SUMMARY.md`.

This workspace contains:
- Android app in `app/`
- Node/Express backend in `backend/`
- PostgreSQL database via Docker Compose

## Quick start (Windows PowerShell)

1. **Configure Environment Variables**

   Copy `.env.example` to `.env` and fill in your settings:

   ```bash
   cp .env.example .env
   ```

   Key variables:
   - `JWT_SECRET`: Set a strong secret for JWT signing.
   - `ADMIN_SECRET`: Secret for creating initial admin user.
   - SMTP settings (see below for Gmail setup).
   - `DEBUG_SHOW_OTP`: Set to `true` for dev mode to expose OTP codes in DB/logs.
   - `ALLOW_SMOKE_SEED`: Set to `true` to enable dev seeding endpoints.

2. **Start services (Postgres + backend):**

   ```powershell
   docker compose up -d --build
   ```

   This will:
   - Start Postgres 15 on host port 5433 (container 5432)
   - Start Redis on host port 6379 (for rate limiting)
   - Build and start the backend on port 8081 (host) -> 8080 (container), run DB migrations automatically

3. **SMTP Configuration (for OTP emails)**

   To send OTP emails via Gmail:
   - Enable 2FA on your Gmail account.
   - Generate an App Password: Go to Google Account > Security > App passwords > Generate for "Mail".
   - Set in `.env`:
     ```
     SMTP_HOST=smtp.gmail.com
     SMTP_PORT=587
     SMTP_SECURE=false
     SMTP_USER=your-gmail@gmail.com
     SMTP_PASS=your-app-password
     SMTP_FROM=your-gmail@gmail.com
     ```
   - If SMTP is not configured, OTPs are logged to console and stored in DB (for dev/testing). The backend will log an explicit message when SMTP is not configured.

4. **Register and login (example curl)**

   Register a user (role USER, Gmail-only):

   ```powershell
   curl -X POST http://localhost:8081/auth/send-otp -H "Content-Type: application/json" -d '{"email":"test.user@gmail.com"}'
   ```

   Get OTP (dev mode, if DEBUG_SHOW_OTP=true):

   ```powershell
   curl http://localhost:8081/dev/last-otp?email=test.user@gmail.com
   ```

   Verify OTP:

   ```powershell
   curl -X POST http://localhost:8081/auth/verify-otp -H "Content-Type: application/json" -d '{"email":"test.user@gmail.com","code":"123456"}'
   ```

   Register an admin (via setup endpoint):

   ```powershell
   curl -X POST http://localhost:8081/setup/create-admin -H "Content-Type: application/json" -d '{"secret":"adminkey","username":"admin@gmail.com","password":"adminpass"}'
   ```

   Login (after verification):

   ```powershell
   curl -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" -d '{"username":"test.user@gmail.com","password":"pass123"}'
   ```

5. **Android app**
   - Open the project in Android Studio.
   - The app points to `http://10.0.2.2:8081/` by default (emulator -> host). For a real device, change `AuthClient`/`MenuApi` baseUrl to your machine IP.
   - Run on emulator. Use Register -> create a user (USER or ADMIN). Login.
   - Admin users will see admin UI (FAB + banner); regular users won't.

6. **Logout**
   - Use the top-right menu -> Logout. This clears the stored token/role and returns to login.

## Notes
- This is a local dev setup. Do not expose the Node server or DB without proper production hardening (HTTPS, rotated secrets, migrations, connection pooling, etc.).
- The backend uses bcrypt for password hashing and JWT for simple token issuance.
- OTP rate limiting: 5 sends per 3 hours per email, with Redis fallback to in-memory.
- Dev endpoints (`/dev/*`) are protected by `DEBUG_SHOW_OTP=true` or `ALLOW_SMOKE_SEED=true`.
- Admin endpoints require JWT with `role: ADMIN`.

## What I (the agent) implemented in this workspace

- Backend
   - Gmail-only OTP policy (send only to gmail.com/googlemail.com).
   - OTP TTL 10 minutes, hashed storage (bcrypt) and optional plain text storage when `DEBUG_SHOW_OTP=true` or `ALLOW_SMOKE_SEED=true` for dev testing.
   - Rate limiting for OTP sends using Redis (when configured) or in-memory fallback. Headers `X-OTP-Remaining`, `X-OTP-Limit`, and `X-OTP-Window-Seconds` are returned on `/auth/send-otp` responses to allow clients to show remaining quota and cooldown.
   - Retry logic for SMTP sends with console fallback when SMTP not configured.
   - `POST /auth/reset-password` endpoint implemented (verify OTP then set new password).
   - Dev helper endpoints: `/dev/last-otp` and `/admin/last-otp` (note: dev endpoint now requires a `DEV_TOKEN` header and access from localhost/private subnets — see `.env` flags below).
   - Refresh token rotation and logout revocation list with `revoked_tokens` table.
   - Docker Compose setup for local development: Postgres database on port 5433, Redis on port 6379, backend on port 8081.
   - Database migration script (`migrate.js`) to create tables on startup.
   - Environment configuration via `.env` file for SMTP, JWT secrets, debug flags.

- Android client/app
   - Register flow updated to auto-send OTP and show an OTP verification area (6 separate boxes) with auto-advance.
   - Forgot-password flow updated: send OTP -> show 6-box OTP input -> set new password -> call `/auth/reset-password`.
   - Auth client (`AuthClient`) extended with `resetPassword(...)` helper.
   - OTP UX improvements: auto-advance, backspace focus, resend cooldown UI (client-side), and register/forgot flows wired to backend.
   - Gradle build setup with clean and assemble tasks.
   - Network clients for authentication and menu API, configured to connect to local backend (10.0.2.2:8081 for emulator).

- Infrastructure
   - Docker Compose configuration for multi-service local dev environment.
   - Gradle wrapper for Android builds.
   - Environment flags for dev mode (DEBUG_SHOW_OTP, DEV_TOKEN, ALLOW_SMOKE_SEED).
   - Smoke tests for backend endpoints.
   - CI workflow for running tests and builds.

## New environment flags and notes
- `DEBUG_SHOW_OTP=true` — keeps plain OTP in DB and allows `/dev/last-otp` (for dev). Do NOT enable in production.
- `DEV_TOKEN` — when set, a request to `/dev/last-otp` must include header `X-DEV-TOKEN: <DEV_TOKEN>` unless request originates from localhost/private network. This hardens the dev endpoint.

## How to test OTP flows quickly
1. Start backend with `DEBUG_SHOW_OTP=true` and `DEV_TOKEN=dev123` in `.env` and `docker compose up -d --build`.
2. Use `curl` or Postman to call `/auth/send-otp` with your Gmail address.
3. Retrieve OTP using:

```powershell
curl -H "X-DEV-TOKEN: dev123" "http://localhost:8081/dev/last-otp?email=test.user@gmail.com"
```

4. Use the OTP in the Android app or via `/auth/verify-otp`.

If you prefer real email delivery, set SMTP env vars as described above.

## Troubleshooting

If you hit problems while testing OTPs, builds, or installs, try the checks below — they cover the common issues we've seen during local development.

- SMTP app-password whitespace (common)
   - When you copy a Gmail App Password from the Google UI it may include spaces. The backend now sanitizes `SMTP_PASS` by removing whitespace, but it's best to paste the 16-character app password without spaces into `.env`.
   - If you suspect SMTP auth failures, check the backend logs:

```powershell
docker compose logs backend --tail=200
```

   - For an SMTP connectivity/auth check (admin-only):

```powershell
curl -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" http://localhost:8081/admin/smtp-check
```

- OTP generation vs delivery
   - If `/auth/send-otp` returns OK but you don't see an email, first check the dev helper (only when `DEBUG_SHOW_OTP=true`):

```powershell
curl -H "X-DEV-TOKEN: <DEV_TOKEN>" "http://localhost:8081/dev/last-otp?email=your.email@gmail.com"
```

   - If the dev helper returns a code, the backend generated the OTP correctly; the issue is email delivery (SMTP, spam, or inbox rules).

- Android build / resource errors
   - Common compile-time failures we saw include missing string resources (AAPT errors from `nav_graph.xml`) and Java sources referring to a different package or missing `R` symbols.
   - Quick local repair checklist:

```powershell
# 1) Clean build artifacts
./gradlew clean

# 2) Assemble debug APK and inspect failures
./gradlew assembleDebug --stacktrace

# 3) If the build fails with missing strings referenced by navigation, add the missing entries to
#    `app/src/main/res/values/strings.xml` or update the nav graph to use existing labels.

# 4) If Java compilation errors show "package com.example.fooddeliveryapp does not exist" or
#    "cannot find symbol: R", inspect the source files under `app/src/main/java/` and either
#    - update the package declarations/imports to `com.example.app`, or
#    - remove/replace files that came from a different project and are not used.

# 5) Rebuild until clean. When successful an APK will be at:
app/build/outputs/apk/debug/app-debug.apk
```

- Install / run / logs (device or emulator)

```powershell
# Uninstall previous debug build
adb uninstall com.example.app

# Install new debug APK (replace path if different)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Start the app
adb shell am start -n com.example.app/.MainActivity

# Capture filtered logcat for registration/OTP tags
adb logcat -d | findstr /i "RegisterFragment\|OtpVerifyFragment\|Using backend"
```

- If you want a one-step helper, create a PowerShell script (example below) that sends an OTP and prints the dev code when `DEBUG_SHOW_OTP=true`:

```powershell
$email = 'your.email@gmail.com'
curl -X POST http://localhost:8081/auth/send-otp -H "Content-Type: application/json" -d "{\"email\":\"$email\"}"
Start-Sleep -Seconds 1
$res = curl -H "X-DEV-TOKEN: testtoken" "http://localhost:8081/dev/last-otp?email=$email"
Write-Output "dev last-otp: $res"
```

If you want, I can add this helper script to `backend/scripts/` for faster testing.

## Files of interest
- `backend/index.js` - Express server and endpoints
- `backend/migrate.js` - DB migration script (creates tables)
- `docker-compose.yml` - starts Postgres, Redis, and backend
- `app/src/main/java/com/example/app/network` - Android Retrofit clients
- `app/src/main/java/com/example/app/*Fragment.java` - UI logic

## Project files (Tổng hợp các file chính)

Below is a concise summary of the important files and folders in this repository, with a short purpose (English) and a Vietnamese note.

- `.env` - Local environment variables used by `docker-compose` and the backend (contains SMTP, DB credentials, flags like `DEBUG_SHOW_OTP`).
   - (Tiếng Việt) File cấu hình môi trường локal; KHÔNG commit giá trị nhạy cảm vào Git.
- `docker-compose.yml` - Orchestrates Postgres, Redis and the backend for local development (maps host ports and provides env overrides).
   - (Tiếng Việt) Dùng để chạy nhanh Postgres + Redis + backend trên máy dev.
- `backend/index.js` - Main Node/Express backend. Handles auth (OTP), users, orders, SMTP sending, Redis rate-limiting, and admin/dev helpers.
   - (Tiếng Việt) Backend chính: OTP, đăng nhập, quản lý đơn hàng, email OTP, endpoint dev/admin.
- `backend/migrate.js` - Database migration/initialization script run at container start to create tables.
   - (Tiếng Việt) Tạo schema và bảng khi chạy lần đầu.
- `backend/package.json` and `package-lock.json` - Backend dependencies and npm scripts (migrate, start, test).
   - (Tiếng Việt) Quản lý dependencies và script chạy backend.
- `backend/tests/` - Smoke/integration tests for backend endpoints (can be run locally via Node or the provided PowerShell runner).
   - (Tiếng Việt) Bộ test nhanh để kiểm tra OTP, login, refresh, v.v.
- `backend/scripts/` - Small helper scripts (e.g., cleanup revoked tokens runner, test runners).
   - (Tiếng Việt) Script hỗ trợ vận hành và CI.
- `backend/Dockerfile` - Docker build instructions for the backend image used by `docker-compose`.
   - (Tiếng Việt) Cấu hình build image backend.
- `app/` - Android application source (Gradle project). Key folders:
   - `app/src/main/java/com/example/app/network` - Retrofit clients wired to the backend.
   - `app/src/main/java/com/example/app/*Fragment.java` - UI fragments for Register, Login, Orders, Cart, etc.
   - (Tiếng Việt) Mã nguồn ứng dụng Android — mở bằng Android Studio.
- `gradlew`, `gradlew.bat`, `build.gradle.kts`, `settings.gradle.kts` - Gradle wrapper and build config for Android app and multi-module build.
   - (Tiếng Việt) Dùng để build app trên CI hoặc cục bộ.
- `backend_logs.txt`, `device_http_logs.txt`, `tmp_log_extract.txt` - Convenience logs / debug artifacts checked into the repo for local troubleshooting.
   - (Tiếng Việt) File log mẫu / trợ giúp gỡ lỗi.
- `README.md` - This file (project documentation and quickstart).
   - (Tiếng Việt) Tài liệu hướng dẫn cài đặt và test nhanh.
- `TODO.md` - Project TODOs and suggested enhancements.
   - (Tiếng Việt) Danh sách việc cần làm cho dự án.

If you'd like, I can also add a machine-readable file index (JSON) or expand this list with file sizes / last-modified timestamps. Tell me which format you prefer (plain list, table, or JSON). 

## Flow -> key files to inspect (Tổng hợp theo luồng)

Below are the main files to check for each user flow (helps pinpoint where an error likely originates). For each file I include the path and a short note what to look for.

- Registration (User sign-up + auto-send OTP)
   - `app/src/main/java/com/example/app/RegisterFragment.java` — UI + client call to `AuthClient.register(...)`. Check client-side validation, intents/navigation to OTP screen, and `Log.i("RegisterFragment", "Using backend: ...")` for baseUrl.
   - `app/src/main/java/com/example/app/network/AuthClient.java` — `register(...)` method calls backend `/auth/register`. Inspect baseUrl resolution and stored tokens.
   - `backend/index.js` — register handler around the `app.post('/auth/register'...)` block (auto-send OTP, DB insert). Check logs `register auto-send-otp stored for userId=` and any `mailer send error for register` messages.

- OTP send / resend (server-side)
   - `app/src/main/java/com/example/app/network/AuthClient.java` — `sendOtp(...)` used by Register/Forgot flows.
   - `backend/index.js` — `/auth/send-otp` endpoint: rate limiting, SMTP send logic, headers `X-OTP-Remaining`, `X-OTP-Limit`, `X-OTP-Window-Seconds`. Look for `mailer send error for send-otp` or `OTP (fallback)` logs.
   - Docker logs: `docker compose logs backend` to view SMTP errors or structured `otp_send` JSON logs.

- OTP verification (entering code)
   - `app/src/main/java/com/example/app/OtpVerifyFragment.java` — collects 6-digit code, shows resend cooldown, handles `verifyOtp(...)` and `resetPassword(...)` depending on mode. Check navigation args (email, mode) and Toast/error handling.
   - `app/src/main/java/com/example/app/OtpVerifyActivity.java` / `OtpSuccessFragment.java` — follow-up UI after verification.
   - `backend/index.js` — `/auth/verify-otp` endpoint: compares hash or stored plain `code`, marks `used`, issues tokens. Look for `verify-otp error` logs or DB rows in `otp_codes`.

- Forgot password / Reset flow
   - `app/src/main/java/com/example/app/ForgotFragment.java` — sends OTP and navigates to OTP verify in `reset` mode; reads rate-limit headers to show cooldown/quota.
   - `OtpVerifyFragment.java` (mode=`reset`) — calls `AuthClient.resetPassword(...)` which calls backend `/auth/reset-password` to verify OTP and update password.
   - `backend/index.js` — `/auth/reset-password` endpoint: validates OTP, updates `users.password`, sets `verified=true`. Check `reset-password error` logs and DB updates.

- Login flow
   - `app/src/main/java/com/example/app/LoginFragment.java` — calls `AuthClient.login(...)`. If server returns `account_not_verified`, it triggers `sendOtp` and navigates to Register/OTP flows. Check error-body parsing and auto-send behavior.
   - `backend/index.js` — `/auth/login` endpoint: checks `verified` flag and returns `account_not_verified` when appropriate.

- Backend infra & DB
   - `backend/migrate.js` — DB schema: `users`, `otp_codes`, `refresh_tokens`, `revoked_tokens`, `orders`, etc. If migrations fail or columns are missing, flows can break (look for migration errors on startup).
   - `docker-compose.yml` — container ports, env vars mapping (SMTP, DB, REDIS). Ensure `.env` values are passed and sanitized (note: app strips whitespace from `SMTP_PASS`).

- Useful debug endpoints & files
   - `GET /dev/last-otp` — returns last OTP for an email when `DEBUG_SHOW_OTP=true` and with `X-DEV-TOKEN` protected (or localhost). Use this to verify OTP generation during dev.
   - `GET /admin/smtp-check` — admin-only SMTP health check (requires ADMIN token). Useful to confirm SMTP connectivity/auth.
   - Log files: `backend_logs.txt` and `docker compose logs backend`.

Tips to localize a problem quickly
- If OTP isn't arriving but `/dev/last-otp` returns a code: the backend is generating OTP correctly; check SMTP logs / app inbox filters / spam.
- If `/dev/last-otp` returns `plain code not stored` or 404: backend stored only hash (production), check logs for the OTP or enable `DEBUG_SHOW_OTP=true` for dev.
- If register/login fails with database errors (unique constraint, missing column): inspect `docker compose logs backend` for `Migration error` or `duplicate key` messages and review `backend/migrate.js`.
- On Android, watch `adb logcat` for tags: `RegisterFragment`, `OtpVerifyFragment`, and `Using backend` messages logged by fragments.

If you want, I'll update `README.md` to include direct command snippets for quickly exercising each flow and exact files to open for debugging (e.g., search patterns and log keys). Should I add those command snippets? 

## Quick command snippets to exercise each flow (PowerShell)

Copy-paste these into PowerShell on your dev machine (adjust email, tokens, and host/ports as needed). These are meant to speed up reproducing OTP, verify and reset flows.

1) Start services (if not running):

```powershell
docker compose up -d --build
```

2) Send OTP (register / explicit send):

```powershell
curl -X POST http://localhost:8081/auth/send-otp -H "Content-Type: application/json" -d '{"email":"your.email@gmail.com"}'
```

3) Retrieve last OTP (dev helper) — requires `DEBUG_SHOW_OTP=true` and `X-DEV-TOKEN` when calling remotely:

```powershell
curl -H "X-DEV-TOKEN: testtoken" "http://localhost:8081/dev/last-otp?email=your.email@gmail.com"
```

4) Verify OTP (complete registration / login flow):

```powershell
curl -X POST http://localhost:8081/auth/verify-otp -H "Content-Type: application/json" -d '{"email":"your.email@gmail.com","otp":"123456"}'
```

5) Resend OTP (from client / resend button) — same as send-otp endpoint:

```powershell
curl -X POST http://localhost:8081/auth/send-otp -H "Content-Type: application/json" -d '{"email":"your.email@gmail.com"}'
```

6) Reset password (forgot flow — server verifies OTP and updates password):

```powershell
curl -X POST http://localhost:8081/auth/reset-password -H "Content-Type: application/json" -d '{"email":"your.email@gmail.com","otp":"123456","new_password":"NewPass123"}'
```

7) Login (after verification):

```powershell
curl -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" -d '{"username":"your.email@gmail.com","password":"YourPassword"}'
```

8) Admin SMTP health check (requires ADMIN token in Authorization header):

```powershell
curl -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" http://localhost:8081/admin/smtp-check
```

9) Tail backend logs (useful to see mailer errors / structured otp_send JSON):

```powershell
docker compose logs backend --tail=200
```

10) Android logs (search for relevant tags while reproducing the flow):

```powershell
# Capture and filter logcat for relevant tags (RegisterFragment, OtpVerifyFragment, "Using backend")
adb logcat -d | findstr /i "RegisterFragment\|OtpVerifyFragment\|Using backend"
```

11) Small PowerShell helper to send OTP and print the dev code (if DEBUG_SHOW_OTP is enabled):

```powershell
$email = 'your.email@gmail.com'
curl -X POST http://localhost:8081/auth/send-otp -H "Content-Type: application/json" -d "{\"email\":\"$email\"}"
Start-Sleep -Seconds 1
$res = curl -H "X-DEV-TOKEN: testtoken" "http://localhost:8081/dev/last-otp?email=$email"
Write-Output "dev last-otp: $res"
```

Notes
- Replace `your.email@gmail.com` and `<ADMIN_ACCESS_TOKEN>` with real values when testing.
- If `/dev/last-otp` returns `plain code not stored` it means the server saved only a hashed OTP (production-like). Re-run with `DEBUG_SHOW_OTP=true` to store plain OTPs for dev, or check backend logs for the OTP fallback message.
- If SMTP fails to send emails, check `docker compose logs backend` for `mailer send error` messages. We added sanitization for `SMTP_PASS` (removes whitespace) to handle Gmail app-password copy/paste.

If you want, I can also add a one-click PowerShell script in `backend/scripts/` that runs the helper above with args. Want me to add that? 

## Testing
- Smoke tests: Run `backend/tests/smoke.js` via Node.
- PowerShell runner: `backend/scripts/run_tests.ps1`.
- Tests include OTP send/verify, login/logout, refresh tokens.

If you want, I can:
- Add persistent menu items in DB and REST endpoints to CRUD menus.
- Add refresh/token expiration handling in the Android app.
- Add automated tests for backend endpoints.

## Design & UX (Specification)

Mục tiêu của FastFood là mang đến trải nghiệm đặt món nhanh chóng – giao hàng minh bạch – quản lý thuận tiện, đồng thời đảm bảo hiệu quả vận hành và bảo mật thông tin người dùng. Giao diện được phát triển theo hướng mobile-first, sử dụng tông màu xanh lá (#27AE60) biểu trưng cho sự tươi mới, an toàn và thân thiện, kết hợp với nền sáng (#F9FBF9) nhằm tạo cảm giác nhẹ nhàng và hiện đại.

👤 A. Giao diện và trải nghiệm của Khách hàng (Customer)
Mục đích

Giao diện khách hàng được xây dựng để giúp người dùng đặt món ăn và theo dõi đơn hàng một cách trực quan, nhanh chóng và bảo mật. Mục tiêu cốt lõi là giảm tối đa số bước thao tác, tăng tốc độ ra quyết định, đồng thời mang lại cảm giác tin cậy trong suốt hành trình sử dụng.

Bố cục và thành phần chính

• Đăng ký và xác minh tài khoản: bao gồm các màn hình nhập thông tin cá nhân, gửi mã OTP qua Gmail và xác minh sáu chữ số để đảm bảo tính bảo mật. Sau khi xác minh thành công, người dùng được chuyển đến trang đăng nhập.
• Trang chủ: có thanh tìm kiếm ở đầu trang, cho phép nhập từ khóa để tìm món hoặc quán. Bên dưới là danh sách quán (Quán A, Quán B, Quán C), mỗi quán được thể hiện bằng thẻ trắng có hình ảnh minh họa.
• Danh mục món ăn: hiển thị danh sách các món trong từng quán với tên món, giá tiền và hình đại diện, sắp xếp theo chiều dọc.
• Chi tiết món: mô tả thông tin chi tiết của món, ảnh minh họa lớn, phần ghi chú, và nút “Thêm vào giỏ” màu xanh lá nổi bật.
• Giỏ hàng: tổng hợp các món đã chọn với số lượng, giá và tổng tiền. Phía dưới là nút “Đặt hàng” để tiến hành thanh toán.
• Thanh toán: người dùng có thể lựa chọn giữa các phương thức như COD, QR, hoặc thẻ ngân hàng. Mỗi phương thức có màn hình riêng để nhập thông tin.
• Lịch sử đơn hàng: hiển thị mã đơn (ví dụ: #FF2025) và tổng tiền tương ứng, giúp người dùng xem lại các đơn đã hoàn tất.
• Theo dõi đơn: tích hợp bản đồ định vị thời gian thực, thể hiện vị trí của shipper, cùng nút “Gọi shipper”.
• Hồ sơ cá nhân: chứa thông tin như địa chỉ, phương thức thanh toán, và nút đăng xuất.

Trải nghiệm người dùng (UX) và giao diện (UI)

• Màu chủ đạo là xanh lá kết hợp nền trắng, tạo cảm giác dễ chịu, an toàn và thân thiện.
• Bố cục được chia thành một cột dọc, tối ưu cho thao tác chạm trên điện thoại.
• Các nút hành động chính (CTA) luôn được đặt ở trung tâm hoặc cuối màn hình, có kích thước lớn, bo góc mềm mại (10–12px).
• Font chữ Poppins dùng cho tiêu đề, Inter dùng cho phần nội dung, và Montserrat cho nút hành động, tạo sự rõ ràng và hiện đại.
• Mỗi bước trong hành trình chỉ có một tác vụ duy nhất (ví dụ: gửi OTP, xác minh, thanh toán), giúp giảm lỗi và rút ngắn thời gian sử dụng.

Luồng hoạt động của khách hàng (Customer Flow)

Người dùng mới bắt đầu bằng việc đăng ký tài khoản → nhận mã OTP qua gmail → xác minh OTP → đăng nhập. Sau đó, họ có thể tìm quán → chọn món → thêm vào giỏ hàng → tiến hành thanh toán. Khi thanh toán thành công, ứng dụng hiển thị màn hình xác nhận và chuyển đến phần theo dõi đơn hàng trên bản đồ. Sau khi đơn được giao, khách hàng có thể truy cập lịch sử đơn hàng để xem lại hoặc đánh giá dịch vụ. Toàn bộ quy trình được thiết kế trơn tru, tạo cảm giác liền mạch và đáng tin cậy.

🏪 B. Giao diện và trải nghiệm của Chủ quán (Merchant)
Mục đích

Giao diện dành cho Merchant được tạo ra để giúp chủ quán kiểm soát toàn bộ hoạt động kinh doanh, bao gồm quản lý menu, xác nhận đơn hàng, theo dõi doanh thu và tạo các chương trình khuyến mãi.

Bố cục và thành phần chính

• Dashboard tổng quan: hiển thị số lượng đơn hàng, doanh thu và các chỉ số quan trọng trong ngày dưới dạng thẻ thống kê.
• Quản lý menu: danh sách món ăn của quán, mỗi món có nút chỉnh sửa hoặc xóa, và một nút “Thêm món mới”.
• Khuyến mãi: danh sách các chương trình ưu đãi hiện có, mỗi chương trình là một thẻ riêng biệt. Phía dưới có nút “Tạo khuyến mãi mới”.
• Đơn hàng: hiển thị các đơn chờ xác nhận, đang chuẩn bị và đã hoàn thành. Khi chọn một đơn cụ thể, màn hình hiển thị mã đơn, danh sách món, giá trị đơn, cùng hai nút “Xác nhận” và “Từ chối”.

UX/UI

• Giao diện dùng nền sáng, chữ đen và nút hành động màu xanh để làm nổi bật thao tác chính.
• Khoảng cách giữa các thành phần được thiết kế hợp lý để tránh nhầm lẫn khi thao tác nhanh.
• Hệ thống bố cục theo dạng “card layout”, dễ mở rộng và thích hợp cho quản trị bằng điện thoại.
• Các nút có nhãn rõ ràng, chỉ dẫn bằng tiếng Việt thân thiện, phù hợp người dùng đại chúng.

Merchant Flow

Chủ quán đăng nhập → truy cập Dashboard → xem thống kê hoạt động → quản lý menu và khuyến mãi → xác nhận đơn hàng mới → theo dõi tiến độ giao → cập nhật doanh thu.
Luồng hoạt động được sắp xếp mạch lạc, giúp người quản lý vận hành trơn tru mà không cần kỹ năng công nghệ cao.

🚴‍♂️ C. Giao diện và trải nghiệm của Shipper (Người giao hàng)
Mục đích

Mục tiêu chính của giao diện Shipper là đơn giản hóa việc nhận đơn và giao hàng, đảm bảo mọi thao tác có thể thực hiện nhanh, dễ nhìn, và an toàn khi di chuyển.

Bố cục và thành phần chính

• Danh sách đơn hàng: chia làm hai phần: “Đơn khả dụng” (mới) và “Đơn đang giao” (đang xử lý).
• Chi tiết đơn: hiển thị mã đơn (#FF2025), địa chỉ khách hàng, danh sách món, và nút “Liên hệ khách hàng” hoặc “Đánh dấu đã giao”.
• Bản đồ điều hướng: khung bản đồ lớn ở trung tâm hiển thị tuyến đường từ quán đến khách hàng; bên dưới là nút “Bắt đầu giao hàng”.
• Thu nhập: danh sách các đơn đã giao và tổng tiền công từng ngày, giúp shipper theo dõi hiệu suất.

UX/UI

• Giao diện có độ tương phản cao để dễ quan sát khi ở ngoài trời.
• Các nút lớn, màu xanh nổi bật, có thể thao tác bằng một tay.
• Phần bản đồ chiếm phần lớn không gian nhằm tối ưu trải nghiệm điều hướng.
• Thông tin quan trọng (địa chỉ, mã đơn) được đặt ở vị trí trung tâm tầm nhìn.

Shipper Flow

Shipper đăng nhập → xem danh sách đơn khả dụng → chọn đơn → xem chi tiết và điều hướng → giao hàng → xác nhận hoàn tất → xem thu nhập tổng kết.
Luồng này đảm bảo tính tuyến tính, không cần quay lại nhiều màn hình, giảm thời gian thao tác thực địa.

🖥 D. Giao diện và trải nghiệm của Quản trị viên (Admin)
Mục đích

Giao diện Admin được thiết kế để giám sát và điều phối toàn bộ hệ thống, bao gồm theo dõi doanh thu, thống kê hoạt động, và quản lý người dùng.

Bố cục và thành phần chính

• Dashboard tổng quan: trình bày các chỉ số hệ thống (tổng người dùng, doanh thu, số lượng đơn hàng) bằng thẻ thông tin.
• Quản lý người dùng: danh sách tài khoản của Customer, Merchant và Shipper. Mỗi mục có nút khóa/mở quyền truy cập.
• Quản lý báo cáo: theo dõi hiệu suất từng quán, giải quyết khiếu nại và điều chỉnh hệ thống khi cần.

UX/UI

• Giao diện trực quan, sử dụng bảng dữ liệu kết hợp card để hiển thị thông tin lớn.
• Màu chủ đạo giữ nguyên nhận diện thương hiệu xanh – trắng.
• Các thành phần hành động (chỉnh sửa, khóa, xem chi tiết) luôn đặt ở bên phải để dễ truy cập.
• Tốc độ phản hồi nhanh, ưu tiên thao tác quản trị hệ thống.

Admin Flow

Admin đăng nhập → xem Dashboard tổng quan → truy cập mục người dùng hoặc báo cáo → can thiệp khi có sự cố → theo dõi cập nhật từ Merchant và Shipper theo thời gian thực.

🎨 E. Nguyên lý thiết kế UX/UI tổng thể

• Toàn bộ giao diện tuân thủ nguyên tắc “1 màn hình – 1 hành động chính”, giúp giảm tải nhận thức.
• Hệ thống dùng màu xanh lá (#27AE60) làm điểm nhấn cho mọi nút hành động quan trọng, biểu trưng cho tươi mát và an toàn.
• Nền trắng và khoảng trắng rộng tạo cảm giác thoáng, giảm căng thẳng khi sử dụng trong thời gian dài.
• Font chữ hiện đại (Poppins, Inter, Montserrat) tạo sự thống nhất và chuyên nghiệp.
• Các nút và thẻ (card) có góc bo mềm, bóng đổ nhẹ, giúp giao diện có chiều sâu tự nhiên.
• Thanh điều hướng cố định ở đáy màn hình gồm bốn biểu tượng: Home – Search – Orders – Profile, giúp người dùng dễ dàng di chuyển giữa các phần.

🔄 F. User Flow Narrative (Tổng hợp cho tất cả vai trò)

Hành trình người dùng trong hệ thống FastFood bắt đầu từ khách hàng. Họ đăng ký tài khoản, nhận mã OTP qua Gmail và xác minh thành công để đăng nhập vào hệ thống. Từ đó, họ có thể tìm kiếm quán ăn, chọn món yêu thích, thêm vào giỏ hàng và thanh toán bằng nhiều phương thức khác nhau. Sau khi thanh toán thành công, người dùng theo dõi vị trí shipper trên bản đồ thời gian thực cho đến khi nhận món.

Ở phía Merchant, hệ thống gửi thông báo khi có đơn mới. Chủ quán xem chi tiết, xác nhận đơn và bắt đầu chuẩn bị món ăn. Khi món đã sẵn sàng, đơn hàng được chuyển tiếp cho Shipper, người sẽ nhận lộ trình trên bản đồ, giao hàng đến đúng địa chỉ khách hàng và cập nhật trạng thái “Đã giao thành công”.

Cuối cùng, Admin đóng vai trò giám sát tổng thể — họ theo dõi hoạt động của tất cả người dùng, đảm bảo hệ thống hoạt động ổn định, bảo mật và không xảy ra xung đột giữa các vai trò.

Toàn bộ chuỗi hoạt động này được kết nối xuyên suốt qua một hệ thống giao diện đồng nhất, đảm bảo trải nghiệm nhanh, rõ, tin cậy và dễ sử dụng cho mọi đối tượng.

## Development & environment

Use these environment flags in `.env` during local development — never enable these in production:

- `DEBUG_SHOW_OTP=true` — stores plain OTP in DB and allows `/dev/last-otp` and `/admin/last-otp` to return codes. Only for dev and CI smoke tests.
- `DEV_TOKEN=some-secret` — when set, requests to `/dev/last-otp` must include header `X-DEV-TOKEN: <DEV_TOKEN>` unless originating from localhost or private subnet. This restricts remote access to the dev helper.
- `ALLOW_SMOKE_SEED=true` — enables `/dev/seed` and `/dev/create-order` endpoints used by smoke tests.

Security notes:
- Keep `DEBUG_SHOW_OTP` off in staging/production. If you must enable remote dev helpers in a shared environment, use a short-lived `DEV_TOKEN` and IP allow-listing at your infrastructure level (NGINX, firewall).
- Use a real SMTP account with app-specific password for email delivery in non-dev environments.

CI notes:
- A simple GitHub Actions workflow is included in `.github/workflows/ci.yml` that runs backend smoke tests and the Android Gradle assemble + unit tests. The workflow expects secrets for `DEV_TOKEN` and any SMTP credentials if you want email delivery during CI.

If you want, I can extend the CI to run instrumentation tests on an emulator matrix and publish artifacts.

