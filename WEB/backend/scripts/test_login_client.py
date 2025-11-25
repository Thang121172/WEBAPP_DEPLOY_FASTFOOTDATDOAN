import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
import django
django.setup()
from django.test import Client
import json

c = Client()
resp = c.post('/api/accounts/login/', data=json.dumps({'username':'testadmin','password':'Password123'}), content_type='application/json')
print('status_code=', resp.status_code)
print('content=', resp.content.decode('utf-8'))
if resp.status_code >= 500:
    # print exception info from Django's server logs isn't directly available; attempt to call view function to capture exception
    print('\n(500 from server; check runserver output for traceback)')
