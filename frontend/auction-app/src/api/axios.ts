import axios from 'axios';
import type { AxiosRequestConfig } from 'axios';

// Our base URL
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api/v1';

/**
 * Auth endpoints that should never trigger a token-refresh on 401.
 * A failed login/register is a user credential error, not an expired token.
 */
const SKIP_REFRESH_PATHS = [
    '/auth/login',
    '/auth/register',
    '/auth/refresh',
    '/auth/logout',
];

// Axios instance 
// withCredentials: true tells the browser to send httpOnly cookies on every
// request. The server sets/clears tokens via Set-Cookie — JS never touches them.

const api = axios.create({
    baseURL: BASE_URL,
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true,
});

// Refresh token queue
// Holds requests that arrived while a refresh was already in flight,
// so we only ever make one refresh call at a time.

type QueueEntry = {
    resolve: () => void;
    reject: (err: unknown) => void;
};

let isRefreshing = false;
let refreshQueue: QueueEntry[] = [];

function enqueueRequest(): Promise<void> {
    return new Promise((resolve, reject) => {
        refreshQueue.push({ resolve, reject });
    });
}

function flushRefreshQueue(error: unknown) {
    refreshQueue.forEach((entry) =>
        error ? entry.reject(error) : entry.resolve()
    );
    refreshQueue = [];
}

// Refresh token logic

interface RetryableConfig extends AxiosRequestConfig {
    _retry?: boolean;
}

function shouldSkipRefresh(url: string | undefined): boolean {
    return SKIP_REFRESH_PATHS.some((path) => url?.includes(path));
}

async function refreshAccessToken(): Promise<void> {
    // Cookies are sent automatically via withCredentials.
    // The server reads the httpOnly refresh token cookie and responds with
    // Set-Cookie headers containing the new access + refresh tokens.
    await axios.post(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true });
}

// Response interceptor

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest: RetryableConfig = error.config;
        const status: number | undefined = error.response?.status;

        const shouldIntercept =
            status === 401 &&
            !originalRequest._retry &&
            !shouldSkipRefresh(originalRequest.url);

        if (!shouldIntercept) {
            return Promise.reject(error);
        }

        // A refresh is already in-flight → queue this request.
        if (isRefreshing) {
            return enqueueRequest().then(() => api(originalRequest));
        }

        // This request kicks off the refresh.
        originalRequest._retry = true;
        isRefreshing = true;

        try {
            await refreshAccessToken();
            // Server has set new cookies — just retry queued requests as-is.
            flushRefreshQueue(null);
            return api(originalRequest);
        } catch (refreshError) {
            flushRefreshQueue(refreshError);
            window.location.href = '/login';
            return Promise.reject(refreshError);
        } finally {
            isRefreshing = false;
        }
    }
);

// Logout

/**
 * Notifies the server to invalidate the current tokens (server clears the
 * cookies via Set-Cookie), then redirects to /login.
 * Always redirects even if the server call fails.
 */
export async function logout(): Promise<void> {
    await api.post('/auth/logout').catch(() => {
        // Intentionally swallowed — redirect always runs.
    });

    window.location.href = '/login';
}

export default api;