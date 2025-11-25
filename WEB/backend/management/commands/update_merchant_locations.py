from django.core.management.base import BaseCommand
from menus.models import Merchant

# Vị trí của bạn
CUSTOMER_LAT = 11.318067
CUSTOMER_LNG = 106.050355

# Tọa độ mới cho các merchants (gần vị trí của bạn)
MERCHANT_LOCATIONS = {
    'quancom_bienhoa': {'lat': 11.320000, 'lng': 106.052000},  # ~300m
    'pizza_bienhoa': {'lat': 11.316000, 'lng': 106.048000},  # ~500m
    'bunthitnuong_bienhoa': {'lat': 11.322000, 'lng': 106.053000},  # ~600m
    'pho_bienhoa': {'lat': 11.314000, 'lng': 106.046000},  # ~800m
    'banhmi_bienhoa': {'lat': 11.325000, 'lng': 106.055000},  # ~1km
    'comtam_bienhoa': {'lat': 11.312000, 'lng': 106.044000},  # ~1.2km
    'cafe_bienhoa': {'lat': 11.319000, 'lng': 106.051000},  # ~200m
    'chicken_bienhoa': {'lat': 11.321000, 'lng': 106.054000},  # ~700m
}


class Command(BaseCommand):
    help = 'Update merchant locations to be near your current location'

    def handle(self, *args, **options):
        self.stdout.write('Updating merchant locations...')
        
        updated_count = 0
        for username, coords in MERCHANT_LOCATIONS.items():
            try:
                merchant = Merchant.objects.get(owner__username=username)
                merchant.latitude = coords['lat']
                merchant.longitude = coords['lng']
                merchant.save()
                self.stdout.write(self.style.SUCCESS(f'✓ Updated {merchant.name}: {coords["lat"]}, {coords["lng"]}'))
                updated_count += 1
            except Merchant.DoesNotExist:
                self.stdout.write(self.style.WARNING(f'  Merchant not found: {username}'))
            except Exception as e:
                self.stdout.write(self.style.ERROR(f'  Error updating {username}: {e}'))
        
        self.stdout.write(self.style.SUCCESS(f'\n✓ Updated {updated_count} merchants'))
        self.stdout.write(f'Your location: {CUSTOMER_LAT}, {CUSTOMER_LNG}')

