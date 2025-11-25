import { useState, useEffect } from 'react';

// Định nghĩa kiểu User
export interface User {
    id: string;
    username: string;
    role: 'customer' | 'merchant' | 'shipper';
}

// Giả lập logic của useAuth
export const useAuth = () => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        // Giả lập một customer đang đăng nhập
        const mockUser: User | null = {
            id: 'mock-customer-123',
            username: 'mock_user',
            role: 'customer'
        }; 

        if (mockUser) {
            setUser(mockUser);
            setIsAuthenticated(true);
        }
        setLoading(false);
    }, []);

    // Các hàm mock
    const fetchMe = async () => {
        console.log('Mock fetchMe executed');
        return;
    };

    const login = async (payload: any) => {
        console.log('Mock login executed with payload:', payload);
        const loggedInUser: User = { id: 'user-' + Math.random(), username: 'new_user', role: 'customer' };
        setUser(loggedInUser);
        setIsAuthenticated(true);
        return { success: true };
    };

    const setToken = async (token: string) => {
        console.log('Mock setToken executed:', token);
    };

    const logout = () => {
        console.log('Mock logout executed');
        setUser(null);
        setIsAuthenticated(false);
    };

    return {
        user,
        loading,
        isAuthenticated,
        fetchMe,
        login,
        setToken,
        logout,
    };
};
