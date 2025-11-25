#!/usr/bin/env python3
"""
Script để seed dữ liệu mẫu: merchants, menu items, và shippers gần vị trí của bạn
Có thể chạy trực tiếp từ root directory: python run_seed.py
"""
import os
import sys
from pathlib import Path

# Thêm backend vào Python path
BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR / 'backend'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')

import django
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile
from menus.models import Merchant, MerchantMember, Category, MenuItem
from decimal import Decimal

User = get_user_model()

# Vị trí của bạn: Biên Hòa, Đồng Nai
CUSTOMER_LAT = 11.318067
CUSTOMER_LNG = 106.050355

# Tạo các merchant gần vị trí của bạn (trong phạm vi 10km)
MERCHANTS_DATA = [
    {
        'username': 'quancom_bienhoa',
        'email': 'quancom@example.com',
        'password': 'Password123',
        'name': 'Quán Cơm Gia Đình',
        'address': '123 Đường Hoàng Văn Bồn, Phường Long Bình, Biên Hòa, Đồng Nai',
        'phone': '02513812345',
        'latitude': 11.320000,  # ~300m từ vị trí bạn
        'longitude': 106.052000,
        'description': 'Quán cơm gia đình với các món ăn Việt Nam truyền thống',
        'menu_items': [
            {'name': 'Cơm Sườn Nướng', 'description': 'Cơm với sườn nướng thơm lừng', 'price': 45000, 'stock': 50},
            {'name': 'Cơm Gà Nướng', 'description': 'Cơm với gà nướng mật ong', 'price': 50000, 'stock': 40},
            {'name': 'Cơm Tấm Sườn Bì Chả', 'description': 'Cơm tấm đầy đủ sườn, bì, chả', 'price': 55000, 'stock': 60},
            {'name': 'Canh Chua Cá', 'description': 'Canh chua cá lóc nấu dứa', 'price': 60000, 'stock': 30},
            {'name': 'Bún Bò Huế', 'description': 'Bún bò Huế đậm đà', 'price': 50000, 'stock': 35},
        ]
    },
    {
        'username': 'pizza_bienhoa',
        'email': 'pizza@example.com',
        'password': 'Password123',
        'name': 'Pizza & Pasta House',
        'address': '456 Đường Phạm Văn Thuận, Phường Tân Hiệp, Biên Hòa, Đồng Nai',
        'phone': '02513812346',
        'latitude': 11.316000,  # ~500m từ vị trí bạn
        'longitude': 106.048000,
        'description': 'Pizza và pasta Ý chính thống',
        'menu_items': [
            {'name': 'Pizza Margherita', 'description': 'Pizza phô mai mozzarella và cà chua', 'price': 120000, 'stock': 25},
            {'name': 'Pizza Hải Sản', 'description': 'Pizza với tôm, mực, cua', 'price': 180000, 'stock': 20},
            {'name': 'Spaghetti Carbonara', 'description': 'Mì Ý sốt kem và thịt xông khói', 'price': 95000, 'stock': 30},
            {'name': 'Lasagna', 'description': 'Lasagna thịt bò và phô mai', 'price': 110000, 'stock': 15},
            {'name': 'Pizza 4 Mùa', 'description': 'Pizza 4 loại topping khác nhau', 'price': 200000, 'stock': 18},
        ]
    },
    {
        'username': 'bunthitnuong_bienhoa',
        'email': 'bunthitnuong@example.com',
        'password': 'Password123',
        'name': 'Bún Thịt Nướng Cô Ba',
        'address': '789 Đường Nguyễn Ái Quốc, Phường Tân Phong, Biên Hòa, Đồng Nai',
        'phone': '02513812347',
        'latitude': 11.322000,  # ~600m từ vị trí bạn
        'longitude': 106.053000,
        'description': 'Bún thịt nướng đặc sản miền Nam',
        'menu_items': [
            {'name': 'Bún Thịt Nướng', 'description': 'Bún với thịt nướng, chả giò', 'price': 40000, 'stock': 80},
            {'name': 'Bún Thịt Nướng Đặc Biệt', 'description': 'Bún thịt nướng + chả giò + nem nướng', 'price': 55000, 'stock': 50},
            {'name': 'Bún Bò Xào', 'description': 'Bún với bò xào rau củ', 'price': 50000, 'stock': 40},
            {'name': 'Bún Chả Giò', 'description': 'Bún với chả giò giòn tan', 'price': 35000, 'stock': 60},
        ]
    },
    {
        'username': 'pho_bienhoa',
        'email': 'pho@example.com',
        'password': 'Password123',
        'name': 'Phở Gia Truyền',
        'address': '321 Đường Trần Hưng Đạo, Phường Quang Vinh, Biên Hòa, Đồng Nai',
        'phone': '02513812348',
        'latitude': 11.314000,  # ~800m từ vị trí bạn
        'longitude': 106.046000,
        'description': 'Phở bò, phở gà nước dùng đậm đà',
        'menu_items': [
            {'name': 'Phở Bò Tái', 'description': 'Phở bò tái chín', 'price': 55000, 'stock': 70},
            {'name': 'Phở Bò Chín', 'description': 'Phở bò chín mềm', 'price': 55000, 'stock': 65},
            {'name': 'Phở Gà', 'description': 'Phở gà thơm ngon', 'price': 50000, 'stock': 60},
            {'name': 'Phở Đặc Biệt', 'description': 'Phở đầy đủ tái, chín, gầu, bò viên', 'price': 70000, 'stock': 45},
            {'name': 'Phở Bò Viên', 'description': 'Phở với bò viên', 'price': 50000, 'stock': 55},
        ]
    },
    {
        'username': 'banhmi_bienhoa',
        'email': 'banhmi@example.com',
        'password': 'Password123',
        'name': 'Bánh Mì Sài Gòn',
        'address': '654 Đường Lê Lợi, Phường Tân Mai, Biên Hòa, Đồng Nai',
        'phone': '02513812349',
        'latitude': 11.325000,  # ~1km từ vị trí bạn
        'longitude': 106.055000,
        'description': 'Bánh mì Sài Gòn đủ loại',
        'menu_items': [
            {'name': 'Bánh Mì Thịt Nướng', 'description': 'Bánh mì với thịt nướng', 'price': 25000, 'stock': 100},
            {'name': 'Bánh Mì Pate', 'description': 'Bánh mì với pate và thịt nguội', 'price': 20000, 'stock': 120},
            {'name': 'Bánh Mì Chả Cá', 'description': 'Bánh mì với chả cá', 'price': 30000, 'stock': 80},
            {'name': 'Bánh Mì Đặc Biệt', 'description': 'Bánh mì đầy đủ thịt, pate, chả', 'price': 35000, 'stock': 90},
        ]
    },
    {
        'username': 'comtam_bienhoa',
        'email': 'comtam@example.com',
        'password': 'Password123',
        'name': 'Cơm Tấm Cali',
        'address': '987 Đường Võ Thị Sáu, Phường Tam Hiệp, Biên Hòa, Đồng Nai',
        'phone': '02513812350',
        'latitude': 11.312000,  # ~1.2km từ vị trí bạn
        'longitude': 106.044000,
        'description': 'Cơm tấm Sài Gòn đúng chuẩn',
        'menu_items': [
            {'name': 'Cơm Tấm Sườn', 'description': 'Cơm tấm với sườn nướng', 'price': 50000, 'stock': 70},
            {'name': 'Cơm Tấm Bì', 'description': 'Cơm tấm với bì', 'price': 45000, 'stock': 60},
            {'name': 'Cơm Tấm Chả', 'description': 'Cơm tấm với chả trứng', 'price': 45000, 'stock': 65},
            {'name': 'Cơm Tấm Đặc Biệt', 'description': 'Cơm tấm đầy đủ sườn, bì, chả', 'price': 60000, 'stock': 50},
        ]
    },
    {
        'username': 'cafe_bienhoa',
        'email': 'cafe@example.com',
        'password': 'Password123',
        'name': 'Cà Phê Sáng',
        'address': '147 Đường Nguyễn Văn Trị, Phường Long Bình Tân, Biên Hòa, Đồng Nai',
        'phone': '02513812351',
        'latitude': 11.319000,  # ~200m từ vị trí bạn
        'longitude': 106.051000,
        'description': 'Cà phê và đồ uống giải khát',
        'menu_items': [
            {'name': 'Cà Phê Đen', 'description': 'Cà phê đen đá', 'price': 15000, 'stock': 200},
            {'name': 'Cà Phê Sữa', 'description': 'Cà phê sữa đá', 'price': 20000, 'stock': 200},
            {'name': 'Sinh Tố Bơ', 'description': 'Sinh tố bơ tươi', 'price': 35000, 'stock': 50},
            {'name': 'Nước Cam Ép', 'description': 'Nước cam ép tươi', 'price': 30000, 'stock': 60},
            {'name': 'Trà Đá', 'description': 'Trà đá mát lạnh', 'price': 10000, 'stock': 300},
        ]
    },
    {
        'username': 'chicken_bienhoa',
        'email': 'chicken@example.com',
        'password': 'Password123',
        'name': 'Gà Rán KFC Style',
        'address': '258 Đường Đồng Khởi, Phường Tân Hòa, Biên Hòa, Đồng Nai',
        'phone': '02513812352',
        'latitude': 11.321000,  # ~700m từ vị trí bạn
        'longitude': 106.054000,
        'description': 'Gà rán giòn, nóng hổi',
        'menu_items': [
            {'name': 'Gà Rán 2 Miếng', 'description': '2 miếng gà rán giòn', 'price': 65000, 'stock': 40},
            {'name': 'Gà Rán 4 Miếng', 'description': '4 miếng gà rán giòn', 'price': 120000, 'stock': 30},
            {'name': 'Combo Gà Rán', 'description': 'Gà rán + khoai tây + nước', 'price': 85000, 'stock': 35},
            {'name': 'Cánh Gà Rán', 'description': '6 cánh gà rán', 'price': 70000, 'stock': 45},
        ]
    },
]

