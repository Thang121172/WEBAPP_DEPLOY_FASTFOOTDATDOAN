import os
import sys
from django.core.wsgi import get_wsgi_application

# Lấy đường dẫn tuyệt đối của thư mục chứa wsgi.py (trên server là /app)
current_directory = os.path.dirname(os.path.abspath(__file__))

# Thêm thư mục hiện tại vào sys.path
if current_directory not in sys.path:
    sys.path.append(current_directory)

# **KIỂM TRA THÊM: Nếu dự án của bạn có thư mục gốc lồng, thêm thư mục cha.**
# (Ví dụ: nếu mọi thứ nằm trong /app/WEB_DACN/, thì thư mục cha của wsgi.py là /app)
parent_directory = os.path.dirname(current_directory)
if parent_directory not in sys.path:
    sys.path.append(parent_directory)

# Đảm bảo thư mục gốc (root) của dự án đã được thêm. 
# Việc này giải quyết lỗi ModuleNotFoundError cho 'backend' và 'orders'.
print(f"DEBUG: sys.path after fix: {sys.path}") # Dòng debug hữu ích

# Thiết lập biến môi trường
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'settings')

application = get_wsgi_application()