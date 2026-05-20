import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../../components/ui/card';
import { authApi } from '../../api/authApi';

interface ForgotPasswordVerifyBoxProps {
    email: string;
    onError: (title: string, message: string) => void;
}

export default function ForgotPasswordVerifyBox({ email, onError }: ForgotPasswordVerifyBoxProps) {
    const [code, setCode] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (newPassword !== confirmPassword) {
            onError('Validation Error', 'New passwords do not match.');
            return;
        }

        setIsLoading(true);

        try {
            // First verify the validation token
            await authApi.verifyPasswordReset({ email, verificationCode: code.trim() });

            // Execute password updates directly to the database layer
            await authApi.resetPassword({ email, password: newPassword });

            navigate('/login');
        } catch (error: any) {
            const errorMsg = error.response?.data?.message || 'Verification or update action failed. Please check your recovery code token.';
            onError('Reset Failed', errorMsg);
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
                height: '590px',
                minHeight: '590px',
                maxHeight: '590px'
            }}
        >
            <CardHeader className="text-center space-y-[6px] pt-[30px] pb-[5px]">
                <CardTitle className="text-[26px] font-extrabold text-[#0D0D0D] tracking-tight">
                    Change Password
                </CardTitle>
                <CardDescription className="text-[14px] text-gray-500 px-4">
                    Enter the code sent to <span className="font-semibold text-gray-800">{email}</span> alongside your new credentials.
                </CardDescription>
            </CardHeader>

            <form onSubmit={handleSubmit} className="flex-1 flex flex-col justify-between pb-[30px]">
                <CardContent className="space-y-[14px] pt-[10px]">
                    <div className="space-y-[6px]">
                        <Label htmlFor="code" className="text-[#0D0D0D] font-semibold text-[13px]">
                            Reset Verification Code
                        </Label>
                        <Input
                            id="code"
                            type="text"
                            placeholder="6-digit verification code"
                            maxLength={6}
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            required
                            disabled={isLoading}
                            className="h-[42px] tracking-[2px] text-center font-bold focus-visible:ring-[#F5C518] text-[15px]"
                        />
                    </div>

                    <div className="space-y-[6px]">
                        <Label htmlFor="newPassword" className="text-[#0D0D0D] font-semibold text-[13px]">
                            New Password
                        </Label>
                        <Input
                            id="newPassword"
                            type="password"
                            placeholder="••••••••"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            required
                            disabled={isLoading}
                            className="h-[42px] focus-visible:ring-[#F5C518] text-[15px]"
                        />
                    </div>

                    <div className="space-y-[6px]">
                        <Label htmlFor="confirmPassword" className="text-[#0D0D0D] font-semibold text-[13px]">
                            Confirm New Password
                        </Label>
                        <Input
                            id="confirmPassword"
                            type="password"
                            placeholder="••••••••"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            disabled={isLoading}
                            className="h-[42px] focus-visible:ring-[#F5C518] text-[15px]"
                        />
                    </div>
                </CardContent>

                <CardFooter className="pt-[10px] bg-transparent border-t-0 shadow-none">
                    <Button
                        type="submit"
                        disabled={isLoading}
                        className="w-full h-[48px] rounded-full bg-[#F5C518] text-[#0D0D0D] text-[16px] font-bold hover:bg-[#D4A900] transition-colors shadow-md border-none"
                    >
                        {isLoading ? 'Updating Password...' : 'Reset Password'}
                    </Button>
                </CardFooter>
            </form>
        </Card>
    );
}