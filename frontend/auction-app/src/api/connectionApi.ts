import api from './axios';
import type { UserStats } from '../types/connection';

export const connectionApi = {

    follow: (followingId: number) =>
        api.post(`/connections/follow/${followingId}`),

    getStats: (userId: number) =>
        api.get<UserStats>(`/connections/${userId}/stats`),

}
