#!/usr/bin/env python
"""
Script để seed data mẫu vào database
Chạy: python manage.py shell < scripts/seed_data.py
Hoặc: python manage.py runscript seed_data
"""

import os
import django
from decimal import Decimal
from django.utils import timezone
from datetime import timedelta

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile
from menus.models import Merchant, MenuItem
from orders.models import Order, OrderItem, Review, MenuItemReview, Complaint

User = get_user_model()

def create_users():
    """Tạo users mẫu"""
    print("Creating users...")
    
    users_data = [
        {
            'username': 'admin',
            'email': 'admin@fastfood.com',
            'password': 'admin123',
            'role': 'admin',
            'first_name': 'Admin',
            'last_name': 'User'
        },
        {
            'username': 'customer1',
            'email': 'customer1@test.com',
            'password': '123456',
            'role': 'customer',
            'first_name': 'Nguyễn',
            'last_name': 'Văn A'
        },
        {
            'username': 'customer2',
            'email': 'customer2@test.com',
            'password': '123456',
            'role': 'customer',
            'first_name': 'Trần',
            'last_name': 'Thị B'
        },
        {
            'username': 'merchant1',
            'email': 'merchant1@test.com',
            'password': '123456',
            'role': 'merchant',
            'first_name': 'Lê',
            'last_name': 'Văn C'
        },
        {
            'username': 'merchant2',
            'email': 'merchant2@test.com',
            'password': '123456',
            'role': 'merchant',
            'first_name': 'Phạm',
            'last_name': 'Thị D'
        },
        {
            'username': 'shipper1',
            'email': 'shipper1@test.com',
            'password': '123456',
            'role': 'shipper',
            'first_name': 'Hoàng',
            'last_name': 'Văn E'
        },
        {
            'username': 'shipper2',
            'email': 'shipper2@test.com',
            'password': '123456',
            'role': 'shipper',
            'first_name': 'Vũ',
            'last_name': 'Thị F'
        }
    ]
    
    created_users = []
    for user_data in users_data:
        user, created = User.objects.get_or_create(
            username=user_data['username'],
            defaults={
                'email': user_data['email'],
                'first_name': user_data['first_name'],
                'last_name': user_data['last_name']
            }
        )
        if created:
            user.set_password(user_data['password'])
            user.is_active = True  # Đảm bảo user được kích hoạt
            user.save()
            print(f"  Created user: {user.username}")
        else:
            # Đảm bảo user đã tồn tại cũng được kích hoạt và có password đúng
            user.set_password(user_data['password'])
            user.is_active = True
            user.save()
            print(f"  Updated user: {user.username} (activated & password updated)")
        
        # Tạo hoặc cập nhật profile
        profile, _ = Profile.objects.get_or_create(
            user=user,
            defaults={'role': user_data['role']}
        )
        if profile.role != user_data['role']:
            profile.role = user_data['role']
            profile.save()
        
        created_users.append(user)
    
    return created_users

def create_merchants(users):
    """Tạo merchants mẫu"""
    print("\nCreating merchants...")
    
    merchants_data = [
        {
            'name': 'Pizza Hut',
            'description': 'Pizza ngon nhất thành phố',
            'address': '123 Nguyễn Huệ, Q1, TP.HCM',
            'phone': '0901234567',
            'latitude': Decimal('10.7769'),
            'longitude': Decimal('106.7009'),
            'owner_username': 'merchant1'
        },
        {
            'name': 'KFC',
            'description': 'Gà rán thơm ngon',
            'address': '456 Lê Lợi, Q1, TP.HCM',
            'phone': '0907654321',
            'latitude': Decimal('10.7719'),
            'longitude': Decimal('106.6989'),
            'owner_username': 'merchant2'
        },
        {
            'name': 'McDonald\'s',
            'description': 'Burger và đồ ăn nhanh',
            'address': '789 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM',
            'phone': '0901111111',
            'latitude': Decimal('10.8019'),
            'longitude': Decimal('106.7149'),
            'owner_username': 'merchant1'
        }
    ]
    
    created_merchants = []
    for merchant_data in merchants_data:
        owner = User.objects.get(username=merchant_data.pop('owner_username'))
        merchant, created = Merchant.objects.get_or_create(
            name=merchant_data['name'],
            defaults={**merchant_data, 'owner': owner}
        )
        if created:
            print(f"  Created merchant: {merchant.name}")
        else:
            print(f"  Merchant already exists: {merchant.name}")
        created_merchants.append(merchant)
    
    return created_merchants

