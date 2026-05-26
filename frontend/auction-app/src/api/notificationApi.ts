import api from "./axios";
import type { Slice } from "../types/pagination";
import type { NotificationResponse } from "../types/notification";

export const notificationApi = {

    getMyNotificationsFeed: () =>
        api.get<Slice<NotificationResponse>>('/notifications/feed'),

};
