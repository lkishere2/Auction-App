import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginBox from './LoginBox';
import ErrorBox from '../../components/ErrorBox';
import { authApi } from '../../api/authApi';

interface LoginPageProps {
    onLoginSuccess: (user: { username: string; initials: string }) => void;
}

export default function LoginPage({ onLoginSuccess }: LoginPageProps) {
    const [alert, setAlert] = useState<{ title: string; message: string; email?: string; isUnverified: boolean } | null>(null);
    const navigate = useNavigate();

    const handleLoginError = (title: string, message: string, email?: string, isUnverified = false) => {
        setAlert({ title, message, email, isUnverified });
    };

    const handleResendVerification = async () => {
        if (!alert?.email) return;
        try {
            await authApi.resendVerificationCode(alert.email);
            setAlert(null);
            // Điều hướng mượt mà sang luồng xác thực người dùng kèm theo context email
            navigate('/verify/user', { state: { email: alert.email } });
        } catch (error) {
            console.error('Failed to automatically trigger verification re-send drop:', error);
        }
    };

    // Xử lý khi đăng nhập thành công
    const handleLoginSuccessInternal = (userData: { username: string; initials: string }) => {
        // Gọi callback truyền ngược lên App.tsx để lưu thông tin user / token vào state tổng
        onLoginSuccess(userData);
        
        // Chuyển hướng người dùng sang trang Dashboard ở đường dẫn /office
        navigate('/office');
    };

    return (
        <main className="w-screen h-screen min-h-screen flex items-center justify-center bg-gray-50 overflow-hidden select-none relative">
            {/* Đảm bảo component LoginBox của bạn nhận prop onSuccess để truyền lại thông tin user khi login đúng */}
            <LoginBox onError={handleLoginError} onSuccess={handleLoginSuccessInternal} />

            {alert && (
                <ErrorBox
                    title={alert.title}
                    message={alert.message}
                    countdownSeconds={alert.isUnverified ? 120 : undefined}
                    actionButtonText={alert.isUnverified ? "Resend Verification Link" : undefined}
                    onClose={() => setAlert(null)}
                    onActionClick={alert.isUnverified ? handleResendVerification : undefined}
                />
            )}
        </main>
    );
}