SHIPPERS_DATA = [
    {
        'username': 'shipper1',
        'email': 'shipper1@example.com',
        'password': 'Password123',
    },
    {
        'username': 'shipper2',
        'email': 'shipper2@example.com',
        'password': 'Password123',
    },
    {
        'username': 'shipper3',
        'email': 'shipper3@example.com',
        'password': 'Password123',
    },
]


def main():
    print('=' * 60)
    print('Seeding demo data: Merchants, Menu Items, and Shippers')
    print(f'Your location: {CUSTOMER_LAT}, {CUSTOMER_LNG}')
    print('=' * 60)
    
    # Tạo merchants và menu items
    for merchant_data in MERCHANTS_DATA:
        # Tạo hoặc lấy user
        user, created = User.objects.get_or_create(
            username=merchant_data['username'],
            defaults={
                'email': merchant_data['email'],
            }
        )
        if created:
            user.set_password(merchant_data['password'])
            user.is_active = True
            user.save()
            print(f'✓ Created user: {merchant_data["username"]}')
        else:
            print(f'  User exists: {merchant_data["username"]}')
        
        # Tạo hoặc cập nhật profile
        profile, _ = Profile.objects.get_or_create(
            user=user,
            defaults={'role': 'merchant'}
        )
        if profile.role != 'merchant':
            profile.role = 'merchant'
            profile.save()
        
        # Tạo hoặc cập nhật merchant
        defaults = {
            'name': merchant_data['name'],
            'address': merchant_data['address'],
            'phone': merchant_data['phone'],
            'latitude': merchant_data['latitude'],
            'longitude': merchant_data['longitude'],
            'is_active': True,
        }
        # Chỉ thêm description nếu field tồn tại
        if hasattr(Merchant, 'description'):
            defaults['description'] = merchant_data.get('description', '')
        
        merchant, created = Merchant.objects.get_or_create(
            owner=user,
            defaults=defaults
        )
        
        if not created:
            # Cập nhật tọa độ nếu chưa có
            if not merchant.latitude or not merchant.longitude:
                merchant.latitude = merchant_data['latitude']
                merchant.longitude = merchant_data['longitude']
                merchant.save()
            print(f'  Merchant exists: {merchant.name}')
        else:
            print(f'✓ Created merchant: {merchant.name}')
        
        # Tạo MerchantMember
        MerchantMember.objects.get_or_create(
            merchant=merchant,
            user=user,
            defaults={'role': 'owner'}
        )
        
        # Tạo category mặc định
        category, _ = Category.objects.get_or_create(
            merchant=merchant,
            name='Món Chính',
            defaults={'description': 'Các món ăn chính'}
        )
        
        # Tạo menu items
        for item_data in merchant_data['menu_items']:
            menu_item, created = MenuItem.objects.get_or_create(
                merchant=merchant,
                name=item_data['name'],
                defaults={
                    'category': category,
                    'description': item_data.get('description', ''),
                    'price': Decimal(str(item_data['price'])),
                    'stock': item_data.get('stock', 50),
                    'is_available': True,
                }
            )
            if created:
                print(f'    - Created menu item: {menu_item.name}')
    
    # Tạo shippers
    for shipper_data in SHIPPERS_DATA:
        user, created = User.objects.get_or_create(
            username=shipper_data['username'],
            defaults={
                'email': shipper_data['email'],
            }
        )
        if created:
            user.set_password(shipper_data['password'])
            user.is_active = True
            user.save()
            print(f'✓ Created shipper: {shipper_data["username"]}')
        else:
            print(f'  User exists: {shipper_data["username"]}')
        
        # Tạo hoặc cập nhật profile
        profile, _ = Profile.objects.get_or_create(
            user=user,
            defaults={'role': 'shipper'}
        )
        if profile.role != 'shipper':
            profile.role = 'shipper'
            profile.save()
    
    print('=' * 60)
    print(f'✓ Done! Created {len(MERCHANTS_DATA)} merchants and {len(SHIPPERS_DATA)} shippers')
    print('=' * 60)
    print('\n📋 Account Credentials:')
    print('=' * 60)
    for merchant_data in MERCHANTS_DATA:
        print(f"Merchant: {merchant_data['username']} / Password: {merchant_data['password']}")
    for shipper_data in SHIPPERS_DATA:
        print(f"Shipper: {shipper_data['username']} / Password: {shipper_data['password']}")
    print('=' * 60)


if __name__ == '__main__':
    main()

