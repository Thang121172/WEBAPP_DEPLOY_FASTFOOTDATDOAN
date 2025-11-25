import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthContext } from '../context/AuthContext';
import api from '../services/http';

interface AdminStats {
  total_users: number;
  total_merchants: number;
  total_shippers: number;
  total_orders: number;
  total_revenue: number;
  pending_orders: number;
}

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
};

const StatCard: React.FC<{
  title: string;
  value: string | number;
  color: string;
  icon: string;
}> = ({ title, value, color, icon }) => (
  <div className="bg-white rounded-xl shadow-lg p-6 flex flex-col transition duration-300 hover:shadow-xl border border-gray-100">
    <div className="flex items-center justify-between">
      <p className="text-sm font-medium text-gray-500">{title}</p>
      <div className={`p-2 rounded-full ${color} bg-opacity-20 text-2xl`}>
        {icon}
      </div>
    </div>
    <p className="text-3xl font-bold text-gray-900 mt-2">{value}</p>
  </div>
);

export default function AdminHome() {
  const { user, isAuthenticated, loading: authLoading } = useAuthContext();
  const navigate = useNavigate();
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!authLoading && isAuthenticated && user?.role !== 'admin') {
      navigate('/');
      return;
    }

    const fetchAdminStats = async () => {
      setLoading(true);
      try {
        // TODO: Replace with actual admin API endpoint
        // const response = await api.get('/admin/stats/');
        // setStats(response.data);
        
        // Mock data for now
        setStats({
          total_users: 0,
          total_merchants: 0,
          total_shippers: 0,
          total_orders: 0,
          total_revenue: 0,
          pending_orders: 0,
        });
      } catch (error) {
        console.error('Failed to fetch admin stats:', error);
        setStats({
          total_users: 0,
          total_merchants: 0,
          total_shippers: 0,
          total_orders: 0,
          total_revenue: 0,
          pending_orders: 0,
        });
      } finally {
        setLoading(false);
      }
    };

    if (isAuthenticated && user?.role === 'admin') {
      fetchAdminStats();
    }
  }, [isAuthenticated, authLoading, user, navigate]);

  if (authLoading || loading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-xl text-gray-600">Đang tải trang quản trị...</div>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-lg text-gray-600">Không thể tải dữ liệu.</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 bg-gray-50 min-h-screen">
      <h1 className="text-3xl font-bold text-gray-800 mb-6 border-b pb-3">
        Trang Quản trị - Dashboard
      </h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        <StatCard
          title="Tổng số Người dùng"
          value={stats.total_users}
          color="text-blue-500"
          icon="👥"
        />
        <StatCard
          title="Tổng số Cửa hàng"
          value={stats.total_merchants}
          color="text-yellow-500"
          icon="🏪"
        />
        <StatCard
          title="Tổng số Tài xế"
          value={stats.total_shippers}
          color="text-purple-500"
          icon="🚗"
        />
        <StatCard
          title="Tổng số Đơn hàng"
          value={stats.total_orders}
          color="text-green-500"
          icon="📦"
        />
        <StatCard
          title="Tổng Doanh thu"
          value={formatCurrency(stats.total_revenue)}
          color="text-grabGreen-700"
          icon="💰"
        />
        <StatCard
          title="Đơn hàng Chờ xử lý"
          value={stats.pending_orders}
          color="text-red-500"
          icon="⏳"
        />
      </div>

      {/* Quick Actions */}
      <div className="mb-6 flex flex-wrap gap-3">
        <Link
          to="/admin/users"
          className="px-4 py-2 bg-grabGreen-600 hover:bg-grabGreen-700 text-white font-semibold rounded-lg transition-colors"
        >
          👥 Quản lý Người dùng
        </Link>
      </div>

      {/* Additional admin features can be added here */}
      <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100">
        <h2 className="text-2xl font-bold text-gray-800 mb-4">Quản lý hệ thống</h2>
        <p className="text-gray-600">
          Các chức năng quản trị sẽ được thêm vào đây (quản lý người dùng, cửa hàng, đơn hàng, v.v.)
        </p>
      </div>
    </div>
  );
}

