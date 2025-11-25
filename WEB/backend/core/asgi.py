# backend/core/asgi.py

import os
from django.core.asgi import get_asgi_application

# Chạy với settings dev (docker-compose đang set core.settings.dev)
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "core.settings.dev")

application = get_asgi_application()
