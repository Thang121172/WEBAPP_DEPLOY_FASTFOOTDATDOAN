# backend/accounts/utils.py

import random
import string
from datetime import timedelta

from django.conf import settings
from django.core.mail import send_mail
from django.utils import timezone

from .models import OTPRequest


def generate_otp_code(length=6):
    """
    Tạo mã OTP numeric, ví dụ '483920'
    length mặc định = 6.
    """
    digits = string.digits  # "0123456789"
    return "".join(random.choice(digits) for _ in range(length))


def create_and_send_otp(identifier: str, purpose: str, ttl_minutes: int = 5):
    """
    - identifier: thường là email user (vd: "user@gmail.com")
    - purpose: text mô tả ('đăng ký tài khoản', 'khôi phục mật khẩu', ...)
      -> dùng trong nội dung email cho dễ hiểu
    - ttl_minutes: thời gian sống của OTP (mặc định 5 phút)

    1. tạo OTPRequest record (code, expires_at)
    2. gửi OTP qua email với send_mail()
    3. trả về instance để debug nếu cần
    """

    code = generate_otp_code(6)

    now = timezone.now()
    expires_at = now + timedelta(minutes=ttl_minutes)

    otp_obj = OTPRequest.objects.create(
        identifier=identifier,
        code=code,
        created_at=now,
        expires_at=expires_at,
        used=False,
    )

    # Gửi email
    subject = f"Mã OTP {purpose} của bạn"
    message = (
        f"Xin chào,\n\n"
        f"Mã OTP để {purpose} là: {code}\n"
        f"Mã này sẽ hết hạn lúc {expires_at.strftime('%H:%M:%S %d/%m/%Y')}.\n\n"
        f"Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email này.\n\n"
        f"FastFood"
    )

    from_email = settings.DEFAULT_FROM_EMAIL or "no-reply@example.com"
    recipient_list = [identifier]

    # NOTE:
    # Trong dev, nếu bạn không muốn gửi mail thật,
    # bạn có thể chuyển EMAIL_BACKEND trong settings.dev
    # sang console backend để in OTP ra terminal thay vì gửi Gmail.
    #
    # EMAIL_BACKEND = "django.core.mail.backends.console.EmailBackend"
    #
    # Lúc đó otp vẫn được tạo trong DB, còn mã OTP sẽ hiện trong logs.

    try:
        send_mail(
            subject=subject,
            message=message,
            from_email=from_email,
            recipient_list=recipient_list,
            fail_silently=False,
        )
    except Exception as e:
        # Trong môi trường dev đôi khi SMTP fail. Ta vẫn trả OTP object,
        # để FE/dev có thể debug. Trong production thì nên log lỗi.
        print("⚠️ send_mail failed:", e)

    return otp_obj
