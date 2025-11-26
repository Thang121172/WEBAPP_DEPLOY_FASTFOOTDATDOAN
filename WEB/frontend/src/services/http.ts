import axios, {
  type InternalAxiosRequestConfig,
  type AxiosError,
} from "axios";

// Base URL API
// - docker compose: VITE_API_BASE=http://backend:8000/api
// - dev local vite proxy: fallback "/api"
const API_BASE = import.meta.env.VITE_API_BASE || "/api";

const api = axios.create({
  baseURL: API_BASE, // Sử dụng proxy "/api" hoặc biến môi trường
  withCredentials: false,
  timeout: 60000, // Tăng timeout lên 60s để đợi backend wake up (Render free tier)
  headers: {
    "Content-Type": "application/json",
  },
});

// ==========================
// REQUEST INTERCEPTOR
// - Gắn Authorization: Bearer <authToken>
// ==========================
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem("authToken"); // 🔁 đồng bộ với AuthContext.tsx
    if (token) {
      // Trim token để loại bỏ whitespace có thể gây lỗi
      const cleanToken = token.trim();
      
      // Kiểm tra token có format JWT hợp lệ không (có 3 phần cách nhau bởi dấu chấm)
      if (cleanToken.split('.').length !== 3) {
        console.error('[API] Invalid token format detected. Clearing token.');
        localStorage.removeItem("authToken");
        return config;
      }
      
      config.headers = config.headers ?? {};
      (config.headers as any).Authorization = `Bearer ${cleanToken}`;
      console.log(`[API] Request to ${config.url} with token: ${cleanToken.substring(0, 20)}...`);
    } else {
      console.warn(`[API] Request to ${config.url} without token`);
    }
    
    // Nếu là FormData, không set Content-Type header - axios sẽ tự động set với boundary
    if (config.data instanceof FormData) {
      // Xóa Content-Type header để browser tự động set với boundary
      if (config.headers) {
        delete (config.headers as any)['Content-Type'];
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// ==========================
// RESPONSE INTERCEPTOR
// - Nếu backend trả về 401 => xoá token local
// ==========================
api.interceptors.response.use(
  (res) => res,
  (err: AxiosError) => {
    // Xử lý lỗi 401 và lỗi token không hợp lệ
    if (err.response?.status === 401) {
      const errorDetail = (err.response?.data as any)?.detail || '';
      const isTokenError = 
        errorDetail.includes('token') || 
        errorDetail.includes('Token') ||
        errorDetail.includes('authentication') ||
        errorDetail.includes('credentials');
      
      if (isTokenError) {
        // Token không hợp lệ hoặc đã hết hạn
        const token = localStorage.getItem("authToken");
        localStorage.removeItem("authToken");
        localStorage.removeItem("refreshToken"); // Xóa cả refresh token nếu có
        
        console.warn("[API] Token invalid or expired (401). Clearing tokens.");
        console.warn("[API] Error detail:", errorDetail);
        console.warn("[API] Request URL:", err.config?.url);
        console.warn("[API] Had token:", token ? token.substring(0, 20) + '...' : 'none');
        
        // Redirect to login nếu đang ở trang cần authentication
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          // Chỉ redirect nếu không phải đang ở trang login/register
          // (tránh redirect loop)
          setTimeout(() => {
            window.location.href = '/login';
          }, 100);
        }
      }
    }
    return Promise.reject(err);
  }
);

export default api;
export { API_BASE };