def create_menu_items(merchants):
    """Tạo menu items mẫu"""
    print("\nCreating menu items...")
    
    menu_items_data = [
        # Pizza Hut
        {
            'merchant': merchants[0],
            'name': 'Pizza Hải Sản',
            'description': 'Pizza với tôm, mực, cua',
            'price': Decimal('199000'),
            'stock': 50,
            'image_url': ''
        },
        {
            'merchant': merchants[0],
            'name': 'Pizza Thịt Nướng',
            'description': 'Pizza với thịt nướng thơm ngon',
            'price': Decimal('179000'),
            'stock': 30,
            'image_url': ''
        },
        {
            'merchant': merchants[0],
            'name': 'Pizza Phô Mai',
            'description': 'Pizza phô mai đặc biệt',
            'price': Decimal('159000'),
            'stock': 40,
            'image_url': ''
        },
        # KFC
        {
            'merchant': merchants[1],
            'name': 'Gà Rán Giòn',
            'description': '2 miếng gà rán giòn',
            'price': Decimal('89000'),
            'stock': 100,
            'image_url': ''
        },
        {
            'merchant': merchants[1],
            'name': 'Combo Gà Rán',
            'description': 'Gà rán + khoai tây + nước ngọt',
            'price': Decimal('129000'),
            'stock': 80,
            'image_url': ''
        },
        {
            'merchant': merchants[1],
            'name': 'Burger Gà',
            'description': 'Burger gà giòn',
            'price': Decimal('69000'),
            'stock': 60,
            'image_url': ''
        },
        # McDonald's
        {
            'merchant': merchants[2],
            'name': 'Big Mac',
            'description': 'Burger Big Mac cỡ lớn',
            'price': Decimal('99000'),
            'stock': 70,
            'image_url': ''
        },
        {
            'merchant': merchants[2],
            'name': 'McChicken',
            'description': 'Burger gà McChicken',
            'price': Decimal('79000'),
            'stock': 50,
            'image_url': ''
        },
        {
            'merchant': merchants[2],
            'name': 'Khoai Tây Chiên',
            'description': 'Khoai tây chiên giòn',
            'price': Decimal('49000'),
            'stock': 200,
            'image_url': ''
        }
    ]
    
    created_items = []
    for item_data in menu_items_data:
        merchant = item_data.pop('merchant')
        item, created = MenuItem.objects.get_or_create(
            merchant=merchant,
            name=item_data['name'],
            defaults=item_data
        )
        if created:
            print(f"  Created menu item: {item.name} ({merchant.name})")
        else:
            print(f"  Menu item already exists: {item.name}")
        created_items.append(item)
    
    return created_items

def create_orders(users, merchants, menu_items):
    """Tạo orders mẫu"""
    print("\nCreating orders...")
    
    customer1 = User.objects.get(username='customer1')
    customer2 = User.objects.get(username='customer2')
    shipper1 = User.objects.get(username='shipper1')
    
    orders_data = [
        {
            'customer': customer1,
            'merchant': merchants[0],
            'shipper': None,
            'status': Order.Status.DELIVERED,
            'payment_status': Order.PaymentStatus.PAID,
            'delivery_address': '123 Đường ABC, Q1, TP.HCM',
            'note': 'Giao nhanh giúp em',
            'items': [
                {'menu_item': menu_items[0], 'quantity': 1},
                {'menu_item': menu_items[1], 'quantity': 2}
            ],
            'created_at': timezone.now() - timedelta(days=5)
        },
        {
            'customer': customer2,
            'merchant': merchants[1],
            'shipper': shipper1,
            'status': Order.Status.DELIVERING,
            'payment_status': Order.PaymentStatus.PAID,
            'delivery_address': '456 Đường XYZ, Q2, TP.HCM',
            'note': '',
            'items': [
                {'menu_item': menu_items[3], 'quantity': 2},
                {'menu_item': menu_items[4], 'quantity': 1}
            ],
            'created_at': timezone.now() - timedelta(hours=2)
        },
        {
            'customer': customer1,
            'merchant': merchants[2],
            'shipper': None,
            'status': Order.Status.CONFIRMED,
            'payment_status': Order.PaymentStatus.PAID,
            'delivery_address': '789 Đường DEF, Q3, TP.HCM',
            'note': 'Ít cay',
            'items': [
                {'menu_item': menu_items[6], 'quantity': 1},
                {'menu_item': menu_items[8], 'quantity': 2}
            ],
            'created_at': timezone.now() - timedelta(hours=1)
        },
        {
            'customer': customer2,
            'merchant': merchants[0],
            'shipper': None,
            'status': Order.Status.PENDING,
            'payment_status': Order.PaymentStatus.UNPAID,
            'delivery_address': '321 Đường GHI, Q4, TP.HCM',
            'note': '',
            'items': [
                {'menu_item': menu_items[2], 'quantity': 1}
            ],
            'created_at': timezone.now() - timedelta(minutes=30)
        }
    ]
    
    created_orders = []
    for order_data in orders_data:
        items_data = order_data.pop('items')
        created_at = order_data.pop('created_at')
        
        # Tính tổng tiền
        total_amount = Decimal('0')
        for item_data in items_data:
            total_amount += item_data['menu_item'].price * item_data['quantity']
        
        order = Order.objects.create(
            **order_data,
            total_amount=total_amount,
            created_at=created_at
        )
        
        # Tạo order items
        for item_data in items_data:
            menu_item = item_data['menu_item']
            quantity = item_data['quantity']
            OrderItem.objects.create(
                order=order,
                menu_item=menu_item,
                name_snapshot=menu_item.name,
                price_snapshot=menu_item.price,
                quantity=quantity,
                line_total=menu_item.price * quantity
            )
        
        print(f"  Created order: #{order.id} - {order.merchant.name} - {order.status}")
        created_orders.append(order)
    
    return created_orders

