import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','core.settings.dev')
import django
django.setup()
from django.contrib.auth import get_user_model
User = get_user_model()
if not User.objects.filter(username='testadmin').exists():
    User.objects.create_superuser('testadmin','testadmin@example.com','Password123')
    print('created testadmin')
else:
    print('testadmin exists')
