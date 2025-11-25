# backend/core/middleware/request_logger.py

from django.conf import settings

class RequestLoggerMiddleware:
    """
    Debug middleware cho môi trường phát triển.
    - Chỉ chạy nếu DEBUG=True.
    - Nếu request.path bắt đầu bằng /api/accounts/ (đăng ký, đăng nhập, verify OTP...)
      thì in ra method, path và body (tối đa 1000 ký tự).

    Lưu ý:
    - Dùng print() để log ra stdout => bạn sẽ xem được bằng `docker compose logs backend`.
    - Không dùng trong production vì có thể lộ thông tin nhạy cảm.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if settings.DEBUG and request.path.startswith("/api/accounts/"):
            try:
                body_preview = request.body.decode("utf-8", errors="replace")[:1000]
                print(f"[REQ][{request.method}] {request.path} :: {body_preview}")
            except Exception:
                # không để middleware giết request nếu lỗi decode
                pass

        return self.get_response(request)
