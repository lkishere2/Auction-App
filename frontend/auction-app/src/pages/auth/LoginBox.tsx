import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../../components/ui/card';
import { authApi } from '../../api/authApi';

interface LoginBoxProps {
    onError: (title: string, message: string, email?: string, isUnverified?: boolean) => void;
}

export default function LoginBox({ onError }: LoginBoxProps) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            const response = await authApi.login({ email, password });
            console.log('Authentication successful:', response.data);

            // Save tokens securely to the browser storage layer
            localStorage.setItem('accessToken', response.data.accessToken);
            localStorage.setItem('refreshToken', response.data.refreshToken);

            // Redirect cleanly back to the Home Route (/)
            navigate('/home');
        } catch (error: any) {
            // Read backend error message directly from your response body structure
            const errorMsg = error.response?.data?.message || 'Invalid email address or password.';

            // Check against your Spring Boot AccountNotVerifiedException signature
            const isUnverified = errorMsg.includes('not verified') || error.response?.status === 403;

            if (isUnverified) {
                onError('Account Unverified', errorMsg, email, true);
            } else {
                onError('Login Failed', errorMsg, undefined, false);
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Card
            className="border border-gray-100 shadow-[0_20px_50px_rgba(0,0,0,0.1)] px-[16px] rounded-[16px] flex-shrink-0 flex flex-col justify-between overflow-hidden bg-white"
            style={{
                width: '448px',
                minWidth: '448px',
                maxWidth: '448px',
                height: '580px',
                minHeight: '580px',
                maxHeight: '580px'
            }}
        >
            <CardHeader className="text-center space-y-[12px] pt-[40px] pb-[10px]">
                <CardTitle className="text-[30px] font-extrabold text-[#0D0D0D] tracking-tight">
                    BidVault
                </CardTitle>
                <CardDescription className="text-[16px] text-gray-500">
                    Sign in to access your secure account.
                </CardDescription>
            </CardHeader>

            <form onSubmit={handleSubmit} className="flex-1 flex flex-col justify-between pb-[35px]">
                <CardContent className="space-y-[20px] pt-[10px]">
                    <div className="space-y-[10px]">
                        <Label htmlFor="email" className="text-[#0D0D0D] font-semibold text-[14px]">
                            Email Address
                        </Label>
                        <Input
                            id="email"
                            type="email"
                            placeholder="you@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            disabled={isLoading}
                            className="h-[48px] focus-visible:ring-[#F5C518] text-[16px]"
                        />
                    </div>

                    <div className="space-y-[10px]">
                        <div className="flex justify-between items-center">
                            <Label htmlFor="password" className="text-[#0D0D0D] font-semibold text-[14px]">
                                Password
                            </Label>
                            <Link
                                to="/forget-password"
                                className="text-[13px] font-medium text-gray-500 hover:text-[#D4A900] transition-colors"
                            >
                                Forgot password?
                            </Link>
                        </div>
                        <Input
                            id="password"
                            type="password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            disabled={isLoading}
                            className="h-[48px] focus-visible:ring-[#F5C518] text-[16px]"
                        />
                    </div>
                </CardContent>

                <CardFooter className="flex flex-col space-y-[20px] pt-[10px] bg-transparent border-t-0 shadow-none">
                    <Button
                        type="submit"
                        disabled={isLoading}
                        className="w-full h-[48px] rounded-full bg-[#F5C518] text-[#0D0D0D] text-[16px] font-bold hover:bg-[#D4A900] transition-colors shadow-md border-none"
                    >
                        {isLoading ? 'Logging In...' : 'Log In'}
                    </Button>

                    <p className="text-[14px] text-gray-500 text-center">
                        Don't have an account?{' '}
                        <Link
                            to="/register"
                            className="font-bold text-[#0D0D0D] hover:text-[#D4A900] transition-colors underline underline-offset-4"
                        >
                            Register
                        </Link>
                    </p>
                </CardFooter>
            </form>
        </Card>
    );
}