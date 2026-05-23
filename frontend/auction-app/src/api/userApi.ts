import api from './axios';
import type { Page } from '../types/pagination';
import type { UserResponse, UsernameRequest, EmailRequest, PasswordRequest, } from '../types/user';

export const userApi = {

    getMe: () => api.get('/users/me'),

    getInfo: () => api.get<UserResponse>('/users/info'),

    updateUsername: (data: UsernameRequest) => api.patch<void>('/users/update-username', data),

    updateEmail: (data: EmailRequest) => api.patch<void>('/users/update-email', data),

    updatePassword: (data: PasswordRequest) => api.patch<void>('/users/update-password', data),

    getAllUsers: (page = 0, size = 10) => api.get<Page<UserResponse>>(`/users/all?page=${page}&size=${size}`),

    disableUser: (id: number) => api.patch<void>(`/users/disable/${id}`),

};

export default userApi;
