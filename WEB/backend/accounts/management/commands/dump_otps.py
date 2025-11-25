from django.core.management.base import BaseCommand
from accounts.models import OTPRequest
import json


class Command(BaseCommand):
    help = 'Dump recent OTPRequest rows as JSON for debugging'

    def add_arguments(self, parser):
        parser.add_argument('--identifier', help='Filter by identifier (email/phone)')
        parser.add_argument('--limit', type=int, default=50, help='How many rows to show')

    def handle(self, *args, **options):
        identifier = options.get('identifier')
        limit = options.get('limit') or 50
        qs = OTPRequest.objects.order_by('-created_at')
        if identifier:
            qs = qs.filter(identifier=identifier)
        qs = qs[:limit]
        out = []
        for o in qs:
            out.append({
                'id': str(o.id),
                'identifier': o.identifier,
                'code': o.code,
                'created_at': o.created_at.isoformat(),
                'expires_at': o.expires_at.isoformat(),
                'used': o.used,
            })
        self.stdout.write(json.dumps(out, indent=2, ensure_ascii=False))
