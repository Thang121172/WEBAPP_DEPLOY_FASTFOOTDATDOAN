import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';
import api from '../../services/http';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

interface RevenueStats {
  total_revenue: number;
  revenue_today: number;
  revenue_this_month: number;
  revenue_this_year: number;
  total_orders: number;
  orders_today: number;
  orders_this_month: number;
  average_order_value: number;
}

interface ChartDataPoint {
  date: string;
  revenue: number;
  orders: number;
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

export default function MerchantRevenue() {
  const { user, isAuthenticated, loading: authLoading } = useAuthContext();
  const navigate = useNavigate();
  const [stats, setStats] = useState<RevenueStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [chartPeriod, setChartPeriod] = useState<'month' | 'year'>('month');
  const [chartLoading, setChartLoading] = useState(true);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!authLoading && isAuthenticated && user?.role !== 'merchant' && user?.role !== 'admin') {
      navigate('/');
      return;
    }

    const fetchRevenueStats = async () => {
      setLoading(true);
      try {
        const response = await api.get('/merchant/revenue/');
        const data = response.data;
        
        setStats({
          total_revenue: parseFloat(data.total_revenue || '0'),
          revenue_today: parseFloat(data.revenue_today || '0'),
          revenue_this_month: parseFloat(data.revenue_this_month || '0'),
          revenue_this_year: parseFloat(data.revenue_this_year || '0'),
          total_orders: data.total_orders || 0,
          orders_today: data.orders_today || 0,
          orders_this_month: data.orders_this_month || 0,
          average_order_value: parseFloat(data.average_order_value || '0'),
        });
      } catch (error) {
        console.error('Failed to fetch revenue stats:', error);
        setStats({
          total_revenue: 0,
          revenue_today: 0,
          revenue_this_month: 0,
          revenue_this_year: 0,
          total_orders: 0,
          orders_today: 0,
          orders_this_month: 0,
          average_order_value: 0,
        });
      } finally {
        setLoading(false);
      }
    };

    if (isAuthenticated && (user?.role === 'merchant' || user?.role === 'admin')) {
      fetchRevenueStats();
    }
  }, [isAuthenticated, authLoading, user, navigate]);

  // Fetch chart data
  useEffect(() => {
    const fetchChartData = async () => {
      setChartLoading(true);
      try {
        const response = await api.get(`/merchant/revenue/chart/?period=${chartPeriod}`);
        setChartData(response.data.data || []);
      } catch (error) {
        console.error('Failed to fetch chart data:', error);
        setChartData([]);
      } finally {
        setChartLoading(false);
      }
    };

    if (isAuthenticated && (user?.role === 'merchant' || user?.role === 'admin')) {
      fetchChartData();
    }
  }, [isAuthenticated, user, chartPeriod]);

  if (authLoading || loading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="text-xl text-gray-600">Đang tải thống kê doanh thu...</div>
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
      <div className="mb-6">
        <Link
          to="/merchant/dashboard"
          className="text-grabGreen-700 hover:text-grabGreen-800 font-medium mb-4 inline-block"
        >
          &larr; Quay lại Dashboard
        </Link>
        <h1 className="text-3xl font-bold text-gray-800">Doanh thu Cửa hàng</h1>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Tổng doanh thu"
          value={formatCurrency(stats.total_revenue)}
          color="text-grabGreen-700"
          icon="💰"
        />
        <StatCard
          title="Doanh thu hôm nay"
          value={formatCurrency(stats.revenue_today)}
          color="text-yellow-500"
          icon="📅"
        />
        <StatCard
          title="Doanh thu tháng này"
          value={formatCurrency(stats.revenue_this_month)}
          color="text-blue-500"
          icon="📊"
        />
        <StatCard
          title="Doanh thu năm nay"
          value={formatCurrency(stats.revenue_this_year)}
          color="text-purple-500"
          icon="📈"
        />
        <StatCard
          title="Tổng đơn hàng"
          value={stats.total_orders}
          color="text-green-500"
          icon="📦"
        />
        <StatCard
          title="Đơn hàng hôm nay"
          value={stats.orders_today}
          color="text-orange-500"
          icon="🛒"
        />
        <StatCard
          title="Đơn hàng tháng này"
          value={stats.orders_this_month}
          color="text-red-500"
          icon="🎯"
        />
        <StatCard
          title="Giá trị đơn trung bình"
          value={formatCurrency(stats.average_order_value)}
          color="text-indigo-500"
          icon="📊"
        />
      </div>

      {/* Revenue Chart */}
      <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-800">Biểu đồ doanh thu</h2>
          <div className="flex gap-2">
            <button
              onClick={() => setChartPeriod('month')}
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                chartPeriod === 'month'
                  ? 'bg-grabGreen-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              30 Ngày
            </button>
            <button
              onClick={() => setChartPeriod('year')}
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                chartPeriod === 'year'
                  ? 'bg-grabGreen-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              12 Tháng
            </button>
          </div>
        </div>

        {chartLoading ? (
          <div className="flex justify-center items-center h-96">
            <div className="text-gray-600">Đang tải dữ liệu biểu đồ...</div>
          </div>
        ) : chartData.length === 0 ? (
          <div className="flex justify-center items-center h-96">
            <div className="text-gray-600">Chưa có dữ liệu để hiển thị</div>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={400}>
            <AreaChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis
                dataKey="date"
                stroke="#6b7280"
                style={{ fontSize: '12px' }}
                angle={-45}
                textAnchor="end"
                height={80}
              />
              <YAxis
                stroke="#6b7280"
                style={{ fontSize: '12px' }}
                tickFormatter={(value) => {
                  if (value >= 1000000) {
                    return `${(value / 1000000).toFixed(1)}M`;
                  } else if (value >= 1000) {
                    return `${(value / 1000).toFixed(0)}K`;
                  }
                  return value.toString();
                }}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#fff',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  padding: '12px',
                }}
                formatter={(value: number, name: string) => {
                  if (name === 'revenue') {
                    return [formatCurrency(value), 'Doanh thu'];
                  }
                  return [value, 'Số đơn'];
                }}
                labelStyle={{ fontWeight: 'bold', marginBottom: '8px' }}
              />
              <Legend
                wrapperStyle={{ paddingTop: '20px' }}
                formatter={(value) => {
                  if (value === 'revenue') return 'Doanh thu';
                  if (value === 'orders') return 'Số đơn hàng';
                  return value;
                }}
              />
              <Area
                type="monotone"
                dataKey="revenue"
                stroke="#10b981"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#colorRevenue)"
                name="revenue"
              />
              <Line
                type="monotone"
                dataKey="orders"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={{ fill: '#3b82f6', r: 4 }}
                name="orders"
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}

