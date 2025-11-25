import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../services/http';
import { useAuthContext } from '../../context/AuthContext';

export default function RegisterStore() {
  const { user, login } = useAuthContext();
  const navigate = useNavigate();

  // Thông tin cửa hàng (khớp serializer backend RegisterMerchantSerializer)
  const [storeName, setStoreName] = useState('');
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');

  // UI state
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Nếu user đã là merchant → báo và gợi ý đi dashboard
  if (user && user.role === 'merchant') {
    return (
      <div className="flex justify-center items-center h-screen bg-gray-50 p-4">
        <div className="max-w-md w-full p-8 space-y-4 bg-white rounded-xl shadow-2xl border-t-4 border-yellow-500 text-center">
          <h2 className="text-2xl font-bold text-yellow-700">
            Bạn đã là Đối tác Cửa hàng
          </h2>
          <p className="text-gray-600">
            Bạn có thể quản lý cửa hàng của mình tại Dashboard.
          </p>
          <Link
            to="/merchant/dashboard"
            className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-full shadow-sm text-white bg-yellow-600 hover:bg-yellow-700 transition duration-150"
          >
            Đi đến Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!user) {
      // Giữ UX hiện tại của bạn: yêu cầu đăng nhập trước
      setError('Vui lòng đăng nhập trước khi đăng ký cửa hàng.');
      return;
    }

    setSubmitting(true);

    try {
      // Gọi API backend thật:
      // RegisterMerchantView trong backend nhận (nếu user đã login):
      // { name, address?, phone? }
      const payload = {
        name: storeName,
        address: address,
        phone: phone,
      };

      const res = await api.post('/accounts/register_merchant/', payload);

      // Backend dự kiến trả:
      // {
      //   "user": {
      //      "id": ..., "username": ..., "email": ..., "role": "merchant"
      //   },
      //   "merchant": {"id": ..., "name": ...},
      //   "access": "jwt-access-token",
      //   "refresh": "jwt-refresh-token"
      // }
      const data = res.data || {};

      // Cập nhật AuthContext nếu có token và user
      if (data.user && data.access) {
        const mappedUser = {
          id: String(data.user.id ?? ''),
          email: data.user.email ?? '',
          role: (data.user.role ?? 'merchant') as
            | 'customer'
            | 'merchant'
            | 'shipper'
            | 'admin',
          name: data.user.username ?? data.user.email ?? 'Merchant',
        };

        login(data.access, mappedUser);
      }

      setSuccess(
        `Đăng ký cửa hàng thành công: ${data.merchant?.name || storeName}`
      );

      // Điều hướng sang dashboard merchant sau khi thành công
      navigate('/merchant/dashboard');
    } catch (err: any) {
      console.error('Store registration failed:', err);
      const errorMessage =
        err?.response?.data?.detail ||
        err?.response?.data?.username ||
        err?.response?.data?.email ||
        err?.response?.data?.message ||
        'Lỗi kết nối hoặc cửa hàng đã tồn tại.';
      setError(
        Array.isArray(errorMessage) ? errorMessage.join(', ') : errorMessage
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 p-4">
      <div className="w-full max-w-lg p-8 space-y-6 bg-white rounded-xl shadow-2xl border-t-4 border-grabGreen-700">
        <h2 className="text-3xl font-bold text-center text-gray-900">
          Đăng ký trở thành Đối tác Cửa hàng
        </h2>
        <p className="text-center text-gray-500">
          Mở rộng kinh doanh của bạn ngay hôm nay!
        </p>

        <form className="space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="p-3 text-sm font-medium text-red-700 bg-red-100 rounded-lg">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 text-sm font-medium text-grabGreen-700 bg-grabGreen-100 rounded-lg">
              {success}
            </div>
          )}

          {/* Tên cửa hàng */}
          <div>
            <label
              className="block text-sm font-medium text-gray-700 mb-1"
              htmlFor="storeName"
            >
              Tên Cửa hàng
            </label>
            <input
              id="storeName"
              type="text"
              value={storeName}
              onChange={(e) => setStoreName(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
              placeholder="Ví dụ: Cơm Tấm Sài Gòn"
            />
          </div>

          {/* Địa chỉ cửa hàng */}
          <div>
            <label
              className="block text-sm font-medium text-gray-700 mb-1"
              htmlFor="address"
            >
              Địa chỉ Cửa hàng
            </label>
            <input
              id="address"
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
              placeholder="Ví dụ: 123 Đường A, Phường B, Quận C"
            />
          </div>

          {/* Số điện thoại liên hệ (match backend 'phone') */}
          <div>
            <label
              className="block text-sm font-medium text-gray-700 mb-1"
              htmlFor="phone"
            >
              Số điện thoại liên hệ
            </label>
            <input
              id="phone"
              type="text"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
              placeholder="0912 345 678"
            />
            <p className="text-[11px] text-gray-400 mt-1">
              Số này sẽ hiển thị cho shipper khi nhận đơn.
            </p>
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="w-full px-4 py-3 text-lg text-white bg-grabGreen-700 rounded-full font-semibold hover:bg-grabGreen-800 transition duration-150 shadow-lg disabled:opacity-60"
            disabled={submitting || !user}
          >
            {user
              ? submitting
                ? 'Đang gửi đăng ký...'
                : 'Hoàn tất Đăng ký Merchant'
              : 'Vui lòng Đăng nhập'}
          </button>
        </form>
      </div>
    </div>
  );
}
