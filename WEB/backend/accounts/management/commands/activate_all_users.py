"""
Django management command để kích hoạt tất cả tài khoản user trong database
Chạy: python manage.py activate_all_users
"""
from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model

User = get_user_model()


class Command(BaseCommand):
    help = 'Kích hoạt tất cả tài khoản user trong database (set is_active=True)'

    def handle(self, *args, **options):
        self.stdout.write(self.style.WARNING('=' * 60))
        self.stdout.write(self.style.WARNING('KICH HOAT TAT CA TAI KHOAN USER'))
        self.stdout.write(self.style.WARNING('=' * 60))
        self.stdout.write('')

        # Tìm tất cả user chưa active
        inactive_users = User.objects.filter(is_active=False)
        count = inactive_users.count()
        
        if count == 0:
            self.stdout.write(self.style.SUCCESS('✓ Không có tài khoản nào chưa kích hoạt!'))
        else:
            self.stdout.write(self.style.WARNING(f'📋 Tìm thấy {count} tài khoản chưa kích hoạt'))
            self.stdout.write('')
            
            activated_count = 0
            for user in inactive_users:
                self.stdout.write(f'  - {user.username} ({user.email})')
                user.is_active = True
                user.save(update_fields=['is_active'])
                activated_count += 1
                self.stdout.write(self.style.SUCCESS(f'    ✓ Đã kích hoạt!'))
            
            self.stdout.write('')
            self.stdout.write(self.style.SUCCESS(f'✓ Đã kích hoạt {activated_count} tài khoản'))
        
        self.stdout.write('')
        
        # Hiển thị tất cả user gần đây
        self.stdout.write('📋 Danh sách user gần đây (10 user cuối cùng):')
        recent_users = User.objects.all().order_by('-date_joined')[:10]
        for user in recent_users:
            status = self.style.SUCCESS('ACTIVE') if user.is_active else self.style.ERROR('INACTIVE')
            self.stdout.write(f'  - {user.username} ({user.email}) - {status}')
        
        self.stdout.write('')
        self.stdout.write(self.style.SUCCESS('=' * 60))
        self.stdout.write(self.style.SUCCESS('HOÀN TẤT!'))
        self.stdout.write(self.style.SUCCESS('=' * 60))

