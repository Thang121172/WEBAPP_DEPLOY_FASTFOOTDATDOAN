#!/usr/bin/env python3
"""
Script để seed dữ liệu mẫu: merchants, menu items, và shippers gần vị trí của bạn
Vị trí: Biên Hòa, Đồng Nai (10.957393, 106.934225)
"""
import os, sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
import django
django.setup()

from django.core.management import call_command

if __name__ == '__main__':
    print('=' * 60)
    print('Seeding demo data: Merchants, Menu Items, and Shippers')
    print('=' * 60)
    call_command('seed_demo')
    print('=' * 60)

