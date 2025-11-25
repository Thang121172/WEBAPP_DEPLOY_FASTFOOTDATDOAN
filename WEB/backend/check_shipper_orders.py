#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để kiểm tra tại sao shipper không thấy đơn hàng
"""
import os
import sys
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from orders.models import Order
from menus.models import Merchant
from accounts.models import Profile

print("=" * 70)
print("KIEM TRA DON HANG CHO SHIPPER")
print("=" * 70)
print()

# Lấy tất cả đơn hàng READY hoặc PENDING, chưa có shipper
orders = Order.objects.filter(
    status__in=[Order.Status.READY, Order.Status.PENDING],
    shipper__isnull=True
).select_related('merchant', 'customer')

print(f"Tim thay {orders.count()} don hang READY/PENDING chua co shipper:")
print()

for order in orders:
    merchant_name = order.merchant.name.encode('ascii', 'ignore').decode('ascii') if order.merchant.name else 'N/A'
    print(f"Don hang #{order.id}:")
    print(f"  - Status: {order.status}")
    print(f"  - Merchant: {merchant_name} (ID: {order.merchant.id})")
    print(f"  - Merchant GPS: lat={order.merchant.latitude}, lng={order.merchant.longitude}")
    print(f"  - Customer: {order.customer.username}")
    print(f"  - Created: {order.created_at}")
    print()

# Kiểm tra merchant có GPS không
print("=" * 70)
print("KIEM TRA GPS CUA MERCHANT:")
print("=" * 70)
merchants = Merchant.objects.all()
for merchant in merchants:
    has_gps = merchant.latitude is not None and merchant.longitude is not None
    print(f"Merchant {merchant.id}: {merchant.name}")
    print(f"  - GPS: {'Co' if has_gps else 'KHONG CO'}")
    if has_gps:
        print(f"  - lat={merchant.latitude}, lng={merchant.longitude}")
    print()

# Kiểm tra shipper có GPS không
print("=" * 70)
print("KIEM TRA GPS CUA SHIPPER:")
print("=" * 70)
shippers = Profile.objects.filter(role='shipper')
for shipper in shippers:
    has_gps = shipper.latitude is not None and shipper.longitude is not None
    print(f"Shipper: {shipper.user.username}")
    print(f"  - GPS: {'Co' if has_gps else 'KHONG CO'}")
    if has_gps:
        print(f"  - lat={shipper.latitude}, lng={shipper.longitude}")
    print()

