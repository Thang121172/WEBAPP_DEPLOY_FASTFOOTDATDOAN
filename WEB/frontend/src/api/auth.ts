// Đây là file giả lập các hàm gọi API cho chức năng xác thực (Auth)

// Kiểu dữ liệu giả định cho phản hồi thành công
interface AuthSuccessResponse {
    token: string;
    user: {
        id: string;
        username: string;
        email: string;
        role: 'customer' | 'merchant' | 'shipper';
    };
}

// Định nghĩa payload đăng nhập
interface LoginPayload {
    username: string;
    email: string;
    phone: string;
    password: string;
}

// Hàm giả lập cho đăng ký
export async function registerUser(payload: any): Promise<{ success: boolean }> {
    console.log("Mock API: Đăng ký người dùng:", payload);
    await new Promise(resolve => setTimeout(resolve, 1000)); 
    return { success: true };
}

// Hàm giả lập cho xác thực OTP
export async function verifyOTP(email: string, otp: string): Promise<{ success: boolean }> {
    console.log(`Mock API: Xác thực OTP cho ${email} với mã ${otp}`);
    await new Promise(resolve => setTimeout(resolve, 1500)); 

    if (otp === '123456') { 
        return { success: true };
    } else {
        throw new Error('Mã OTP không hợp lệ hoặc đã hết hạn.');
    }
}

/**
 * HÀM ĐĂNG NHẬP ĐÃ SỬA: CHỈ NHẬN MỘT ĐỐI SỐ LÀ PAYLOAD
 */
export async function loginUser(payload: LoginPayload): Promise<AuthSuccessResponse> {
    const identifierUsed = payload.email || payload.username || payload.phone;
    console.log(`Mock API: Đăng nhập với identifier ${identifierUsed}`);
    await new Promise(resolve => setTimeout(resolve, 1000)); 

    // Giả lập đăng nhập thành công: Mật khẩu là 'password'
    if (payload.password === 'password') {
        return {
            token: 'mock_jwt_token_12345',
            user: {
                id: 'user-123',
                username: payload.username || payload.email.split('@')[0] || 'mock_user', 
                email: payload.email,
                role: 'customer', 
            }
        };
    } else {
        throw new Error('Định danh hoặc mật khẩu không đúng.');
    }
}

// Hàm giả lập cho việc đăng xuất
export async function logoutUser(): Promise<void> {
    console.log("Mock API: Đăng xuất người dùng");
    await new Promise(resolve => setTimeout(resolve, 500));
}
