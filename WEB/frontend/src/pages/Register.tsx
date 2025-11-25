import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/http';

// NOTE: Flow OTP
// Step 1 (trang này):
//   POST /api/accounts/register/request-otp/
//   body: { email, password, role }
//   -> backend tạo OTP, gửi mail (hoặc trả debug_otp nếu DEBUG)
//   -> FE lưu tạm email/password/role vào localStorage.pendingRegister
//   -> navigate('/verify-otp?mode=register&email=...')
// Step 2 (VerifyOTP page):
//   POST /api/accounts/register/confirm/
//   body: { email, otp, password, role }
//   -> backend tạo user + profile + trả tokens

export default function Register() {
  const navigate = useNavigate();

  // form state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'customer' | 'merchant' | 'shipper'>(
    'customer'
  );

  // ui state
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setInfo('');
    setSubmitting(true);

    try {
      const normalizedEmail = email.trim().toLowerCase();

      // gọi bước 1: xin OTP
      const resp = await api.post('/accounts/register/request-otp/', {
        email: normalizedEmail,
        password,
        role,
      });

      // lưu tạm dữ liệu cần cho bước confirm OTP
      localStorage.setItem(
        'pendingRegister',
        JSON.stringify({
          email: normalizedEmail,
          password,
          role,
        })
      );

      // backend có thể trả debug_otp khi DEBUG và celery chưa chạy
      if (resp.data?.debug_otp) {
        setInfo(
          `OTP (dev) là ${resp.data.debug_otp}. Nhập OTP ở màn hình tiếp theo.`
        );
      } else {
        setInfo(
          'OTP đã được gửi tới email. Vui lòng kiểm tra hộp thư và nhập mã OTP ở bước tiếp theo.'
        );
      }

      // chuyển sang màn hình nhập OTP, kèm query để VerifyOTP biết đang ở flow register
      navigate(
        `/verify-otp?mode=register&email=${encodeURIComponent(normalizedEmail)}`
      );
    } catch (err: any) {
      console.error('request-otp failed:', err);

      const apiErr = err?.response?.data;
      if (apiErr) {
        if (typeof apiErr === 'string') {
          setError(apiErr);
        } else if (apiErr.detail) {
          setError(apiErr.detail);
        } else if (apiErr.email) {
          setError(
            Array.isArray(apiErr.email)
              ? apiErr.email.join(', ')
              : apiErr.email
          );
        } else {
          setError('Không thể gửi OTP. Vui lòng thử lại.');
        }
      } else {
        setError('Lỗi kết nối máy chủ.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 p-4">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-xl shadow-2xl border-t-4 border-grabGreen-700">
        <h2 className="text-3xl font-bold text-center text-gray-900">
          Đăng ký Tài khoản
        </h2>
        <p className="text-center text-gray-500 text-sm">
          Nhập email & mật khẩu → nhận OTP xác thực 📩
        </p>

        {error && (
          <div className="p-3 text-sm font-medium text-red-700 bg-red-100 rounded-lg text-center">
            {error}
          </div>
        )}

        {info && (
          <div className="p-3 text-sm font-medium text-blue-700 bg-blue-100 rounded-lg text-center">
            {info}
          </div>
        )}

        <form className="space-y-4" onSubmit={handleSubmit}>
          {/* Email */}
          <div>
            <label
              className="block text-sm font-medium text-gray-700 mb-1"
              htmlFor="email"
            >
              Email *
            </label>
            <input
              id="email"
              type="email"
              value={email}
              required
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
              placeholder="you@example.com"
            />
            <p className="text-xs text-gray-500 mt-1">
              Email này cũng sẽ là username để đăng nhập.
            </p>
          </div>

          {/* Password */}
          <div>
            <label
              className="block text-sm font-medium text-gray-700 mb-1"
              htmlFor="password"
            >
              Mật khẩu *
            </label>
            <input
              id="password"
              type="password"
              value={password}
              required
              minLength={6}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
              placeholder="Tối thiểu 6 ký tự"
            />
          </div>

          {/* Role */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Bạn là *
            </label>
            <select
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 text-sm"
              value={role}
              onChange={(e) =>
                setRole(
                  e.target.value as 'customer' | 'merchant' | 'shipper'
                )
              }
            >
              <option value="customer">Khách đặt đồ ăn</option>
              <option value="merchant">Chủ quán / Merchant</option>
              <option value="shipper">Shipper giao hàng</option>
            </select>
            <p className="text-xs text-gray-500 mt-1">
              Có thể đổi quyền sau (ví dụ đăng ký quán).
            </p>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={submitting}
            className={`w-full px-4 py-2 text-white rounded-full font-semibold shadow-md transition duration-150 ${
              submitting
                ? 'bg-gray-400 cursor-not-allowed'
                : 'bg-grabGreen-700 hover:bg-grabGreen-800'
            }`}
          >
            {submitting ? 'Đang gửi OTP...' : 'Gửi OTP đăng ký'}
          </button>

          <div className="text-center text-sm mt-3">
            Đã có tài khoản?{' '}
            <Link
              to="/login"
              className="font-medium text-grabGreen-700 hover:text-grabGreen-800"
            >
              Đăng nhập ngay
            </Link>
          </div>
        </form>

        <div className="text-center text-[11px] text-gray-400">
          Quên mật khẩu?{' '}
          <Link
            to="/forgot"
            className="text-gray-500 hover:text-gray-700 underline"
          >
            Lấy lại bằng OTP
          </Link>
        </div>

        <div className="text-center text-[11px] text-gray-400">
          Khi nhấn "Gửi OTP đăng ký", bạn đồng ý với Điều khoản dịch vụ và
          Chính sách bảo mật.
        </div>
      </div>
    </div>
  );
}
