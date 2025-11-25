#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để set GPS mặc định cho shipper để test
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
from django.utils import timezone

User = get_user_model()

# GPS mặc định: Biên Hòa, Đồng Nai (gần các merchant)
DEFAULT_LAT = 11.318067
DEFAULT_LNG = 106.050355

print("=" * 70)
print("SET GPS MAC DINH CHO SHIPPER")
print("=" * 70)
print()

# Lấy tất cả shipper
shippers = Profile.objects.filter(role='shipper')

if not shippers.exists():
    print("[WARN] Khong tim thay shipper nao")
else:
    for shipper in shippers:
        user = shipper.user
        print(f"Shipper: {user.username} ({user.email})")
        print(f"  - GPS hien tai: lat={shipper.latitude}, lng={shipper.longitude}")
        
        # Set GPS mặc định
        shipper.latitude = DEFAULT_LAT
        shipper.longitude = DEFAULT_LNG
        shipper.location_updated_at = timezone.now()
        shipper.save(update_fields=['latitude', 'longitude', 'location_updated_at'])
        
        print(f"  - GPS moi: lat={shipper.latitude}, lng={shipper.longitude}")
        print(f"  [OK] Da cap nhat GPS")
        print()

print("=" * 70)
print("GPS mac dinh: lat={}, lng={}".format(DEFAULT_LAT, DEFAULT_LNG))
print("Vi tri: Bien Hoa, Dong Nai (gan cac merchant)")
print("=" * 70)

