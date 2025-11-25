import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient
from accounts.models import Profile, OTPRequest
from menus.models import Merchant, MenuItem, Category, MerchantMember
from orders.models import Order, OrderItem
from django.utils import timezone
from datetime import timedelta

User = get_user_model()


@pytest.fixture
def api_client():
    """API client for making requests"""
    return APIClient()


@pytest.fixture
def customer_user(db):
    """Create a customer user with profile"""
    user = User.objects.create_user(
        username="customer1",
        email="customer1@test.com",
        password="testpass123"
    )
    Profile.objects.create(user=user, role="customer")
    return user


@pytest.fixture
def merchant_user(db):
    """Create a merchant user with profile"""
    user = User.objects.create_user(
        username="merchant1",
        email="merchant1@test.com",
        password="testpass123"
    )
    Profile.objects.create(user=user, role="merchant")
    return user


@pytest.fixture
def shipper_user(db):
    """Create a shipper user with profile"""
    user = User.objects.create_user(
        username="shipper1",
        email="shipper1@test.com",
        password="testpass123"
    )
    Profile.objects.create(user=user, role="shipper")
    return user


@pytest.fixture
def admin_user(db):
    """Create an admin user with profile"""
    user = User.objects.create_user(
        username="admin1",
        email="admin1@test.com",
        password="testpass123",
        is_staff=True,
        is_superuser=True
    )
    Profile.objects.create(user=user, role="admin")
    return user


@pytest.fixture
def authenticated_customer_client(api_client, customer_user):
    """API client authenticated as customer"""
    api_client.force_authenticate(user=customer_user)
    return api_client


@pytest.fixture
def authenticated_merchant_client(api_client, merchant_user):
    """API client authenticated as merchant"""
    api_client.force_authenticate(user=merchant_user)
    return api_client


@pytest.fixture
def authenticated_shipper_client(api_client, shipper_user):
    """API client authenticated as shipper"""
    api_client.force_authenticate(user=shipper_user)
    return api_client


@pytest.fixture
def merchant(db, merchant_user):
    """Create a merchant"""
    return Merchant.objects.create(
        owner=merchant_user,
        name="Test Restaurant",
        address="123 Test St",
        phone="0123456789",
        is_active=True
    )


@pytest.fixture
def category(db, merchant):
    """Create a category"""
    return Category.objects.create(
        merchant=merchant,
        name="Main Course",
        description="Main dishes"
    )


@pytest.fixture
def menu_item(db, merchant, category):
    """Create a menu item"""
    return MenuItem.objects.create(
        merchant=merchant,
        category=category,
        name="Test Burger",
        description="A delicious burger",
        price=10.00,
        stock=100,
        is_available=True
    )


@pytest.fixture
def order(db, customer_user, merchant, menu_item):
    """Create an order"""
    order = Order.objects.create(
        customer=customer_user,
        merchant=merchant,
        status=Order.Status.PENDING,
        payment_status=Order.PaymentStatus.UNPAID,
        delivery_address="123 Delivery St",
        total_amount=20.00
    )
    OrderItem.objects.create(
        order=order,
        menu_item=menu_item,
        name_snapshot=menu_item.name,
        price_snapshot=menu_item.price,
        quantity=2,
        line_total=20.00
    )
    return order


@pytest.fixture
def otp_request(db):
    """Create a valid OTP request"""
    return OTPRequest.objects.create(
        identifier="test@example.com",
        code="123456",
        purpose=OTPRequest.PURPOSE_REGISTER,
        expires_at=timezone.now() + timedelta(minutes=5),
        used=False
    )
