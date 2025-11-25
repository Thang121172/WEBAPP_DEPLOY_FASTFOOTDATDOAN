import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','core.settings.dev')
import django
django.setup()
from django.apps import apps
from django.conf import settings
print('AUTH_USER_MODEL=', settings.AUTH_USER_MODEL)
app = apps.get_app_config('accounts')
print('Models in accounts:', [m.__name__ for m in app.get_models()])
