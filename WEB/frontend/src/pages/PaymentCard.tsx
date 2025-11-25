import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/http';
import { useAuthContext } from '../context/AuthContext';

// ===================================
// INTERFACES
// ===================================

interface CardDetails {
  card_holder_name: string;
  card_number: string;    // hiển thị dạng "4242 4242 ..."
  expiry_month: string;   // "12"
  expiry_year: string;    // "25"
  cvv: string;
  is_default: boolean;
}

// ===================================
// HELPERS
// ===================================

const getYearOptions = () => {
  // Lấy 2 số cuối của năm hiện tại
  // ví dụ: 2025 -> 25
  const currentYY = new Date().getFullYear() % 100;
  return Array.from({ length: 10 }, (_, i) =>
    String(currentYY + i).padStart(2, '0')
  );
};

const formatCardNumberDisplay = (num: string) => {
  // Xoá khoảng trắng cũ -> nhóm 4 số -> thêm khoảng trắng
  return num
    .replace(/\s/g, '')
    .replace(/(\d{4})/g, '$1 ')
    .trim();
};

// ===================================
// COMPONENT
// ===================================

export default function PaymentCard() {
  const { isAuthenticated } = useAuthContext();
  const navigate = useNavigate();

  const [cardDetails, setCardDetails] = useState<CardDetails>({
    card_holder_name: '',
    card_number: '',
    expiry_month: '',
    expiry_year: '',
    cvv: '',
    is_default: false,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // ---------------------------------
  // Handlers
  // ---------------------------------
  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;

    // xử lý riêng cho số thẻ & cvv
    if (name === 'card_number') {
      // chỉ giữ số, cắt tối đa 16 số
      const cleaned = value.replace(/\s/g, '').slice(0, 16);
      const formatted = formatCardNumberDisplay(cleaned);
      setCardDetails((prev) => ({ ...prev, card_number: formatted }));
      return;
    }

    if (name === 'cvv') {
      // giới hạn 4 ký tự số
      const cvvVal = value.replace(/\D/g, '').slice(0, 4);
      setCardDetails((prev) => ({ ...prev, cvv: cvvVal }));
      return;
    }

    if (name === 'expiry_month') {
      // month 2 ký tự
      const mm = value.slice(0, 2);
      setCardDetails((prev) => ({ ...prev, expiry_month: mm }));
      return;
    }

    // default
    setCardDetails((prev) => ({ ...prev, [name]: value }));
  };

  const handleCheckboxChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setCardDetails((prev) => ({ ...prev, is_default: e.target.checked }));
  };

  const validateForm = (): boolean => {
    const {
      card_number,
      expiry_month,
      expiry_year,
      cvv,
      card_holder_name,
    } = cardDetails;

    if (!card_holder_name.trim()) {
      setError('Vui lòng nhập tên chủ thẻ.');
      return false;
    }

    const digits = card_number.replace(/\s/g, '');
    if (digits.length !== 16) {
      setError('Số thẻ không hợp lệ (cần 16 chữ số).');
      return false;
    }

    if (
      expiry_month.length !== 2 ||
      Number(expiry_month) < 1 ||
      Number(expiry_month) > 12
    ) {
      setError('Tháng hết hạn không hợp lệ.');
      return false;
    }

    if (expiry_year.length !== 2) {
      setError('Năm hết hạn không hợp lệ.');
      return false;
    }

    if (cvv.length < 3 || cvv.length > 4) {
      setError('Mã CVV không hợp lệ.');
      return false;
    }

    setError('');
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      alert('Bạn cần đăng nhập trước.');
      navigate('/login');
      return;
    }

    if (!validateForm()) return;

    setLoading(true);
    setError('');

    const payload = {
      ...cardDetails,
      card_number: cardDetails.card_number.replace(/\s/g, ''), // bỏ khoảng trắng
    };

    try {
      // TODO: Sau này gọi API backend để lưu thẻ
      // ví dụ:
      // await api.post('/payments/cards/', payload)

      console.log('>> Thêm thẻ mock:', payload);

      alert('Thêm thẻ mới thành công (mock).');
      navigate('/payment'); // quay lại màn chọn phương thức thanh toán
    } catch (err) {
      console.error('Payment card save failed:', err);
      setError('Lỗi kết nối. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------
  // RENDER
  // ---------------------------------

  return (
    <div className="container mx-auto p-4 bg-gray-50 min-h-screen">
      <h1 className="text-3xl font-bold text-gray-800 mb-6 border-b pb-3">
        Thêm Thẻ thanh toán mới
      </h1>

      <div className="max-w-xl mx-auto bg-white rounded-xl shadow-2xl p-8 border-t-4 border-grabGreen-700">
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Error block */}
          {error && (
            <div className="p-3 bg-red-100 text-red-700 rounded-lg font-medium text-sm border border-red-300">
              {error}
            </div>
          )}

          {/* Card Number */}
          <div>
            <label
              htmlFor="card_number"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Số thẻ (16 chữ số)
            </label>
            <input
              type="text"
              id="card_number"
              name="card_number"
              value={cardDetails.card_number}
              onChange={handleChange}
              placeholder="xxxx xxxx xxxx xxxx"
              maxLength={19} // 16 digits + 3 spaces
              inputMode="numeric"
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 text-lg font-mono tracking-wider"
              required
            />
          </div>

          {/* Card Holder Name */}
          <div>
            <label
              htmlFor="card_holder_name"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Tên chủ thẻ
            </label>
            <input
              type="text"
              id="card_holder_name"
              name="card_holder_name"
              value={cardDetails.card_holder_name}
              onChange={handleChange}
              placeholder="NGUYEN VAN A"
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 text-lg uppercase"
              required
            />
          </div>

          {/* Expiry + CVV */}
          <div className="flex space-x-4">
            {/* Expiry (MM/YY) */}
            <div className="flex-grow">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Ngày hết hạn (MM/YY)
              </label>
              <div className="flex space-x-2">
                {/* Month */}
                <select
                  name="expiry_month"
                  value={cardDetails.expiry_month}
                  onChange={handleChange}
                  className="flex-1 p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
                  required
                >
                  <option value="">MM</option>
                  {Array.from({ length: 12 }, (_, i) =>
                    String(i + 1).padStart(2, '0')
                  ).map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>

                {/* Year */}
                <select
                  name="expiry_year"
                  value={cardDetails.expiry_year}
                  onChange={handleChange}
                  className="flex-1 p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
                  required
                >
                  <option value="">YY</option>
                  {getYearOptions().map((y) => (
                    <option key={y} value={y}>
                      {y}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* CVV */}
            <div className="w-1/4">
              <label
                htmlFor="cvv"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                CVV
              </label>
              <input
                type="text"
                id="cvv"
                name="cvv"
                value={cardDetails.cvv}
                onChange={handleChange}
                placeholder="***"
                maxLength={4}
                inputMode="numeric"
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 text-lg font-mono text-center"
                required
              />
            </div>
          </div>

          {/* Default Checkbox */}
          <div className="flex items-center">
            <input
              id="is_default"
              name="is_default"
              type="checkbox"
              checked={cardDetails.is_default}
              onChange={handleCheckboxChange}
              className="h-5 w-5 text-grabGreen-600 border-gray-300 rounded focus:ring-grabGreen-500"
            />
            <label
              htmlFor="is_default"
              className="ml-2 block text-sm text-gray-900"
            >
              Đặt làm phương thức thanh toán mặc định
            </label>
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="w-full py-3 text-lg text-white bg-grabGreen-700 rounded-full font-semibold hover:bg-grabGreen-800 transition duration-150 shadow-lg disabled:bg-gray-400 disabled:cursor-not-allowed"
            disabled={loading}
          >
            {loading ? 'Đang xử lý...' : 'Lưu thẻ & Quay lại thanh toán'}
          </button>

          <p className="mt-4 text-xs text-center text-gray-500">
            Thông tin thẻ của bạn được bảo mật tuyệt đối. (Demo)
          </p>
        </form>
      </div>
    </div>
  );
}
