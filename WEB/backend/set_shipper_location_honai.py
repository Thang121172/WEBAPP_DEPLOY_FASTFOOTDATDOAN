#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để set GPS của shipper về Khu công nghiệp Hố Nai, gần khách hàng
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

# GPS Khu công nghiệp Hố Nai, Biên Hòa, Đồng Nai (gần khách hàng và merchant)
# Vị trí: KCN Hố Nai, Phường Hố Nai, Biên Hòa
HONAI_LAT = 11.315000  # Gần với customer (11.318067) và merchant (11.32x)
HONAI_LNG = 106.049000  # Gần với customer (106.050355) và merchant (106.05x)

print("=" * 70)
print("SET GPS SHIPPER VE KHU CONG NGHIEP HO NAI")
print("=" * 70)
print()

# Tìm shipper đang test (có thể là testshipperrr@gmail.com hoặc shipper mới nhất)
# Hoặc set cho tất cả shipper
shippers = Profile.objects.filter(role='shipper')

if not shippers.exists():
    print("[WARN] Khong tim thay shipper nao")
else:
    print(f"Tim thay {shippers.count()} shipper. Dang cap nhat GPS...")
    print()
    
    for shipper in shippers:
        user = shipper.user
        print(f"Shipper: {user.username} ({user.email})")
        print(f"  - GPS cu: lat={shipper.latitude}, lng={shipper.longitude}")
        
        # Set GPS về KCN Hố Nai
        shipper.latitude = HONAI_LAT
        shipper.longitude = HONAI_LNG
        shipper.location_updated_at = timezone.now()
        shipper.save(update_fields=['latitude', 'longitude', 'location_updated_at'])
        
        print(f"  - GPS moi: lat={shipper.latitude}, lng={shipper.longitude}")
        print(f"  [OK] Da cap nhat GPS ve KCN Ho Nai")
        print()

print("=" * 70)
print("GPS KCN HO NAI: lat={}, lng={}".format(HONAI_LAT, HONAI_LNG))
print("Vi tri: Khu cong nghiep Ho Nai, Bien Hoa, Dong Nai")
print("Gan voi:")
print("  - Customer: 11.318067, 106.050355 (~300m)")
print("  - Merchant (Quan Com): 11.320000, 106.052000 (~600m)")
print("=" * 70)

