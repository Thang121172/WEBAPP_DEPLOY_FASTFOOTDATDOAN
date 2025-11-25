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
text = resp.content.decode('utf-8', errors='ignore')
print('STATUS:', resp.status_code)
# search for common markers
for marker in ['Traceback (most recent call last):', 'Exception Type', 'Exception Value', 'Traceback', 'TypeError', 'ValueError', 'AttributeError']:
    if marker in text:
        print('\n--- Marker:', marker, 'found ---\n')
        # print surrounding context
        idx = text.find(marker)
        start = max(0, idx-400)
        end = min(len(text), idx+800)
        print(text[start:end])

# if nothing found, print small prefix of the page to inspect
if all(m not in text for m in ['Traceback','Exception']):
    print('\n--- Page head (first 2000 chars) ---\n')
    print(text[:2000])
