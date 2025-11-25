from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    OrderViewSet,
    MerchantViewSet,
    ShipperViewSet,
    merchant_dashboard,   # ✅ thêm endpoint dashboard
)

# -------------------------------
# Đăng ký router cho các ViewSet
# -------------------------------
router = DefaultRouter()
router.register(r'orders', OrderViewSet, basename='order')
router.register(r'merchant', MerchantViewSet, basename='merchant')
router.register(r'shipper', ShipperViewSet, basename='shipper')

# -------------------------------
# URL patterns
# -------------------------------
urlpatterns = [
    path('', include(router.urls)),
    # ✅ endpoint dashboard cho Merchant
    path('merchant/dashboard/', merchant_dashboard, name='merchant-dashboard'),
]
