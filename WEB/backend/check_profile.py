import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
django.setup()

from django.contrib.auth import get_user_model
from accounts.models import Profile

User = get_user_model()
u = User.objects.get(email='hoangminhthang12a15@gmail.com')
print(f'User: {u.email}')

try:
    p = u.profile
    print(f'Profile exists: role={p.role}')
except Profile.DoesNotExist:
    print('Profile does not exist, creating...')
    Profile.objects.create(user=u, role='customer')
    print('Profile created!')

