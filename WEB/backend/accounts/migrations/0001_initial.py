# accounts/migrations/0001_initial.py
from django.db import migrations, models
import uuid


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        # không phụ thuộc AUTH_USER_MODEL ở đây vì chỉ tạo OTPRequest
    ]

    operations = [
        migrations.CreateModel(
            name='OTPRequest',
            fields=[
                ('id', models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ('identifier', models.CharField(max_length=255, help_text='Email (hoặc phone sau này) dùng để nhận OTP')),
                ('code', models.CharField(max_length=8, help_text='Mã OTP, ví dụ 6 chữ số')),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('expires_at', models.DateTimeField(help_text='Mã OTP hết hạn sau thời điểm này')),
                ('used', models.BooleanField(default=False, help_text='Đã dùng OTP này để verify chưa')),
            ],
        ),
    ]
