# backend/orders/apps.py

from django.apps import AppConfig


class OrdersConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    
    # SỬA LỖI: Tên (name) phải là đường dẫn module đầy đủ
    name = "orders"
    verbose_name = "Orders / Checkout / Delivery Flow"

    def ready(self):
        """
        Hook chạy khi app 'orders' được load.
        """
        pass