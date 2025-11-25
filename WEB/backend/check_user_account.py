#!/usr/bin/env python3
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

# Tìm tài khoản vừa đăng ký
email = 'tesstaccc@gmail.com'
print(f'Dang tim tai khoan voi email: {email}')
print('=' * 60)

try:
    user = User.objects.get(email__iexact=email)
    print(f'[OK] Tim thay user:')
    print(f'  - ID: {user.id}')
    print(f'  - Username: {user.username}')
    print(f'  - Email: {user.email}')
    print(f'  - is_active: {user.is_active}')
    print(f'  - date_joined: {user.date_joined}')
    
    # Kiem tra profile
    try:
        profile = user.profile
        print(f'  - Profile role: {profile.role}')
    except Profile.DoesNotExist:
        print(f'  - Profile: KHONG CO')
    
    # Test password
    print(f'\nDang test password...')
    from django.contrib.auth import authenticate
    test_user = authenticate(username=user.username, password='Thang2004')
    if test_user:
        print(f'  [OK] Password dung!')
    else:
        print(f'  [LOI] Password sai hoac user khong active!')
        # Thu authenticate voi email
        test_user2 = authenticate(username=user.email, password='Thang2004')
        if test_user2:
            print(f'  [OK] Password dung khi dung email!')
        else:
            print(f'  [LOI] Password sai!')
    
    # Kich hoat tai khoan neu chua active
    if not user.is_active:
        print(f'\n[WARNING] Tai khoan chua duoc kich hoat!')
        print(f'Dang kich hoat tai khoan...')
        user.is_active = True
        user.save(update_fields=['is_active'])
        print(f'[OK] Da kich hoat tai khoan!')
    
except User.DoesNotExist:
    print(f'[LOI] Khong tim thay user voi email: {email}')
    print(f'\nDanh sach user gan day:')
    recent_users = User.objects.all().order_by('-date_joined')[:5]
    for u in recent_users:
        print(f'  - {u.username} ({u.email}) - Active: {u.is_active}')

