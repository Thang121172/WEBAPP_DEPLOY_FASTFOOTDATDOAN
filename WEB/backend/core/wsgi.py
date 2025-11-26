"""
WSGI config for the backend project.

It exposes the WSGI callable as a module-level variable named ``application``.
For more information on this file, see
https://docs.djangoproject.com/en/4.2/howto/deployment/wsgi/
"""

import os

from django.core.wsgi import get_wsgi_application

# Đảm bảo settings module đúng - sử dụng biến môi trường hoặc fallback
os.environ.setdefault('DJANGO_SETTINGS_MODULE', os.environ.get('DJANGO_SETTINGS_MODULE', 'core.settings.dev'))

application = get_wsgi_application()