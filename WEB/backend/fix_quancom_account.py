#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để kiểm tra và sửa tài khoản quancom_bienhoa
"""
import os
import sys
import django
import io

# Fix encoding for Windows console
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile
from menus.models import Merchant

User = get_user_model()

def fix_quancom_account():
    username = 'quancom_bienhoa'
    email = 'quancom@example.com'
    password = 'Password123'
    
    print("=" * 70)
    print("KIEM TRA VA SUA TAI KHOAN QUANCOM_BIENHOA")
    print("=" * 70)
    print()
    
    # Tìm user
    try:
        user = User.objects.get(username=username)
        print(f"[OK] Tim thay user: {username}")
        print(f"  - Email: {user.email}")
        print(f"  - is_active: {user.is_active}")
        print(f"  - is_staff: {user.is_staff}")
        print(f"  - is_superuser: {user.is_superuser}")
    except User.DoesNotExist:
        print(f"[LOI] Khong tim thay user: {username}")
        print("  Dang tao user moi...")
        user = User.objects.create_user(
            username=username,
            email=email,
            password=password,
            is_active=True
        )
        print(f"[OK] Da tao user: {username}")
    
    # Kiểm tra và sửa is_active
    if not user.is_active:
        print(f"[WARN] Tai khoan chua duoc kich hoat. Dang kich hoat...")
        user.is_active = True
        user.save()
        print(f"[OK] Da kich hoat tai khoan")
    else:
        print(f"[OK] Tai khoan da duoc kich hoat")
    
    # Đặt lại mật khẩu
    print(f"  Dang dat lai mat khau...")
    user.set_password(password)
    user.save()
    print(f"[OK] Da dat lai mat khau: {password}")
    
    # Kiểm tra Profile
    try:
        profile = user.profile
        print(f"[OK] Tim thay profile")
        print(f"  - Role: {profile.role}")
        if profile.role != 'merchant':
            profile.role = 'merchant'
            profile.save()
            print(f"  [OK] Da cap nhat role thanh 'merchant'")
    except Profile.DoesNotExist:
        print(f"[WARN] Khong tim thay profile. Dang tao profile...")
        profile = Profile.objects.create(
            user=user,
            role='merchant'
        )
        print(f"[OK] Da tao profile voi role='merchant'")
    
    # Kiểm tra Merchant
    try:
        merchant = Merchant.objects.get(owner=user)
        print(f"[OK] Tim thay merchant")
        print(f"  - Ten: {merchant.name}")
        print(f"  - ID: {merchant.id}")
        print(f"  - Dia chi: {merchant.address}")
    except Merchant.DoesNotExist:
        print(f"[WARN] Khong tim thay merchant. Dang tao merchant...")
        merchant = Merchant.objects.create(
            owner=user,
            name='Quan Com Gia Dinh',
            address='123 Duong Hoang Van Bon, Phuong Long Binh, Bien Hoa, Dong Nai',
            phone='02513812345',
            latitude=11.320000,
            longitude=106.052000,
            description='Quan com gia dinh voi cac mon an Viet Nam truyen thong'
        )
        print(f"[OK] Da tao merchant: {merchant.name}")
    
    print()
    print("=" * 70)
    print("THONG TIN DANG NHAP:")
    print("=" * 70)
    print(f"Username: {username}")
    print(f"Email: {email}")
    print(f"Password: {password}")
    print(f"is_active: {user.is_active}")
    print(f"Role: {user.profile.role}")
    print("=" * 70)
    print()
    print("Ban co the dang nhap voi thong tin tren!")

if __name__ == '__main__':
    try:
        fix_quancom_account()
    except Exception as e:
        print(f"Loi: {e}")
        import traceback
        traceback.print_exc()