def create_reviews(orders):
    """Tạo reviews mẫu"""
    print("\nCreating reviews...")
    
    # Chỉ review đơn đã DELIVERED
    delivered_orders = [o for o in orders if o.status == Order.Status.DELIVERED]
    
    if delivered_orders:
        order = delivered_orders[0]
        review, created = Review.objects.get_or_create(
            order=order,
            customer=order.customer,
            defaults={
                'order_rating': 5,
                'merchant_rating': 5,
                'shipper_rating': 5 if order.shipper else None,
                'comment': 'Rất hài lòng với dịch vụ!'
            }
        )
        if created:
            print(f"  Created review for order #{order.id}")
            
            # Tạo menu item reviews
            for order_item in order.items.all()[:2]:  # Review 2 món đầu
                MenuItemReview.objects.create(
                    review=review,
                    order_item=order_item,
                    rating=5,
                    comment='Món rất ngon!'
                )
        else:
            print(f"  Review already exists for order #{order.id}")

def create_complaints(orders):
    """Tạo complaints mẫu"""
    print("\nCreating complaints...")
    
    # Tạo 1 complaint mẫu
    if orders:
        order = orders[0]
        complaint, created = Complaint.objects.get_or_create(
            order=order,
            customer=order.customer,
            defaults={
                'complaint_type': Complaint.Type.FOOD_QUALITY,
                'title': 'Món ăn không đúng với đơn đặt',
                'description': 'Tôi đặt pizza hải sản nhưng nhận được pizza thịt nướng',
                'status': Complaint.Status.PENDING
            }
        )
        if created:
            print(f"  Created complaint for order #{order.id}")
        else:
            print(f"  Complaint already exists for order #{order.id}")

def main():
    print("=" * 50)
    print("SEEDING DATABASE")
    print("=" * 50)
    
    users = create_users()
    merchants = create_merchants(users)
    menu_items = create_menu_items(merchants)
    orders = create_orders(users, merchants, menu_items)
    create_reviews(orders)
    create_complaints(orders)
    
    print("\n" + "=" * 50)
    print("SEEDING COMPLETED!")
    print("=" * 50)
    print(f"\nSummary:")
    print(f"  - Users: {User.objects.count()}")
    print(f"  - Merchants: {Merchant.objects.count()}")
    print(f"  - Menu Items: {MenuItem.objects.count()}")
    print(f"  - Orders: {Order.objects.count()}")
    print(f"  - Reviews: {Review.objects.count()}")
    print(f"  - Complaints: {Complaint.objects.count()}")
    print("\nTest accounts:")
    print("  - Admin: admin / admin123")
    print("  - Customer: customer1 / 123456")
    print("  - Merchant: merchant1 / 123456")
    print("  - Shipper: shipper1 / 123456")

if __name__ == '__main__':
    main()

