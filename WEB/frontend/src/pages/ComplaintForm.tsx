import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../services/http';
import { useAuthContext } from '../context/AuthContext';
import { useToast } from '../components/Toast';

interface Order {
  id: number;
  status: string;
  merchant: { id: number; name: string };
}

const COMPLAINT_TYPES = [
  { value: 'ORDER_ISSUE', label: 'Vấn đề về đơn hàng' },
  { value: 'FOOD_QUALITY', label: 'Chất lượng món ăn' },
  { value: 'DELIVERY_ISSUE', label: 'Vấn đề giao hàng' },
  { value: 'PAYMENT_ISSUE', label: 'Vấn đề thanh toán' },
  { value: 'OTHER', label: 'Khác' }
];

export default function ComplaintForm() {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthContext();
  const { showToast } = useToast();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [complaintType, setComplaintType] = useState('OTHER');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    const fetchOrder = async () => {
      setLoading(true);
      try {
        const response = await api.get(`/orders/${orderId}/`);
        setOrder(response.data);
      } catch (error) {
        console.error('Failed to fetch order:', error);
        showToast('Không thể tải thông tin đơn hàng', 'error');
        navigate('/');
      } finally {
        setLoading(false);
      }
    };

    if (orderId) {
      fetchOrder();
    }
  }, [orderId, isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!orderId || !title || !description) {
      showToast('Vui lòng điền đầy đủ thông tin', 'warning');
      return;
    }

    setSubmitting(true);
    try {
      await api.post('/complaints/', {
        order_id: parseInt(orderId),
        complaint_type: complaintType,
        title: title,
        description: description
      });

      showToast('Khiếu nại đã được gửi thành công. Chúng tôi sẽ xử lý sớm nhất có thể.', 'success');
      navigate(`/orders/${orderId}`);
    } catch (error: any) {
      console.error('Failed to submit complaint:', error);
      showToast(error.response?.data?.detail || 'Không thể gửi khiếu nại. Vui lòng thử lại.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-xl text-gray-600">Đang tải...</div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-lg font-medium text-gray-700">Không tìm thấy đơn hàng</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 bg-gray-50 min-h-screen">
      <div className="mb-6">
        <Link to={`/orders/${orderId}`} className="text-grabGreen-700 hover:text-grabGreen-800 font-medium">
          &larr; Quay lại
        </Link>
        <h1 className="text-3xl font-bold text-gray-800 mt-2">Gửi khiếu nại - Đơn hàng #{orderId}</h1>
      </div>

      <form onSubmit={handleSubmit} className="max-w-3xl mx-auto bg-white rounded-xl shadow-lg p-6">
        {/* Order Info */}
        <div className="mb-6 p-4 bg-gray-50 rounded-lg">
          <p className="text-sm text-gray-600">Cửa hàng: <span className="font-semibold">{order.merchant.name}</span></p>
          <p className="text-sm text-gray-600">Trạng thái: <span className="font-semibold">{order.status}</span></p>
        </div>

        {/* Complaint Type */}
        <div className="mb-6">
          <label className="block text-lg font-semibold text-gray-800 mb-3">
            Loại khiếu nại <span className="text-red-500">*</span>
          </label>
          <select
            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-grabGreen-500 focus:border-transparent"
            value={complaintType}
            onChange={(e) => setComplaintType(e.target.value)}
            required
          >
            {COMPLAINT_TYPES.map((type) => (
              <option key={type.value} value={type.value}>
                {type.label}
              </option>
            ))}
          </select>
        </div>

        {/* Title */}
        <div className="mb-6">
          <label className="block text-lg font-semibold text-gray-800 mb-3">
            Tiêu đề <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-grabGreen-500 focus:border-transparent"
            placeholder="Ví dụ: Món ăn không đúng với đơn đặt"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        {/* Description */}
        <div className="mb-6">
          <label className="block text-lg font-semibold text-gray-800 mb-3">
            Mô tả chi tiết <span className="text-red-500">*</span>
          </label>
          <textarea
            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-grabGreen-500 focus:border-transparent"
            rows={6}
            placeholder="Vui lòng mô tả chi tiết vấn đề bạn gặp phải..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
          />
        </div>

        {/* Submit Button */}
        <div className="flex gap-3">
          <Link
            to={`/orders/${orderId}`}
            className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-3 px-6 rounded-lg transition-colors text-center"
          >
            Hủy
          </Link>
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 bg-orange-600 hover:bg-orange-700 text-white font-semibold py-3 px-6 rounded-lg transition-colors disabled:opacity-50"
          >
            {submitting ? 'Đang gửi...' : 'Gửi khiếu nại'}
          </button>
        </div>
      </form>
    </div>
  );
}

