#!/usr/bin/env python3
import os
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')

import django
django.setup()

from django.contrib.auth import authenticate, get_user_model
from accounts.serializers import LoginSerializer

User = get_user_model()

# Test data
email = 'tesstaccc@gmail.com'
password = 'Thang2004'

print('=' * 60)
print('TEST LOGIN LOGIC')
print('=' * 60)
print()

# 1. Tim user
user = User.objects.filter(email__iexact=email).first()
if not user:
    print(f'[LOI] Khong tim thay user voi email: {email}')
    sys.exit(1)

print(f'[OK] Tim thay user:')
print(f'  - ID: {user.id}')
print(f'  - Username: {user.username}')
print(f'  - Email: {user.email}')
print(f'  - is_active: {user.is_active}')
print()

# 2. Test authenticate truc tiep
print('Test 1: Authenticate voi username thuc:')
auth1 = authenticate(username=user.username, password=password)
print(f'  Result: {auth1}')
if auth1:
    print(f'  [OK] Authenticate thanh cong!')
else:
    print(f'  [LOI] Authenticate that bai!')
print()

print('Test 2: Authenticate voi email:')
auth2 = authenticate(username=email, password=password)
print(f'  Result: {auth2}')
if auth2:
    print(f'  [OK] Authenticate thanh cong!')
else:
    print(f'  [LOI] Authenticate that bai!')
print()

# 3. Test LoginSerializer (nhu frontend gui)
print('Test 3: LoginSerializer voi email va username (nhu frontend):')
login_data = {
    'email': email,
    'username': email,  # Frontend gửi cả hai
    'password': password
}
serializer = LoginSerializer(data=login_data)
if serializer.is_valid():
    print(f'  [OK] Serializer valid!')
    result = serializer.save()
    print(f'  Result: {result}')
else:
    print(f'  [LOI] Serializer invalid!')
    print(f'  Errors: {serializer.errors}')
print()

# 4. Test LoginSerializer chi voi email
print('Test 4: LoginSerializer chi voi email:')
login_data2 = {
    'email': email,
    'password': password
}
serializer2 = LoginSerializer(data=login_data2)
if serializer2.is_valid():
    print(f'  [OK] Serializer valid!')
    result2 = serializer2.save()
    print(f'  Result: {result2}')
else:
    print(f'  [LOI] Serializer invalid!')
    print(f'  Errors: {serializer2.errors}')

