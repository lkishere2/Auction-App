import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../../components/ui/card';
import { authApi } from '../../api/authApi';

interface VerificationBoxProps {
    email: string;
    onError: (title: string, message: string) => void;
}

export default function VerificationBox({ email, onError }: VerificationBoxProps) {
    const [code, setCode] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isResending, setIsResending] = useState(false);
    const navigate = useNavigate();

    const handleVerify = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            await authApi.verifyUser({ email, verificationCode: code.trim() });
            navigate('/login');
        } catch (error: any) {
            const errorMsg = error.response?.data?.message || 'Invalid or expired verification code.';
            onError('Verification Failed', errorMsg);
        } finally {
            setIsLoading(false);
        }
    };

    const handleResendCode = async () => {
        if (isResending) return;
        setIsResending(true);
        try {
            await authApi.resendVerificationCode(email);
        } catch (error: any) {
            const errorMsg = error.response?.data?.message || 'Failed to resend code. Please try again later.';
            onError('Resend Failed', errorMsg);
        } finally {
            setIsResending(false);
        }
    };

    return (
        <Card
            className="border border-gray-100 shadow-[0_20px_50px_rgba(0,0,0,0.1)] px-[16px] rounded-[16px] flex-shrink-0 flex flex-col justify-between overflow-hidden bg-white"
            style={{
                width: '448px',
                minWidth: '448px',
                maxWidth: '448px',
                height: '420px',
                minHeight: '420px',
                maxHeight: '420px'
            }}
        >
            <CardHeader className="text-center space-y-[12px] pt-[40px] pb-[10px]">
                <CardTitle className="text-[26px] font-extrabold text-[#0D0D0D] tracking-tight">
                    Verify your email
                </CardTitle>
                <CardDescription className="text-[14px] text-gray-500 px-4">
                    We sent a verification code to <span className="font-semibold text-gray-800">{email}</span>. Enter the code below to activate your account.
                </CardDescription>
            </CardHeader>

            <form onSubmit={handleVerify} className="flex-1 flex flex-col justify-between pb-[35px]">
                <CardContent className="space-y-[20px] pt-[10px]">
                    <div className="space-y-[10px]">
                        <Label htmlFor="code" className="text-[#0D0D0D] font-semibold text-[14px] flex justify-between">
                            <span>Verification Code</span>
                            <button
                                type="button"
                                onClick={handleResendCode}
                                disabled={isResending}
                                className="text-[13px] text-gray-500 hover:text-[#D4A900] font-medium underline"
                            >
                                {isResending ? 'Sending...' : 'Resend code'}
                            </button>
                        </Label>
                        <Input
                            id="code"
                            type="text"
                            placeholder="Enter 6-digit code"
                            maxLength={6}
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            required
                            disabled={isLoading}
                            className="h-[48px] text-center tracking-[4px] font-bold text-[20px] focus-visible:ring-[#F5C518]"
                        />
                    </div>
                </CardContent>

                <CardFooter className="pt-[10px] bg-transparent border-t-0 shadow-none">
                    <Button
                        type="submit"
                        disabled={isLoading}
                        className="w-full h-[48px] rounded-full bg-[#F5C518] text-[#0D0D0D] text-[16px] font-bold hover:bg-[#D4A900] transition-colors shadow-md border-none"
                    >
                        {isLoading ? 'Verifying...' : 'Verify'}
                    </Button>
                </CardFooter>
            </form>
        </Card>
    );
}