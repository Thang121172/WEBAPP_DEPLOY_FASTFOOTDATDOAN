#!/usr/bin/env python3
import os, sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
import django
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile

User = get_user_model()

TEST_USERS = [
    ("customer1", "customer1@example.com", "Password123", "customer"),
    ("merchant1", "merchant1@example.com", "Password123", "merchant"),
    ("shipper1", "shipper1@example.com", "Password123", "shipper"),
    ("admin1", "admin1@example.com", "Password123", "admin"),
]

def create_or_update_user(username, email, password, role):
    if not User.objects.filter(username=username).exists():
        if role == 'admin':
            User.objects.create_superuser(username, email=email, password=password)
            user = User.objects.get(username=username)
        else:
            user = User.objects.create_user(username, email=email, password=password, is_active=True)
        profile, created = Profile.objects.get_or_create(user=user, defaults={'role': role})
        if not created and profile.role != role:
            profile.role = role
            profile.save()
        print(f'created {username} ({role})')
    else:
        user = User.objects.get(username=username)
        # Đảm bảo user đã tồn tại cũng được kích hoạt
        if not user.is_active:
            user.is_active = True
            user.save(update_fields=['is_active'])
            print(f'  [Updated] Activated user: {username}')
        profile, created = Profile.objects.get_or_create(user=user, defaults={'role': role})
        if not created and profile.role != role:
            profile.role = role
            profile.save()
        print(f'{username} exists ({profile.role})')


def main():
    for u, e, p, r in TEST_USERS:
        create_or_update_user(u, e, p, r)
    print('done')


if __name__ == '__main__':
    main()
