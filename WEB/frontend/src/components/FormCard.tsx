import React, { ReactNode } from 'react';

interface FormCardProps {
    title: string;
    children: ReactNode;
}

/**
 * Component FormCard: Tạo khung chuẩn cho các trang form (Login, Register, OTP).
 * Giúp đồng bộ giao diện người dùng.
 */
export default function FormCard({ title, children }: FormCardProps) {
    return (
        <div className="w-full max-w-md mx-auto p-6 md:p-8 bg-white rounded-xl shadow-2xl border border-gray-100">
            {/* Tiêu đề Form */}
            <h2 className="text-2xl md:text-3xl font-extrabold text-gray-800 mb-6 text-center">
                {title}
            </h2>
            
            {/* Nội dung Form */}
            <div className="space-y-6">
                {children}
            </div>
        </div>
    );
}