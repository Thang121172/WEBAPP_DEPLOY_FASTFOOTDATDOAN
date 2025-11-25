import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthContext } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import api from '../services/http';

interface ShipperProfile {
  name: string;
  email: string;
  phone?: string;
  vehicle_type?: string;
  license_plate?: string;
  rating?: number;
  total_deliveries?: number;
}

export default function ShipperProfile() {
  const { user, isAuthenticated, loading: authLoading } = useAuthContext();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [profile, setProfile] = useState<ShipperProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState<ShipperProfile>({
    name: '',
    email: '',
    phone: '',
    vehicle_type: '',
    license_plate: '',
  });

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!authLoading && isAuthenticated && user?.role !== 'shipper') {
      navigate('/');
      return;
    }

    const fetchProfile = async () => {
      setLoading(true);
      try {
        // TODO: Replace with actual profile API endpoint
        // const response = await api.get('/shipper/profile/');
        // setProfile(response.data);
        // setFormData(response.data);
        
        // Use user data from context for now
        setProfile({
          name: user?.name || '',
          email: user?.email || '',
          phone: '',
          vehicle_type: '',
          license_plate: '',
          rating: 0,
          total_deliveries: 0,
        });
        setFormData({
          name: user?.name || '',
          email: user?.email || '',
          phone: '',
          vehicle_type: '',
          license_plate: '',
        });
      } catch (error) {
        console.error('Failed to fetch profile:', error);
      } finally {
        setLoading(false);
      }
    };

    if (isAuthenticated && user?.role === 'shipper') {
      fetchProfile();
    }
  }, [isAuthenticated, authLoading, user, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // TODO: Replace with actual update API endpoint
      // await api.put('/shipper/profile/', formData);
      // setProfile(formData);
      setProfile(formData);
      setEditing(false);
      showToast('Cập nhật thông tin thành công!', 'success');
    } catch (error) {
      console.error('Failed to update profile:', error);
      showToast('Không thể cập nhật thông tin. Vui lòng thử lại.', 'error');
    }
  };

  if (authLoading || loading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-xl text-gray-600">Đang tải thông tin...</div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-lg text-gray-600">Không thể tải thông tin.</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 bg-gray-50 min-h-screen">
      <div className="mb-6">
        <Link
          to="/shipper"
          className="text-grabGreen-700 hover:text-grabGreen-800 font-medium mb-4 inline-block"
        >
          &larr; Quay lại Dashboard
        </Link>
        <h1 className="text-3xl font-bold text-gray-800">Hồ sơ của tôi</h1>
      </div>

      <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100 max-w-2xl">
        {!editing ? (
          <>
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold text-gray-800">Thông tin cá nhân</h2>
              <button
                onClick={() => setEditing(true)}
                className="px-4 py-2 bg-grabGreen-700 text-white rounded-lg font-medium hover:bg-grabGreen-800 transition"
              >
                Chỉnh sửa
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-500">Họ và tên</label>
                <p className="text-lg text-gray-800">{profile.name}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-500">Email</label>
                <p className="text-lg text-gray-800">{profile.email}</p>
              </div>
              {profile.phone && (
                <div>
                  <label className="text-sm font-medium text-gray-500">Số điện thoại</label>
                  <p className="text-lg text-gray-800">{profile.phone}</p>
                </div>
              )}
              {profile.vehicle_type && (
                <div>
                  <label className="text-sm font-medium text-gray-500">Loại xe</label>
                  <p className="text-lg text-gray-800">{profile.vehicle_type}</p>
                </div>
              )}
              {profile.license_plate && (
                <div>
                  <label className="text-sm font-medium text-gray-500">Biển số xe</label>
                  <p className="text-lg text-gray-800">{profile.license_plate}</p>
                </div>
              )}
              {profile.rating !== undefined && (
                <div>
                  <label className="text-sm font-medium text-gray-500">Đánh giá</label>
                  <p className="text-lg text-gray-800">⭐ {profile.rating.toFixed(1)}</p>
                </div>
              )}
              {profile.total_deliveries !== undefined && (
                <div>
                  <label className="text-sm font-medium text-gray-500">Tổng chuyến giao</label>
                  <p className="text-lg text-gray-800">{profile.total_deliveries}</p>
                </div>
              )}
            </div>
          </>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold text-gray-800">Chỉnh sửa thông tin</h2>
              <button
                type="button"
                onClick={() => {
                  setEditing(false);
                  setFormData(profile);
                }}
                className="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-400 transition"
              >
                Hủy
              </button>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Họ và tên</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                value={formData.email}
                disabled
                className="w-full p-3 border border-gray-300 rounded-lg bg-gray-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Số điện thoại</label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Loại xe</label>
              <input
                type="text"
                value={formData.vehicle_type}
                onChange={(e) => setFormData({ ...formData, vehicle_type: e.target.value })}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
                placeholder="Ví dụ: Xe máy, Xe đạp điện"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Biển số xe</label>
              <input
                type="text"
                value={formData.license_plate}
                onChange={(e) => setFormData({ ...formData, license_plate: e.target.value })}
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500"
                placeholder="Ví dụ: 51A-12345"
              />
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-grabGreen-700 text-white rounded-lg font-semibold hover:bg-grabGreen-800 transition"
            >
              Lưu thay đổi
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

