import React, { useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthContext } from '../context/AuthContext';

export default function OtpSuccess() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuthContext();

  useEffect(() => {
    // Auto redirect after 3 seconds
    const timer = setTimeout(() => {
      if (isAuthenticated && user) {
        // Redirect based on user role
        switch (user.role) {
          case 'merchant':
            navigate('/merchant/dashboard');
            break;
          case 'shipper':
            navigate('/shipper');
            break;
          case 'admin':
            navigate('/admin');
            break;
          case 'customer':
          default:
            navigate('/customer');
            break;
        }
      } else {
        navigate('/login');
      }
    }, 3000);

    return () => clearTimeout(timer);
  }, [isAuthenticated, user, navigate]);

  return (
    <div className="flex justify-center items-center min-h-screen bg-gray-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full text-center border-t-4 border-grabGreen-700">
        <div className="text-6xl mb-4">✅</div>
        <h1 className="text-3xl font-bold text-gray-800 mb-4">Xác minh thành công!</h1>
        <p className="text-gray-600 mb-6">
          Tài khoản của bạn đã được kích hoạt thành công. Bạn sẽ được chuyển hướng tự động...
        </p>
        <div className="space-y-3">
          {isAuthenticated && user ? (
            <>
              <Link
                to={
                  user.role === 'merchant'
                    ? '/merchant/dashboard'
                    : user.role === 'shipper'
                    ? '/shipper'
                    : user.role === 'admin'
                    ? '/admin'
                    : '/customer'
                }
                className="block w-full py-3 bg-grabGreen-700 text-white rounded-lg font-semibold hover:bg-grabGreen-800 transition"
              >
                Đi đến trang chủ
              </Link>
            </>
          ) : (
            <Link
              to="/login"
              className="block w-full py-3 bg-grabGreen-700 text-white rounded-lg font-semibold hover:bg-grabGreen-800 transition"
            >
              Đăng nhập ngay
            </Link>
          )}
          <Link
            to="/"
            className="block w-full py-3 bg-gray-200 text-gray-700 rounded-lg font-semibold hover:bg-gray-300 transition"
          >
            Về trang chủ
          </Link>
        </div>
      </div>
    </div>
  );
}

