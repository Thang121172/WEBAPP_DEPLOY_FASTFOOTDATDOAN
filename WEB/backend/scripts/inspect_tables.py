import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','core.settings.dev')
import django
django.setup()
from django.db import connection
cur=connection.cursor()
for table in ('accounts_user','auth_user'):
    cur.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name=%s", [table])
    rows=cur.fetchall()
    print(f"\n{table} columns:")
    for r in rows:
        print(r)
