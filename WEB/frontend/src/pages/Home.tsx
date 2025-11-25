import React from 'react'
import { Link } from 'react-router-dom';
import { useAuthContext } from '../context/AuthContext' 

// ===================================
// ROLE PLACEHOLDERS
// ===================================

// Placeholder cho Khách hàng
function CustomerHome() {
    return (
        <div className="text-center p-12 bg-white rounded-xl shadow-2xl border-t-4 border-grabGreen-700 max-w-2xl mx-auto my-10">
            <h2 className="text-3xl font-extrabold text-gray-900 mb-4">Chào mừng đến với Fast Food!</h2>
            <p className="text-gray-600 mb-6">Bạn đang ở vai trò Khách hàng. Hãy khám phá những món ăn ngon gần bạn.</p>
            <Link 
                to="/customer" 
                className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-full shadow-sm text-white bg-grabGreen-700 hover:bg-grabGreen-800 transition duration-150"
            >
                Đặt hàng ngay
            </Link>
            <div className='mt-4 text-sm text-gray-500'>
                <Link to="/merchant/register" className="font-medium hover:text-grabGreen-700 transition">Đăng ký trở thành đối tác cửa hàng</Link>
            </div>
        </div>
    );
}

// Placeholder cho Đối tác Cửa hàng
function MerchantHome(){
    return (
        <div className="max-w-4xl mx-auto my-10 p-8 bg-white rounded-xl shadow-2xl border-t-4 border-yellow-500">
            <h2 className="text-3xl font-extrabold text-yellow-700 mb-4">Quản lý Cửa hàng</h2>
            <p className="text-gray-600 mb-6">Bạn đang ở vai trò Đối tác Cửa hàng. Xem tổng quan đơn hàng và thống kê.</p>
            <Link 
                to="/merchant/dashboard" 
                className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-full shadow-sm text-white bg-yellow-600 hover:bg-yellow-700 transition duration-150"
            >
                Đi đến Dashboard
            </Link>
        </div>
    );
}

// Placeholder cho Đối tác Tài xế
function ShipperHome(){
    return (
        <div className="max-w-4xl mx-auto my-10 p-8 bg-white rounded-xl shadow-2xl border-t-4 border-blue-500">
            <h2 className="text-3xl font-extrabold text-blue-700 mb-4">Quản lý Đơn giao</h2>
            <p className="text-gray-600 mb-6">Bạn đang ở vai trò Tài xế. Xem các đơn hàng đang chờ nhận và giao.</p>
            <Link 
                to="/shipper" 
                className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-full shadow-sm text-white bg-blue-600 hover:bg-blue-700 transition duration-150"
            >
                Xem Đơn hàng
            </Link>
        </div>
    );
}


// ===================================
// MAIN COMPONENT
// ===================================

export default function Home() {
    const { user, loading } = useAuthContext();

    // Hiển thị loading trong khi chờ xác thực
    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen">
                <p className='text-gray-500'>Đang tải...</p>
            </div>
        );
    }

    // Hiển thị giao diện dựa trên vai trò người dùng
    if (user) {
        switch (user.role) {
            case 'merchant':
                return <MerchantHome />;
            case 'shipper':
                return <ShipperHome />;
            case 'customer':
            default:
                // Nếu là admin hoặc customer, hiển thị giao diện CustomerHome mặc định
                return <CustomerHome />;
        }
    }

    // Trường hợp chưa đăng nhập, hiển thị giao diện CustomerHome mặc định
    return <CustomerHome />;
}
