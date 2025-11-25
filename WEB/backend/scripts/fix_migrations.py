"""
Script để xóa migration record và chạy lại migrations cho orders app
"""
import os
import sys
from pathlib import Path

# Setup Django
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')

import django
django.setup()

from django.db import connection

def delete_migration_records(app_name='orders'):
    """Xóa migration records của app khỏi django_migrations table"""
    with connection.cursor() as cursor:
        # Kiểm tra xem có migration records nào không
        cursor.execute(
            "SELECT id, app, name FROM django_migrations WHERE app = %s ORDER BY id",
            [app_name]
        )
        records = cursor.fetchall()
        
        if not records:
            print(f"Không tìm thấy migration records nào cho app '{app_name}'")
            return
        
        print(f"\nTìm thấy {len(records)} migration record(s) cho app '{app_name}':")
        for record in records:
            print(f"  - ID: {record[0]}, App: {record[1]}, Name: {record[2]}")
        
        # Xác nhận xóa
        print(f"\nĐang xóa migration records cho app '{app_name}'...")
        cursor.execute(
            "DELETE FROM django_migrations WHERE app = %s",
            [app_name]
        )
        deleted_count = cursor.rowcount
        print(f"✅ Đã xóa {deleted_count} migration record(s)")
        
        # Commit transaction
        connection.commit()

def show_all_migrations():
    """Hiển thị tất cả migration records"""
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT id, app, name FROM django_migrations ORDER BY app, id"
        )
        records = cursor.fetchall()
        
        if not records:
            print("Không có migration records nào trong database")
            return
        
        print(f"\nTất cả migration records trong database ({len(records)} records):")
        current_app = None
        for record in records:
            app, name = record[1], record[2]
            if app != current_app:
                current_app = app
                print(f"\n  [{app}]")
            print(f"    - {name}")

if __name__ == '__main__':
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == '--show':
        show_all_migrations()
    else:
        print("=" * 60)
        print("Script xóa migration records và chạy lại migrations")
        print("=" * 60)
        
        # Hiển thị migrations hiện tại
        show_all_migrations()
        
        # Xóa migration records cho orders app
        delete_migration_records('orders')
        
        print("\n" + "=" * 60)
        print("✅ Hoàn thành! Bây giờ bạn có thể chạy:")
        print("   python manage.py migrate orders")
        print("=" * 60)

