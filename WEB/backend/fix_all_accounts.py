#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để kích hoạt và đặt lại mật khẩu cho TẤT CẢ tài khoản
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

# Mật khẩu mặc định cho tất cả tài khoản
DEFAULT_PASSWORD = 'Password123'

print("=" * 70)
print("KICH HOAT VA DAT LAI MAT KHAU CHO TAT CA TAI KHOAN")
print("=" * 70)
print()

# Lấy tất cả user
all_users = User.objects.all()

print(f"Tim thay {all_users.count()} tai khoan. Dang cap nhat...")
print()

fixed_count = 0

for user in all_users:
    try:
        # Kích hoạt tài khoản
        if not user.is_active:
            user.is_active = True
            fixed_count += 1
        
        # Đặt lại mật khẩu
        user.set_password(DEFAULT_PASSWORD)
        user.save()
        
        # Lấy role từ profile
        try:
            profile = user.profile
            role = profile.role
        except Profile.DoesNotExist:
            role = 'N/A'
        
        print(f"[OK] {user.username} ({user.email}) - Role: {role} - is_active: {user.is_active}")
        
    except Exception as e:
        print(f"[LOI] {user.username}: {e}")

print()
print("=" * 70)
print(f"Da cap nhat {fixed_count} tai khoan chua kich hoat")
print(f"Da dat lai mat khau cho TAT CA tai khoan: {DEFAULT_PASSWORD}")
print("=" * 70)
print()
print("THONG TIN DANG NHAP MAC DINH:")
print(f"Password: {DEFAULT_PASSWORD}")
print()
print("Cac tai khoan co the dung:")
print("  - Customer: bat ky email nao")
print("  - Merchant: quancom_bienhoa / quancom@example.com")
print("  - Shipper: testshipperrr@gmail.com")
print("=" * 70)

