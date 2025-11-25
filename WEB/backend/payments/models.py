# backend/payments/models.py

from django.db import models
from django.conf import settings
from django.contrib.auth import get_user_model 

# QUAN TRỌNG: Sử dụng get_user_model()
User = get_user_model() 

class Payment(models.Model):
    # Sử dụng User model đã được lấy thông qua get_user_model()
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='payments') 
    
    amount = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    payment_method = models.CharField(max_length=50, default='Cash') # Ví dụ: 'VNPAY', 'MOMO', 'Cash'
    transaction_id = models.CharField(max_length=100, unique=True, null=True, blank=True)
    status = models.CharField(max_length=20, default='Pending') # Ví dụ: 'Completed', 'Failed', 'Pending'
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "Payment"
        verbose_name_plural = "Payments"

    def __str__(self):
        return f"Payment {self.id} - {self.user.email} - {self.amount}"