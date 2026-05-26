import api, { logout } from './axios';
import type { AuthResponse, EmailRequest, LoginRequest, RegisterRequest, ResetPasswordRequest, VerifyRequest } from '../types/auth';

export const authApi = {
    register: (data: RegisterRequest) => api.post<string>('/auth/register', data),

    login: (data: LoginRequest) => api.post<AuthResponse>('/auth/login', data),

    logout, // POST /auth/logout — handled in axios.ts (sends X-Refresh-Token header + blacklists JTI)

    refresh: (refreshToken: string) => api.post<AuthResponse>('/auth/refresh', { refreshToken } satisfies { refreshToken: string }),

    verifyUser: (data: VerifyRequest) => api.post<string>('/auth/verify', data),

    resendVerificationCode: (email: string) => api.post<string>('/auth/verify/resend', { email } satisfies EmailRequest),

    requestPasswordReset: (email: string) => api.post<string>('/auth/password-reset/request', { email } satisfies EmailRequest),

    verifyPasswordReset: (data: VerifyRequest) => api.post<string>('/auth/password-reset/verify', data),

    resetPassword: (data: ResetPasswordRequest) => api.post<string>('/auth/password-reset/confirm', data),
};