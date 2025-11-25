#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để kiểm tra và sửa tài khoản shipper
"""
import os
import sys
import django
import io

if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile

User = get_user_model()

# Tìm tất cả shipper
shippers = Profile.objects.filter(role='shipper')

print("=" * 70)
print("KIEM TRA VA SUA TAI KHOAN SHIPPER")
print("=" * 70)
print()

for shipper_profile in shippers:
    user = shipper_profile.user
    print(f"Shipper: {user.username} ({user.email})")
    print(f"  - is_active: {user.is_active}")
    print(f"  - Password: Password123 (mac dinh)")
    
    # Kích hoạt tài khoản
    if not user.is_active:
        user.is_active = True
        user.save()
        print(f"  [OK] Da kich hoat tai khoan")
    else:
        print(f"  [OK] Tai khoan da duoc kich hoat")
    
    # Đặt lại mật khẩu
    user.set_password('Password123')
    user.save()
    print(f"  [OK] Da dat lai mat khau: Password123")
    print()

print("=" * 70)
print("THONG TIN DANG NHAP:")
print("=" * 70)
print("Username hoac Email: testshipperrr@gmail.com")
print("Password: Password123")
print("=" * 70)

