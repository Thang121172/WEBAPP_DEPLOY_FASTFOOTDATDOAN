#!/usr/bin/env python3
"""
Tim tai khoan cua hang "Quan Com Gia Dinh"
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
from menus.models import Merchant
from accounts.models import Profile

User = get_user_model()

print('=' * 60)
print('TIM TAI KHOAN CUA HANG: QUAN COM GIA DINH')
print('=' * 60)
print()

# Tim merchant theo ten
merchants = Merchant.objects.filter(name__icontains='com gia dinh')
print(f'Tim thay {merchants.count()} cua hang:')
print()

for merchant in merchants:
    user = merchant.owner
    try:
        profile = user.profile
    except Profile.DoesNotExist:
        profile = None
    
    print(f'Cua hang: {merchant.name}')
    print(f'  - ID: {merchant.id}')
    print(f'  - Username: {user.username}')
    print(f'  - Email: {user.email}')
    print(f'  - is_active: {user.is_active}')
    print(f'  - Role: {profile.role if profile else "N/A"}')
    print(f'  - Dia chi: {merchant.address}')
    print(f'  - Dien thoai: {merchant.phone}')
    print()

# Tim theo username
print('Tim theo username "quancom_bienhoa":')
try:
    user = User.objects.get(username='quancom_bienhoa')
    print(f'  [OK] Tim thay user:')
    print(f'    - Username: {user.username}')
    print(f'    - Email: {user.email}')
    print(f'    - is_active: {user.is_active}')
    
    try:
        profile = user.profile
        print(f'    - Role: {profile.role}')
    except Profile.DoesNotExist:
        print(f'    - Profile: Khong co')
    
    merchant = Merchant.objects.filter(owner=user).first()
    if merchant:
        print(f'    - Merchant ID: {merchant.id}')
        print(f'    - Merchant is_active: {merchant.is_active}')
    
    print()
    print('=' * 60)
    print('THONG TIN DANG NHAP:')
    print('=' * 60)
    print(f'Email: {user.email}')
    print(f'Password: Password123')
    print('=' * 60)
    
except User.DoesNotExist:
    print('  [LOI] Khong tim thay user voi username: quancom_bienhoa')

