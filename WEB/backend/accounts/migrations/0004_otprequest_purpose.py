# Generated migration to add purpose field to OTPRequest

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0003_alter_profile_default_address_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='otprequest',
            name='purpose',
            field=models.CharField(
                choices=[('đăng ký tài khoản', 'Đăng ký'), ('khôi phục mật khẩu', 'Khôi phục mật khẩu')],
                default='đăng ký tài khoản',
                help_text='Mục đích sử dụng của mã OTP này',
                max_length=50,
                verbose_name='Mục đích'
            ),
        ),
    ]

