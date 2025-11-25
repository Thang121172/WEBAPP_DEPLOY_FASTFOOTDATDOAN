import React, { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuthContext } from "../context/AuthContext";

export default function Merchant() {
  const { user, isAuthenticated, loading } = useAuthContext();
  const navigate = useNavigate();
  const [status, setStatus] = useState<
    "checking" | "registered" | "unregistered"
  >("checking");

  // Giả lập xác định trạng thái cửa hàng
  // Thực tế: sẽ gọi API kiểu /api/accounts/my_merchants/ để xem user có merchant chưa
  useEffect(() => {
    if (!loading && isAuthenticated) {
      if (user?.role === "merchant") {
        // Đã là merchant -> chuyển sang dashboard
        setStatus("registered");
        navigate("/merchant/dashboard", { replace: true });
      } else {
        // Người dùng đăng nhập nhưng chưa là merchant (customer / shipper / admin)
        setStatus("unregistered");
      }
    } else if (!loading && !isAuthenticated) {
      // Chưa đăng nhập -> ép vào login
      navigate("/login", { state: { from: "/merchant" }, replace: true });
    }
  }, [user, isAuthenticated, loading, navigate]);

  if (loading || status === "checking") {
    return (
      <div className="flex justify-center items-center h-screen bg-gray-50">
        <div className="text-xl text-gray-600">Đang kiểm tra trạng thái...</div>
      </div>
    );
  }

  // Nếu status là "registered", user đã được navigate() sang dashboard trong useEffect,
  // nhưng React vẫn render 1 khung fallback rất ngắn nếu điều hướng chưa hoàn tất.
  if (status === "registered") {
    return (
      <div className="flex justify-center items-center h-screen bg-gray-50">
        <div className="text-gray-600 text-center text-sm">
          Đang chuyển đến Dashboard cửa hàng...
        </div>
      </div>
    );
  }

  // status === "unregistered"
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 p-4">
      <div className="w-full max-w-2xl p-8 space-y-6 text-center bg-white rounded-xl shadow-2xl border-t-4 border-grabGreen-700">
        <h2 className="text-4xl font-extrabold text-gray-900">
          Trở thành Đối tác Cửa hàng FastFood
        </h2>

        <p className="text-lg text-gray-600">
          Mở rộng phạm vi tiếp cận của bạn, tăng doanh thu và phục vụ khách hàng
          mới xung quanh bạn.
        </p>

        <div className="space-y-4">
          <div className="p-4 bg-grabGreen-50 rounded-lg text-grabGreen-800 border border-grabGreen-200 text-left">
            <p className="font-semibold">
              Nếu bạn đã có tài khoản khách hàng:
            </p>
            <p className="text-sm mt-1">
              Hãy nhấn <strong>&quot;Bắt đầu Đăng ký Cửa hàng&quot;</strong> để
              gửi thông tin cửa hàng lên hệ thống.
            </p>
          </div>

          <div className="p-4 bg-gray-100 rounded-lg text-gray-700 border border-gray-200 text-left">
            <p className="font-semibold">Chưa có tài khoản?</p>
            <p className="text-sm mt-1">
              Vui lòng <strong>Đăng ký</strong> tài khoản Khách hàng trước khi
              tạo cửa hàng.
            </p>
          </div>
        </div>

        {isAuthenticated ? (
          <Link
            to="/merchant/register"
            className="inline-flex items-center justify-center w-full sm:w-auto px-8 py-3 text-lg text-white bg-grabGreen-700 rounded-full font-semibold hover:bg-grabGreen-800 transition duration-150 shadow-lg transform hover:scale-105"
          >
            Bắt đầu Đăng ký Cửa hàng
          </Link>
        ) : (
          <div className="flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-3 justify-center">
            <Link
              to="/register"
              className="inline-flex items-center justify-center px-8 py-3 text-lg text-grabGreen-700 bg-grabGreen-100 border border-grabGreen-300 rounded-full font-semibold hover:bg-grabGreen-200 transition duration-150"
            >
              Đăng ký Tài khoản
            </Link>
            <Link
              to="/login"
              className="inline-flex items-center justify-center px-8 py-3 text-lg text-white bg-grabGreen-700 rounded-full font-semibold hover:bg-grabGreen-800 transition duration-150 shadow-lg"
            >
              Đăng nhập
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
