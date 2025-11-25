import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthContext } from '../context/AuthContext'

/**
 * Component bảo vệ Route.
 * Nếu người dùng chưa đăng nhập, sẽ điều hướng (Navigate) về trang /login.
 */
export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuthContext()
  
  // Trong khi đang tải trạng thái xác thực, không hiển thị gì cả
  if (loading) return null
  
  // Nếu chưa được xác thực, chuyển hướng đến trang đăng nhập
  if (!isAuthenticated) return <Navigate to="/login" replace />
  
  // Nếu đã được xác thực, hiển thị nội dung bên trong (children)
  return <>{children}</>
}