import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
import django
django.setup()
import json
from django.test import Client

c = Client()
# login
resp = c.post('/api/accounts/login/', data=json.dumps({'username':'testadmin','password':'Password123'}), content_type='application/json')
print('login status', resp.status_code)
print(resp.content.decode('utf-8'))
if resp.status_code == 200:
    j = resp.json()
    access = j.get('access') or j.get('token') or j.get('access_token')
    if access:
        resp2 = c.get('/api/accounts/me/', HTTP_AUTHORIZATION=f'Bearer {access}')
        print('me status', resp2.status_code)
        print(resp2.content.decode('utf-8'))
    else:
        print('no access token in login response')
else:
    print('login failed; see above')
