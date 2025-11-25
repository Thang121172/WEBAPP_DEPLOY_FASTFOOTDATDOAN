import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthContext } from '../context/AuthContext'

type Role = "customer" | "merchant" | "shipper" | "admin";

type RoleGateProps = {
  allow: Role[];                 // ✅ prop cần thiết
  children: React.ReactNode;
  fallback?: React.ReactNode;    // tuỳ chọn: render thay thế nếu không đủ quyền
};
export default function RoleGate({ allow, children, fallback = null }: RoleGateProps) {
  const { user } = useAuthContext();
  const role: Role = (user?.role as Role) ?? "customer";

  if (allow.includes(role)) return <>{children}</>;
  return <>{fallback}</>;
}
