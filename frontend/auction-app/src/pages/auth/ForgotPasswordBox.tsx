import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../../components/ui/card';
import { authApi } from '../../api/authApi';

interface ForgotPasswordBoxProps {
    onError: (title: string, message: string) => void;
}

export default function ForgotPasswordBox({ onError }: ForgotPasswordBoxProps) {
    const [email, setEmail] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            await authApi.requestPasswordReset(email);
            // Links straight to your new verify path passing the email context
            navigate('/verify/forget-password', { state: { email } });
        } catch (error: any) {
            const errorMsg = error.response?.data?.message || 'Something went wrong. Please check your email and try again.';
            onError('Request Failed', errorMsg);
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
                height: '380px',
                minHeight: '380px',
                maxHeight: '380px'
            }}
        >
            <CardHeader className="text-center space-y-[12px] pt-[40px] pb-[10px]">
                <CardTitle className="text-[26px] font-extrabold text-[#0D0D0D] tracking-tight">
                    Reset Password
                </CardTitle>
                <CardDescription className="text-[14px] text-gray-500 px-2">
                    Enter your account email, and we will send you a verification code to recover your credentials.
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
                </CardContent>

                <CardFooter className="flex flex-col space-y-[16px] pt-[10px] bg-transparent border-t-0 shadow-none">
                    <Button
                        type="submit"
                        disabled={isLoading}
                        className="w-full h-[48px] rounded-full bg-[#F5C518] text-[#0D0D0D] text-[16px] font-bold hover:bg-[#D4A900] transition-colors shadow-md border-none"
                    >
                        {isLoading ? 'Sending Code...' : 'Send Recovery Code'}
                    </Button>
                    <Link to="/login" className="text-[14px] font-bold text-[#0D0D0D] text-center hover:underline">
                        Back to Log In
                    </Link>
                </CardFooter>
            </form>
        </Card>
    );
}