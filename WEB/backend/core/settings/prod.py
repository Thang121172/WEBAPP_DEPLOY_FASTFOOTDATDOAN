# core/settings/prod.py

from .base import *  # Import tất cả từ base
import dj_database_url

# =========================================================
# DEBUG / SECRET_KEY
# =========================================================
DEBUG = False

SECRET_KEY = os.environ.get('SECRET_KEY') or os.environ.get('DJANGO_SECRET_KEY')
if not SECRET_KEY:
    raise ValueError("SECRET_KEY or DJANGO_SECRET_KEY environment variable must be set in production.")

# =========================================================
# ALLOWED_HOSTS
# =========================================================
# Hỗ trợ cả Render và VPS deployment
RENDER_EXTERNAL_HOSTNAME = os.environ.get('RENDER_EXTERNAL_HOSTNAME')
_allowed_hosts = os.environ.get('ALLOWED_HOSTS', '')

if _allowed_hosts:
    ALLOWED_HOSTS = [h.strip() for h in _allowed_hosts.split(',') if h.strip()]
elif RENDER_EXTERNAL_HOSTNAME:
    ALLOWED_HOSTS = [RENDER_EXTERNAL_HOSTNAME]
else:
    # Fallback - nên set ALLOWED_HOSTS trong env
    ALLOWED_HOSTS = ['127.0.0.1', 'localhost']

# =========================================================
# DATABASE
# =========================================================
# Hỗ trợ cả DATABASE_URL (Render) và individual vars (VPS/Docker)
DATABASE_URL = os.environ.get('DATABASE_URL')
if DATABASE_URL:
    # Render hoặc các platform dùng DATABASE_URL
    DATABASES = {
        'default': dj_database_url.config(
            default=DATABASE_URL,
            conn_max_age=600,
            conn_health_checks=True,
        )
    }
    # Yêu cầu SSL cho Render
    if DATABASES['default'].get('ENGINE') and 'render.com' in str(DATABASE_URL):
        DATABASES['default']['OPTIONS'] = {'sslmode': 'require'}
else:
    # VPS/Docker với biến môi trường riêng
    DATABASES = {
        'default': {
            'ENGINE': 'django.db.backends.postgresql',
            'NAME': os.environ.get('POSTGRES_DB', 'fastfood'),
            'USER': os.environ.get('POSTGRES_USER', 'app'),
            'PASSWORD': os.environ.get('POSTGRES_PASSWORD', ''),
            'HOST': os.environ.get('POSTGRES_HOST', 'db'),
            'PORT': os.environ.get('POSTGRES_PORT', '5432'),
            'CONN_MAX_AGE': 600,
        }
    }

# =========================================================
# CORS
# =========================================================
_cors_origins = os.environ.get('CORS_ORIGINS', '')
if _cors_origins:
    CORS_ALLOWED_ORIGINS = [
        origin.strip() for origin in _cors_origins.split(',') if origin.strip()
    ]
else:
    # Fallback - nên set CORS_ORIGINS trong env
    CORS_ALLOWED_ORIGINS = []

CORS_ALLOW_CREDENTIALS = True
CORS_ALLOW_ALL_ORIGINS = False  # Bảo mật: chỉ cho phép origins đã khai báo

# =========================================================
# STATIC FILES
# =========================================================
STATIC_ROOT = BASE_DIR / 'staticfiles'
STATICFILES_STORAGE = 'whitenoise.storage.CompressedManifestStaticFilesStorage'

# =========================================================
# MEDIA FILES
# =========================================================
MEDIA_ROOT = BASE_DIR / 'media'
MEDIA_URL = '/media/'

# =========================================================
# SECURITY SETTINGS
# =========================================================
# Chỉ bật SSL redirect nếu đang chạy qua HTTPS (không bật khi dùng Nginx reverse proxy)
USE_SSL_REDIRECT = os.environ.get('USE_SSL_REDIRECT', 'False').lower() == 'true'
if USE_SSL_REDIRECT:
    SECURE_SSL_REDIRECT = True

SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_BROWSER_XSS_FILTER = True
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = 'DENY'

# HSTS (HTTP Strict Transport Security)
SECURE_HSTS_SECONDS = 31536000  # 1 year
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True

# =========================================================
# REDIS / CELERY
# =========================================================
REDIS_HOST = os.environ.get('REDIS_HOST', 'redis')
REDIS_PASSWORD = os.environ.get('REDIS_PASSWORD', '')
REDIS_PORT = os.environ.get('REDIS_PORT', '6379')

if REDIS_PASSWORD:
    CELERY_BROKER_URL = f'redis://:{REDIS_PASSWORD}@{REDIS_HOST}:{REDIS_PORT}/1'
    CELERY_RESULT_BACKEND = f'redis://:{REDIS_PASSWORD}@{REDIS_HOST}:{REDIS_PORT}/2'
else:
    CELERY_BROKER_URL = f'redis://{REDIS_HOST}:{REDIS_PORT}/1'
    CELERY_RESULT_BACKEND = f'redis://{REDIS_HOST}:{REDIS_PORT}/2'

# =========================================================
# LOGGING
# =========================================================
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'verbose': {
            'format': '{levelname} {asctime} {module} {message}',
            'style': '{',
        },
    },
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
            'formatter': 'verbose',
        },
    },
    'root': {
        'handlers': ['console'],
        'level': 'INFO',
    },
    'loggers': {
        'django': {
            'handlers': ['console'],
            'level': 'INFO',
            'propagate': False,
        },
        'django.request': {
            'handlers': ['console'],
            'level': 'ERROR',
            'propagate': False,
        },
    },
}