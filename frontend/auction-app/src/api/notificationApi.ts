import api from "./axios";
import type { NotificationResponse } from "../types/notification";

export const notificationApi = {
    getMyNotificationsFeed: () => api.get<NotificationResponse[]>('/notifications/feed'),

};
