# backend/accounts/tasks.py

from celery import shared_task
from django.core.mail import send_mail
from django.conf import settings
import logging

logger = logging.getLogger(__name__)

# Lấy TTL (Time-To-Live) từ settings (dùng để hiển thị trong email)
OTP_TTL_MINUTES = getattr(settings, 'OTP_TTL_MINUTES', 5)


@shared_task(bind=True, max_retries=3)
def send_otp_email(self, email: str, code: str, purpose: str): # ĐÃ THÊM THAM SỐ 'purpose'
    """
    Gửi OTP qua email cho user, phân biệt theo mục đích (purpose).
    Chạy trong celery_worker container.
    """
    # Điều chỉnh Subject và Message để bao gồm Purpose
    subject = f"Mã xác thực {purpose} của bạn | FastFood App"
    
    message = (
        f"Xin chào,\n\n"
        f"Mã OTP xác thực {purpose} của bạn là: {code}\n"
        f"Mã này sẽ hết hạn sau {OTP_TTL_MINUTES} phút.\n\n"
        f"Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.\n\n"
        f"Trân trọng!"
    )

    # Sử dụng DEFAULT_FROM_EMAIL đã cấu hình trong settings
    from_email = getattr(settings, "DEFAULT_FROM_EMAIL", settings.EMAIL_HOST_USER)
    recipient_list = [email]

    # Gửi email. Nếu lỗi tạm thời (SMTP refuse...), Celery có thể retry.
    try:
        send_mail(
            subject,
            message,
            from_email,
            recipient_list,
            fail_silently=False,
        )
        logger.info(f"Successfully sent OTP for {purpose} to {email}")
        return {"status": "sent", "to": email, "purpose": purpose}
    except Exception as exc:
        logger.error(f"Failed to send email OTP to {email} (Attempt {self.request.retries + 1}): {exc}")
        # Celery retry (với countdown 5 giây)
        raise self.retry(exc=exc, countdown=5)
