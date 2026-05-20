import axios from 'axios';
import type { AxiosRequestConfig } from 'axios';

// ─── Storage helpers ──────────────────────────────────────────────────────────

const TOKEN_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

export const tokenStorage = {
    getAccess: () => localStorage.getItem(TOKEN_KEY),
    getRefresh: () => localStorage.getItem(REFRESH_KEY),
    set: (accessToken: string, refreshToken: string) => {
        localStorage.setItem(TOKEN_KEY, accessToken);
        localStorage.setItem(REFRESH_KEY, refreshToken);
    },
    clear: () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_KEY);
    },
};

// ─── Axios instance ───────────────────────────────────────────────────────────

const BASE_URL =
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api/v1';

const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ─── Request interceptor: attach access token ─────────────────────────────────

api.interceptors.request.use(
    (config) => {
        const token = tokenStorage.getAccess();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ─── Token refresh state ──────────────────────────────────────────────────────
// Prevents multiple concurrent requests from each triggering their own refresh.

let isRefreshing = false;
let pendingQueue: Array<{
    resolve: (token: string) => void;
    reject: (err: unknown) => void;
}> = [];

function flushQueue(error: unknown, token: string | null) {
    pendingQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token!)));
    pendingQueue = [];
}

// ─── Response interceptor: handle 401 → refresh → retry ──────────────────────

// Endpoints that should never trigger a refresh attempt on 401.
// A failed login/register is a user error, not an expired token.
const AUTH_WRITE_PATHS = [
    '/auth/login',
    '/auth/register',
    '/auth/refresh',
    '/auth/logout',
];

function isAuthWritePath(url: string | undefined): boolean {
    if (!url) return false;
    return AUTH_WRITE_PATHS.some((path) => url.includes(path));
}

interface RetryableConfig extends AxiosRequestConfig {
    _retry?: boolean;
}

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest: RetryableConfig = error.config;
        const status: number | undefined = error.response?.status;

        // Only intercept 401s that aren't from auth write endpoints and haven't retried yet.
        if (
            status !== 401 ||
            originalRequest._retry ||
            isAuthWritePath(originalRequest.url)
        ) {
            return Promise.reject(error);
        }

        const refreshToken = tokenStorage.getRefresh();

        // No refresh token at all → clear storage and redirect.
        if (!refreshToken) {
            tokenStorage.clear();
            window.location.href = '/login';
            return Promise.reject(error);
        }

        // If a refresh is already in-flight, queue this request.
        if (isRefreshing) {
            return new Promise<string>((resolve, reject) => {
                pendingQueue.push({ resolve, reject });
            }).then((newAccessToken) => {
                originalRequest.headers = {
                    ...originalRequest.headers,
                    Authorization: `Bearer ${newAccessToken}`,
                };
                return api(originalRequest);
            });
        }

        // This request is the one doing the refresh.
        originalRequest._retry = true;
        isRefreshing = true;

        try {
            // POST /api/v1/auth/refresh  body: { refreshToken }
            // Backend returns: { accessToken, refreshToken, expiresIn }
            const { data } = await axios.post(`${BASE_URL}/auth/refresh`, {
                refreshToken,
            });

            const { accessToken: newAccess, refreshToken: newRefresh } = data;
            tokenStorage.set(newAccess, newRefresh);

            // Patch the default header so subsequent requests use the new token immediately.
            api.defaults.headers.common['Authorization'] = `Bearer ${newAccess}`;

            flushQueue(null, newAccess);

            originalRequest.headers = {
                ...originalRequest.headers,
                Authorization: `Bearer ${newAccess}`,
            };
            return api(originalRequest);
        } catch (refreshError) {
            flushQueue(refreshError, null);
            tokenStorage.clear();
            window.location.href = '/login';
            return Promise.reject(refreshError);
        } finally {
            isRefreshing = false;
        }
    }
);

// ─── Logout helper ────────────────────────────────────────────────────────────
// Mirrors AuthServiceImpl.logout: sends both Bearer + X-Refresh-Token so the
// backend can blacklist the access token's JTI and delete the refresh token.

export async function logout(): Promise<void> {
    const refreshToken = tokenStorage.getRefresh();

    await api
        .post(
            '/auth/logout',
            {},
            {
                headers: {
                    ...(refreshToken ? { 'X-Refresh-Token': refreshToken } : {}),
                },
            }
        )
        .catch(() => {
            // Always clear locally even if the server call fails.
        });

    tokenStorage.clear();
    window.location.href = '/login';
}

export default api;