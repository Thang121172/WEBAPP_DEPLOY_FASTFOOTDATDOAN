import os
import sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','core.settings.dev')
import django
django.setup()
from django.db import connection
import sys
app = sys.argv[1] if len(sys.argv) > 1 else None
cur=connection.cursor()
if app:
    cur.execute("select id, app, name from django_migrations where app=%s order by id", [app])
else:
    cur.execute('select id, app, name from django_migrations order by id')
rows=cur.fetchall()
for r in rows:
    print(r)
