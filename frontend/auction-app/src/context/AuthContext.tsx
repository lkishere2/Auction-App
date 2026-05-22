import React, { createContext, useContext, useEffect, useState } from 'react';

interface User {
    username: string;
    initials: string;
    email?: string;
}

interface AuthContextType {
    isLoggedIn: boolean;
    user: User | null;
    logout: () => void;
    checkAuth: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [user, setUser] = useState<User | null>(null);

    const decodeToken = (token: string): any => {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map((c) => {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error('Failed to decode token:', error);
            return null;
        }
    };

    const checkAuth = () => {
        const accessToken = localStorage.getItem('accessToken');
        if (accessToken) {
            try {
                const decoded = decodeToken(accessToken);
                if (decoded) {
                    const username = decoded.sub || decoded.username || 'User';
                    const initials = username.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2) || 'U';
                    setUser({
                        username,
                        initials,
                        email: decoded.email || decoded.sub
                    });
                    setIsLoggedIn(true);
                    return;
                }
            } catch (error) {
                console.error('Error checking auth:', error);
            }
        }
        setIsLoggedIn(false);
        setUser(null);
    };

    const logout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        setIsLoggedIn(false);
        setUser(null);
    };

    useEffect(() => {
        checkAuth();
    }, []);

    return (
        <AuthContext.Provider value={{ isLoggedIn, user, logout, checkAuth }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
