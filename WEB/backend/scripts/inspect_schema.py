import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','core.settings.dev')
import django
django.setup()
from django.db import connection

def cols(table):
    cur = connection.cursor()
    cur.execute("SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name=%s ORDER BY ordinal_position", [table])
    return cur.fetchall()

tables = ['accounts_user','auth_user','accounts_user_groups','accounts_user_user_permissions','auth_user_groups','auth_user_user_permissions']
for t in tables:
    try:
        c = cols(t)
        print('\nTABLE:', t)
        if c:
            for col in c:
                print('  ', col)
        else:
            print('  (no columns / table missing)')
    except Exception as e:
        print('\nTABLE:', t, ' ERROR:', e)
