import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import FormCard from '../components/FormCard';
import api from '../services/http';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // message hiển thị cho user
  const [message, setMessage] = useState<{
    text: string;
    type: 'success' | 'error' | 'info';
  }>({
    text: 'Nhập email đã đăng ký để nhận mã OTP đặt lại mật khẩu.',
    type: 'info',
  });

  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    const cleanEmail = (email || '').trim().toLowerCase();
    if (!cleanEmail) {
      setMessage({
        text: 'Vui lòng nhập email.',
        type: 'error',
      });
      setSubmitting(false);
      return;
    }

    try {
      // GỌI API gửi OTP quên mật khẩu
      // Backend: POST /api/accounts/forgot/request-otp/
      // body: { "email": "..." }
      const res = await api.post('/accounts/forgot/request-otp/', {
        email: cleanEmail,
      });

      // Backend luôn trả 200:
      // {
      //   "detail": "Nếu email tồn tại, OTP khôi phục mật khẩu đã được gửi.",
      //   "expires_at": "...",
      //   "debug_otp": "123456" (chỉ trong DEBUG khi Celery fail)
      // }

      const detail =
        res.data?.detail ||
        'Nếu email tồn tại, OTP khôi phục mật khẩu đã được gửi.';

      // điều hướng tới màn nhập OTP + mật khẩu mới
      navigate(
        `/verify-otp?mode=reset&email=${encodeURIComponent(cleanEmail)}`,
        {
          state: {
            successMessage: detail,
          },
        }
      );
    } catch (err: any) {
      console.error('Lỗi gửi OTP reset password:', err);

      const apiErr = err?.response?.data;
      let msg = 'Không thể gửi OTP. Vui lòng thử lại sau.';

      if (apiErr) {
        if (typeof apiErr === 'string') {
          msg = apiErr;
        } else if (apiErr.detail) {
          msg = apiErr.detail;
        }
      }

      setMessage({
        text: msg,
        type: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const messageClasses =
    message.type === 'success'
      ? 'bg-grabGreen-100 text-grabGreen-800'
      : message.type === 'error'
      ? 'bg-red-100 text-red-800'
      : 'bg-blue-100 text-blue-800';

  return (
    <div className="flex justify-center items-center min-h-screen bg-gray-50 p-4">
      <FormCard title="Quên mật khẩu">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Thông báo */}
          {message.text && (
            <div
              className={`p-3 rounded-lg text-sm font-medium ${messageClasses}`}
            >
              {message.text}
            </div>
          )}

          {/* Email */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email đã đăng ký
            </label>
            <input
              type="email"
              value={email}
              required
              onChange={(e) => setEmail(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 transition duration-150"
              placeholder="email@domain.com"
            />
          </div>

          {/* Gửi OTP */}
          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3 rounded-lg font-bold text-white transition duration-200 shadow-md bg-grabGreen-700 hover:bg-grabGreen-800 disabled:opacity-60"
          >
            {submitting ? 'Đang gửi mã OTP...' : 'Gửi mã OTP đặt lại mật khẩu'}
          </button>
        </form>

        <div className="mt-6 text-center text-sm">
          <Link
            to="/login"
            className="text-grabGreen-700 font-semibold hover:underline"
          >
            Quay lại đăng nhập
          </Link>
        </div>
      </FormCard>
    </div>
  );
}
