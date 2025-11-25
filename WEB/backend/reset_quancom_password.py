#!/usr/bin/env python3
"""
Script để reset password cho tài khoản quancom_bienhoa
"""
import os
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')

import django
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile

User = get_user_model()

print('=' * 60)
print('RESET PASSWORD CHO TAI KHOAN quancom_bienhoa')
print('=' * 60)
print()

username = 'quancom_bienhoa'
email = 'quancom@example.com'
password = 'Password123'

try:
    user = User.objects.get(username=username)
    print(f'✓ Tìm thấy user: {username}')
    print(f'  Email: {user.email}')
    print(f'  Is Active: {user.is_active}')
    
    # Reset password
    user.set_password(password)
    user.is_active = True
    user.save()
    
    print()
    print(f'✓ Đã reset password thành công!')
    print()
    print('=' * 60)
    print('THÔNG TIN ĐĂNG NHẬP:')
    print('=' * 60)
    print(f'Username: {username}')
    print(f'Email: {email}')
    print(f'Password: {password}')
    print('=' * 60)
    print()
    
    # Kiểm tra profile
    try:
        profile = Profile.objects.get(user=user)
        print(f'✓ Profile: Role = {profile.role}')
    except Profile.DoesNotExist:
        print('⚠️  Chưa có profile')
        
except User.DoesNotExist:
    print(f'❌ Không tìm thấy user: {username}')
    print('   Có thể cần chạy script seed để tạo tài khoản.')

