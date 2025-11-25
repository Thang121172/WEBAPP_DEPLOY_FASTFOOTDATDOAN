# accounts/migrations/0002_profile.py

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0001_initial'),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='Profile',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),

                ('role', models.CharField(
                    max_length=20,
                    choices=[
                        ('customer', 'Customer'),
                        ('merchant', 'Merchant'),
                        ('shipper', 'Shipper'),
                        ('admin', 'Admin'),
                    ],
                    default='customer',
                    help_text='Phân loại tài khoản để hạn chế endpoint phù hợp',
                )),

                ('default_address', models.CharField(
                    max_length=255,
                    blank=True,
                    null=True,
                    help_text='Địa chỉ giao hàng mặc định (dùng cho customer)',
                )),

                ('store_name', models.CharField(
                    max_length=150,
                    blank=True,
                    null=True,
                    help_text='Tên cửa hàng/quán (dùng cho merchant)',
                )),

                ('store_address', models.CharField(
                    max_length=255,
                    blank=True,
                    null=True,
                    help_text='Địa chỉ cửa hàng/quán (dùng cho merchant)',
                )),

                ('vehicle_plate', models.CharField(
                    max_length=32,
                    blank=True,
                    null=True,
                    help_text='Biển số xe shipper',
                )),

                ('is_available', models.BooleanField(
                    default=True,
                    help_text='Shipper đang bật chế độ nhận đơn?',
                )),

                ('user', models.OneToOneField(
                    on_delete=django.db.models.deletion.CASCADE,
                    related_name='profile',
                    to=settings.AUTH_USER_MODEL,
                )),
            ],
        ),
    ]
