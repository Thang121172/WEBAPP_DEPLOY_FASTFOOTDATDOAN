import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import api from '../services/http';
import FormCard from '../components/FormCard';
import { useAuthContext } from '../context/AuthContext';

// NOTE: helper đọc query param
function useQuery() {
  return new URLSearchParams(window.location.search);
}

export default function VerifyOTP() {
  const nav = useNavigate();
  const location = useLocation();
  const { login } = useAuthContext();

  const query = useQuery();
  const urlMode = query.get('mode'); // 'reset' => quên mật khẩu, còn lại => đăng ký
  const urlEmail = query.get('email') || '';

  // =========================================
  // State
  // =========================================
  const [email, setEmail] = useState<string>('');
  const [otp, setOtp] = useState<string>('');
  const [newPassword, setNewPassword] = useState<string>(''); // chỉ dùng cho reset pass
  const [submitting, setSubmitting] = useState(false);

  const [message, setMessage] = useState<{
    text: string;
    type: 'success' | 'error' | 'info';
  }>({
    text: '',
    type: 'info',
  });

  // =========================================
  // Khi mount:
  //  - Nếu mode !== 'reset' => đây là flow đăng ký
  //    -> đọc info tạm trong localStorage.pendingRegister
  //
  //  - Nếu mode === 'reset' => flow quên mật khẩu
  //    -> dùng ?email=... trong URL (từ trang ForgotPassword)
  //
  // Đồng thời hiển thị thông điệp phù hợp
  // =========================================
  useEffect(() => {
    if (urlMode === 'reset') {
      // flow QUÊN MẬT KHẨU
      setEmail(urlEmail.toLowerCase());
      setMessage({
        text: urlEmail
          ? `Vui lòng kiểm tra mã OTP được gửi đến ${urlEmail}. Nhập OTP và mật khẩu mới để đặt lại mật khẩu.`
          : 'Vui lòng nhập email, OTP và mật khẩu mới.',
        type: 'info',
      });
    } else {
      // flow ĐĂNG KÝ
      try {
        const raw = localStorage.getItem('pendingRegister');
        if (raw) {
          const parsed = JSON.parse(raw);
          if (parsed?.email) {
            setEmail(String(parsed.email).toLowerCase());
            setMessage({
              text: `Chúng mình đã gửi mã OTP tới ${parsed.email}. Nhập OTP để kích hoạt tài khoản.`,
              type: 'info',
            });
          } else {
            setMessage({
              text: 'Thiếu thông tin đăng ký. Vui lòng đăng ký lại.',
              type: 'error',
            });
          }
        } else {
          setMessage({
            text: 'Thiếu thông tin đăng ký. Vui lòng đăng ký lại.',
            type: 'error',
          });
        }
      } catch (err) {
        setMessage({
          text: 'Không đọc được thông tin đăng ký tạm. Vui lòng đăng ký lại.',
          type: 'error',
        });
      }
    }

    // Nếu RegisterView hoặc ForgotPassword vừa push state.successMessage vào location
    if (
      location.state &&
      (location.state as any).successMessage &&
      typeof (location.state as any).successMessage === 'string'
    ) {
      setMessage({
        text: (location.state as any).successMessage,
        type: 'success',
      });
    }
  }, [urlMode, urlEmail, location.state]);

  // =========================================
  // Helper: gọi OTP debug để auto-fill OTP (dev only)
  // GET /api/accounts/otp/debug/
  // trả [{identifier, code, ...}, ...]
  // Mình lấy OTP đầu tiên khớp email hiện tại.
  // =========================================
  const fetchLatestOTPDev = async (e: React.MouseEvent) => {
    e.preventDefault();

    if (!email) {
      setMessage({
        text: 'Hãy nhập email trước khi lấy OTP DEV.',
        type: 'error',
      });
      return;
    }

    try {
      const res = await api.get('/accounts/otp/debug/');
      const list = Array.isArray(res.data) ? res.data : [];
      // tìm OTP có identifier == email hiện tại
      const match = list.find(
        (item: any) =>
          String(item.identifier || '').toLowerCase() ===
          String(email || '').toLowerCase()
      );
      if (match?.code) {
        setOtp(match.code);
        setMessage({
          text:
            'Đã tự động điền OTP từ /otp/debug (dev mode). Nếu lên production sẽ không có chức năng này.',
          type: 'success',
        });
      } else {
        setMessage({
          text: 'Không tìm thấy OTP phù hợp trong dev debug.',
          type: 'error',
        });
      }
    } catch (err) {
      console.error(err);
      setMessage({
        text: 'Không thể gọi /otp/debug/. Có thể DEBUG=False hoặc Celery chưa chạy.',
        type: 'error',
      });
    }
  };

  // =========================================
  // Submit handler
  //
  // Nếu mode === 'reset' => gọi reset password endpoint:
  //   POST /api/accounts/reset-password/confirm/
  //   { email, otp, new_password }
  //   -> nav('/login', successMessage=...)
  //
  // Nếu mode !== 'reset' => xác nhận đăng ký:
  //   localStorage.pendingRegister = { email, password, role }
  //   POST /api/accounts/register/confirm/
  //   { email, otp, password, role }
  //
  //   backend trả:
  //   {
  //     user_id,
  //     email,
  //     role,
  //     tokens: { access, refresh }
  //   }
  //
  //   -> login(accessToken, { id, email, role, name })
  //   -> clear pendingRegister
  //   -> nav('/')
  // =========================================
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setMessage({ text: '', type: 'info' });

    const cleanEmail = String(email || '').trim().toLowerCase();
    const cleanOTP = String(otp || '').trim();

    if (!cleanEmail) {
        setMessage({
          text: 'Thiếu email.',
          type: 'error',
        });
        setSubmitting(false);
        return;
    }

    if (!cleanOTP) {
        setMessage({
          text: 'Vui lòng nhập mã OTP.',
          type: 'error',
        });
        setSubmitting(false);
        return;
    }

    try {
      if (urlMode === 'reset') {
        // =======================
        // QUÊN MẬT KHẨU FLOW
        // =======================
        if (!newPassword) {
          setMessage({
            text: 'Vui lòng nhập mật khẩu mới.',
            type: 'error',
          });
          setSubmitting(false);
          return;
        }

        await api.post('/accounts/reset-password/confirm/', {
          email: cleanEmail,
          otp: cleanOTP,
          new_password: newPassword,
        });

        // nếu ok -> quay về login
        nav('/login', {
          state: {
            successMessage:
              'Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.',
          },
        });
        return;
      } else {
        // =======================
        // ĐĂNG KÝ FLOW
        // =======================
        // lấy data pendingRegister trong localStorage
        const raw = localStorage.getItem('pendingRegister');
        if (!raw) {
          setMessage({
            text: 'Không tìm thấy thông tin đăng ký tạm. Vui lòng đăng ký lại.',
            type: 'error',
          });
          setSubmitting(false);
          return;
        }

        const pending = JSON.parse(raw || '{}');
        const regPassword = pending.password || '';
        const regRole =
          pending.role ||
          'customer'; // fallback cho chắc nếu FE chưa set role
        const regEmail = String(pending.email || '').toLowerCase();

        if (!regPassword) {
          setMessage({
            text: 'Thiếu mật khẩu từ bước đăng ký. Vui lòng đăng ký lại.',
            type: 'error',
          });
          setSubmitting(false);
          return;
        }

        // gọi API confirm
        const confirmResp = await api.post(
          '/accounts/register/confirm/',
          {
            email: regEmail,
            otp: cleanOTP,
            password: regPassword,
            role: regRole,
          }
        );

        // confirmResp.data:
        // {
        //    user_id,
        //    email,
        //    role,
        //    tokens: { access, refresh }
        // }
        const data = confirmResp.data || {};
        const accessToken = data.tokens?.access;
        const refreshToken = data.tokens?.refresh;
        const newUserId = data.user_id;
        const newUserRole = data.role || 'customer';
        const newUserEmail = data.email || regEmail;

        if (!accessToken) {
          // Nếu vì lý do gì đó backend ko trả token,
          // cứ cho user quay vô /login
          nav('/login', {
            state: {
              successMessage:
                'Tạo tài khoản thành công. Vui lòng đăng nhập.',
            },
          });
          return;
        }

        // chuẩn hóa userData cho AuthContext
        const userData = {
          id: String(newUserId ?? ''),
          email: newUserEmail,
          role: newUserRole as 'customer' | 'merchant' | 'shipper' | 'admin',
          name: newUserEmail, // tạm dùng email làm "name"
        };

        // Lưu access token + user vào context
        login(accessToken, userData);

        // Nếu muốn lưu refresh token, bạn có thể thêm:
        if (refreshToken) {
          localStorage.setItem('refreshToken', refreshToken);
        }

        // clear pendingRegister để không reuse
        localStorage.removeItem('pendingRegister');

        // về trang chủ
        nav('/');
        return;
      }
    } catch (err: any) {
      console.error(err);
      // backend các serializer thường trả kiểu:
      // { otp: "OTP không hợp lệ" } hoặc { detail: "..."}
      const data = err?.response?.data;
      let msg = 'Có lỗi xảy ra. Vui lòng kiểm tra lại OTP / mật khẩu.';
      if (data) {
        if (typeof data === 'string') {
          msg = data;
        } else if (data.detail) {
          msg = data.detail;
        } else if (data.otp) {
          msg = Array.isArray(data.otp) ? data.otp.join(', ') : data.otp;
        } else if (data.email) {
          msg = Array.isArray(data.email) ? data.email.join(', ') : data.email;
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

  // UI màu thông báo
  const messageClasses =
    message.type === 'success'
      ? 'bg-grabGreen-100 text-grabGreen-800'
      : message.type === 'error'
      ? 'bg-red-100 text-red-800'
      : 'bg-blue-100 text-blue-800';

  const isResetMode = urlMode === 'reset';

  return (
    <div className="flex justify-center items-center min-h-screen bg-gray-50 p-4">
      <FormCard
        title={isResetMode ? 'Quên mật khẩu: Xác minh OTP' : 'Xác minh OTP đăng ký'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Thông báo */}
          {message.text && (
            <div
              className={`p-3 rounded-lg text-sm font-medium ${messageClasses}`}
            >
              {message.text}
            </div>
          )}

          {/* Email (readonly trong reset nếu có từ URL, editable nếu thiếu) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 transition duration-150"
              placeholder="email@domain.com"
              type="email"
              required
            />
          </div>

          {/* OTP code */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Mã OTP
            </label>
            <input
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 transition duration-150"
              placeholder="Nhập mã 6 chữ số"
              maxLength={6}
              required
            />
          </div>

          {/* New password: chỉ hiện nếu đang reset mật khẩu */}
          {isResetMode && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Mật khẩu mới
              </label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 transition duration-150"
                placeholder="Nhập mật khẩu mới"
                minLength={6}
                required
              />
            </div>
          )}

          {/* Nút hành động */}
          <div className="flex gap-3 pt-2">
            <button
              type="submit"
              disabled={submitting}
              className="flex-grow py-3 rounded-lg font-bold text-white transition duration-200 shadow-md bg-grabGreen-700 hover:bg-grabGreen-800 disabled:opacity-60"
            >
              {submitting
                ? isResetMode
                  ? 'Đang đặt lại...'
                  : 'Đang xác minh...'
                : isResetMode
                ? 'Xác nhận đặt lại mật khẩu'
                : 'Xác minh & tạo tài khoản'}
            </button>

            {/* Dev helper: auto điền OTP từ /otp/debug/ */}
            <button
              type="button"
              onClick={fetchLatestOTPDev}
              disabled={submitting}
              className="py-3 px-4 rounded-lg font-medium text-white transition duration-200 shadow-md bg-slate-500 hover:bg-slate-600 disabled:opacity-60 text-sm"
            >
              Lấy OTP (DEV)
            </button>
          </div>
        </form>

        <div className="mt-4 text-center text-sm">
          {isResetMode ? (
            <Link
              to="/forgot"
              className="text-grabGreen-700 font-semibold hover:underline"
            >
              Gửi lại OTP
            </Link>
          ) : (
            <Link
              to="/register"
              className="text-grabGreen-700 font-semibold hover:underline"
            >
              Quay lại đăng ký
            </Link>
          )}
        </div>
      </FormCard>
    </div>
  );
}
