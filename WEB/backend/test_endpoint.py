#!/usr/bin/env python3
"""
Test endpoint register/request-otp/
"""
import os
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')

import django
django.setup()

from django.test import Client
from django.urls import reverse, resolve
from django.urls.exceptions import NoReverseMatch

print('=' * 60)
print('TEST ENDPOINT: /api/accounts/register/request-otp/')
print('=' * 60)
print()

# Test 1: Kiem tra URL routing
print('Test 1: Kiem tra URL routing')
try:
    url = reverse('register_request_otp')
    print(f'  [OK] URL name "register_request_otp" -> {url}')
except NoReverseMatch as e:
    print(f'  [LOI] Khong tim thay URL name: {e}')
    # Thu tim bang path
    try:
        from django.urls import resolve
        match = resolve('/api/accounts/register/request-otp/')
        print(f'  [OK] Resolve path -> {match.url_name}')
    except Exception as e2:
        print(f'  [LOI] Khong resolve duoc path: {e2}')
print()

# Test 2: Test endpoint voi test client
print('Test 2: Test endpoint voi Django test client')
client = Client()
test_data = {
    'email': 'test@example.com',
    'password': 'Test123456',
    'role': 'customer'
}

# Test voi trailing slash
print('  Testing: POST /api/accounts/register/request-otp/')
response1 = client.post('/api/accounts/register/request-otp/', data=test_data, content_type='application/json')
print(f'  Status Code: {response1.status_code}')
if response1.status_code == 200:
    print(f'  [OK] Endpoint hoat dong!')
    print(f'  Response: {response1.json()}')
elif response1.status_code == 404:
    print(f'  [LOI] 404 - Endpoint khong ton tai!')
    print(f'  Response: {response1.content.decode()}')
else:
    print(f'  [WARNING] Status code: {response1.status_code}')
    print(f'  Response: {response1.content.decode()[:200]}')
print()

# Test khong co trailing slash
print('  Testing: POST /api/accounts/register/request-otp (khong co trailing slash)')
response2 = client.post('/api/accounts/register/request-otp', data=test_data, content_type='application/json')
print(f'  Status Code: {response2.status_code}')
if response2.status_code == 200:
    print(f'  [OK] Endpoint hoat dong!')
elif response2.status_code == 404:
    print(f'  [LOI] 404 - Endpoint khong ton tai!')
else:
    print(f'  [WARNING] Status code: {response2.status_code}')
print()

# Test 3: List tat ca URL patterns
print('Test 3: List URL patterns trong accounts.urls')
from accounts import urls as accounts_urls
print('  URL patterns:')
for pattern in accounts_urls.urlpatterns:
    print(f'    - {pattern.pattern} -> {pattern.name}')
print()

print('=' * 60)
print('HOAN TAT!')
print('=' * 60)

