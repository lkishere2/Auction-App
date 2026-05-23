import api from './axios';
import type { FeedbackRequest, FeedbackResponse } from '../types/feedback';
import type { Page } from '../types/pagination';

export const feedbackApi = {

    createFeedback: (data: FeedbackRequest) => api.post<FeedbackResponse>('/feedback', data),

    updateFeedback: (id: number, data: FeedbackRequest) => api.put<FeedbackResponse>(`/feedback/${id}`, data),

    deleteFeedback: (id: number) => api.delete<void>(`/feedback/${id}`),

    getCurrentUserFeedback: (page = 0, size = 10) => api.get<FeedbackResponse[]>(`/feedback/my?page=${page}&size=${size}`),

    getAllFeedback: (page = 0, size = 20) => api.get<Page<FeedbackResponse>>(`/feedback/all?page=${page}&size=${size}`),

};

export default feedbackApi;
