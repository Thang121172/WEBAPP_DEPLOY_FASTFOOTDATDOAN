#!/usr/bin/env python3
"""
Kich hoat tat ca tai khoan user
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

User = get_user_model()

print('=' * 60)
print('KICH HOAT TAT CA TAI KHOAN USER')
print('=' * 60)
print()

# Tim tat ca user chua active
inactive_users = User.objects.filter(is_active=False)
print(f'Tim thay {inactive_users.count()} tai khoan chua kich hoat')
for user in inactive_users:
    print(f'  - {user.username} ({user.email})')
    user.is_active = True
    user.save(update_fields=['is_active'])
    print(f'    [OK] Da kich hoat!')
print()

# Hien thi tat ca user gan day
print('Danh sach user gan day (10 user cuoi cung):')
recent_users = User.objects.all().order_by('-date_joined')[:10]
for user in recent_users:
    status = 'ACTIVE' if user.is_active else 'INACTIVE'
    print(f'  - {user.username} ({user.email}) - {status}')

print()
print('=' * 60)
print('HOAN TAT!')
print('=' * 60)

