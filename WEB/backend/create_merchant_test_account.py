#!/usr/bin/env python3
"""
Script để tạo tài khoản merchant test
Chạy: python create_merchant_test_account.py
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
from menus.models import Merchant, MerchantMember

User = get_user_model()

# Tài khoản merchant test
MERCHANT_ACCOUNTS = [
    {
        'username': 'merchant_default',
        'email': 'merchant@fastfood.com',
        'password': 'Merchant123',
        'store_name': 'Cửa Hàng Mặc Định',
    },
    {
        'username': 'quancom_bienhoa',
        'email': 'quancom@example.com',
        'password': 'Password123',
        'store_name': 'Quán Cơm Gia Đình',
    },
    {
        'username': 'pizza_bienhoa',
        'email': 'pizza@example.com',
        'password': 'Password123',
        'store_name': 'Pizza & Pasta House',
    },
    {
        'username': 'merchant1',
        'email': 'merchant1@test.com',
        'password': '123456',
        'store_name': 'Merchant Test 1',
    },
]

def create_merchant_account(username, email, password, store_name):
    """Tạo hoặc cập nhật tài khoản merchant"""
    # Tạo hoặc lấy user
    user, created = User.objects.get_or_create(
        username=username,
        defaults={
            'email': email,
            'is_active': True,
        }
    )
    
    if created:
        user.set_password(password)
        user.is_active = True
        user.save()
        print(f'[OK] Da tao user: {username}')
    else:
        # Cap nhat password va dam bao is_active = True
        user.set_password(password)
        user.is_active = True
        user.email = email
        user.save()
        print(f'[OK] Da cap nhat user: {username}')
    
    # Tao hoac cap nhat profile
    profile, profile_created = Profile.objects.get_or_create(
        user=user,
        defaults={'role': 'merchant'}
    )
    
    if not profile_created:
        profile.role = 'merchant'
        profile.store_name = store_name
        profile.save()
        print(f'  [OK] Da cap nhat profile: role = merchant')
    else:
        profile.store_name = store_name
        profile.save()
        print(f'  [OK] Da tao profile: role = merchant')
    
    # Tao merchant neu chua co
    merchant, merchant_created = Merchant.objects.get_or_create(
        owner=user,
        defaults={
            'name': store_name,
            'address': 'Dia chi mau',
            'phone': '0123456789',
            'is_active': True,
        }
    )
    
    if merchant_created:
        print(f'  [OK] Da tao merchant: {store_name}')
    else:
        merchant.is_active = True
        merchant.save()
        print(f'  [OK] Merchant da ton tai: {store_name}')
    
    # Tao MerchantMember neu chua co
    member, member_created = MerchantMember.objects.get_or_create(
        user=user,
        merchant=merchant,
        defaults={'role': 'owner'}
    )
    
    if member_created:
        print(f'  [OK] Da them user vao merchant voi vai tro owner')
    
    return user

def main():
    print('=' * 60)
    print('TAO TAI KHOAN MERCHANT TEST')
    print('=' * 60)
    print()
    
    for account in MERCHANT_ACCOUNTS:
        print(f"Dang xu ly: {account['username']}...")
        try:
            create_merchant_account(
                account['username'],
                account['email'],
                account['password'],
                account['store_name']
            )
            print()
        except Exception as e:
            print(f'  Loi: {e}')
            print()
    
    print('=' * 60)
    print('HOAN TAT!')
    print('=' * 60)
    print()
    print('Cac tai khoan da duoc tao/cap nhat:')
    for account in MERCHANT_ACCOUNTS:
        print(f"  - Username: {account['username']}")
        print(f"    Email: {account['email']}")
        print(f"    Password: {account['password']}")
        print()

if __name__ == '__main__':
    main()

