# backend/core/settings/dev.py

from .base import *  # noqa
import os

# =========================================================
# DEV MODE
# =========================================================
DEBUG = True

# =========================================================
# ALLOWED_HOSTS
# =========================================================
# Giữ lại các host từ base + thêm các host hay gặp trong dev/docker
_extra_hosts = [
    "0.0.0.0",
    "localhost",
    "127.0.0.1",
    "backend",             # service name trong docker-compose (backend container)
    "fastfood_backend",    # container_name mình set trong docker-compose
    "testserver",          # Django test client
]

for h in _extra_hosts:
    if h not in ALLOWED_HOSTS:
        ALLOWED_HOSTS.append(h)

# =========================================================
# CORS
# =========================================================
# Nếu .env hoặc biến môi trường đã set CORS_ORIGINS thì base.py đã tạo CORS_ALLOWED_ORIGINS.
# Nếu chưa set, ta fallback cho dev frontend (5173/5174).
if not CORS_ALLOWED_ORIGINS:
    CORS_ALLOWED_ORIGINS = [
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5174",
    ]

# Trong dev, nếu bạn muốn mở full mọi origin cho tiện test API từ Postman/nghiệm thử tạm,
# thì bật dòng dưới. Nếu không muốn quá mở, để nguyên CORS_ALLOWED_ORIGINS ở trên.
# CORS_ALLOW_ALL_ORIGINS = True

CORS_ALLOW_CREDENTIALS = True

# =========================================================
# LOGGING DEV
# =========================================================
# Nâng level log lên DEBUG cho các app chính để dễ debug local
for logger_name in ["django", "accounts", "orders", "menus", "payments", "celery"]:
    if logger_name in LOGGING["loggers"]:
        LOGGING["loggers"][logger_name]["level"] = "DEBUG"

# =========================================================
# EMAIL (OTP)
# =========================================================
# Ở dev bạn đang dùng Gmail SMTP thật từ docker-compose (EMAIL_HOST=smtp.gmail.com ...)
# Nên mình giữ nguyên backend email từ base.py để bạn test OTP thật.
# Nếu muốn chặn gửi mail thật trong lúc dev, bạn có thể uncomment dòng sau:
# EMAIL_BACKEND = "django.core.mail.backends.console.EmailBackend"

# =========================================================
# JWT LIFETIME
# =========================================================
# Bạn có thể để nguyên SIMPLE_JWT từ base (30 phút access / 7 ngày refresh)
# hoặc nới lỏng cho dev. Ở đây mình giữ nguyên để tránh lệch hành vi production.
# SIMPLE_JWT["ACCESS_TOKEN_LIFETIME"] = timedelta(hours=12)
# SIMPLE_JWT["REFRESH_TOKEN_LIFETIME"] = timedelta(days=30)

# =========================================================
# DATABASE / CELERY / STATIC
# =========================================================
# Không override gì thêm:
# - DATABASES, CELERY_BROKER_URL, CELERY_RESULT_BACKEND... đã config trong base.py
#   và đọc từ biến môi trường docker-compose.
# - STATIC_ROOT, MEDIA_ROOT giữ nguyên.
