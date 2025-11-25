#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Script để kiểm tra đơn hàng đã giao của shipper
"""
import os
import sys
import django
import io
from datetime import datetime

if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from orders.models import Order
from django.contrib.auth import get_user_model
from django.utils import timezone

User = get_user_model()

# Tìm shipper testshipperrr@gmail.com
try:
    shipper = User.objects.get(email='testshipperrr@gmail.com')
    print(f"Shipper: {shipper.username} ({shipper.email})")
    print()
    
    # Lấy tất cả đơn đã giao
    delivered_orders = Order.objects.filter(
        shipper=shipper,
        status=Order.Status.DELIVERED
    ).order_by('-updated_at')
    
    print(f"Tong so don da giao: {delivered_orders.count()}")
    print()
    
    # Kiểm tra từng đơn
    today = timezone.now().date()
    today_start = timezone.make_aware(timezone.datetime.combine(today, timezone.datetime.min.time()))
    
    print(f"Ngay hom nay: {today}")
    print(f"Thoi gian bat dau hom nay: {today_start}")
    print()
    
    today_count = 0
    today_earnings = 0
    
    for order in delivered_orders:
        is_today = order.updated_at >= today_start
        delivery_fee = 20000  # Phí cơ bản
        
        print(f"Don #{order.id}:")
        print(f"  - Status: {order.status}")
        print(f"  - Created: {order.created_at}")
        print(f"  - Updated: {order.updated_at}")
        print(f"  - Updated date: {order.updated_at.date()}")
        print(f"  - Is today: {is_today}")
        print(f"  - Delivery fee: {delivery_fee} VND")
        print()
        
        if is_today:
            today_count += 1
            today_earnings += delivery_fee
    
    print(f"Tong ket:")
    print(f"  - Don giao hom nay: {today_count}")
    print(f"  - Thu nhap hom nay: {today_earnings} VND")
    
except User.DoesNotExist:
    print("Khong tim thay shipper testshipperrr@gmail.com")

