import { useState, useEffect } from 'react';
import { ShieldAlert, Timer, X } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from './ui/card';

interface ErrorBoxProps {
    title?: string;
    message: string;
    countdownSeconds?: number;
    onClose: () => void;
    onActionClick: () => void;
    actionButtonText: string;
}

export default function ErrorBox({
    title = "Action Required",
    message,
    countdownSeconds = 60,
    onClose,
    onActionClick,
    actionButtonText
}: ErrorBoxProps) {
    const [timeLeft, setTimeLeft] = useState(countdownSeconds);

    useEffect(() => {
        if (timeLeft <= 0) {
            onClose();
            return;
        }

        const timer = setInterval(() => {
            setTimeLeft((prev) => prev - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [timeLeft, onClose]);

    const formatTime = (seconds: number) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    return (
        <div
            style={{
                position: 'fixed',
                bottom: '24px',
                right: '24px',
                width: '400px',
                minWidth: '400px',
                maxWidth: '400px',
                height: '280px',
                minHeight: '280px',
                maxHeight: '280px',
                zIndex: 9999
            }}
            className="select-none animate-in fade-in slide-in-from-bottom-5 duration-300"
        >
            <Card className="w-full h-full border border-gray-200/80 shadow-[0_20px_50px_rgba(0,0,0,0.15)] px-[16px] rounded-[16px] flex flex-col justify-between overflow-hidden bg-white relative">

                {/* Close Button Top Right */}
                <button
                    onClick={onClose}
                    className="absolute top-[16px] right-[16px] text-gray-400 hover:text-[#0D0D0D] transition-colors p-[2px] bg-transparent border-none outline-none cursor-pointer"
                >
                    <X size={18} />
                </button>

                {/* Header Context */}
                <CardHeader className="flex flex-row items-start space-x-[14px] space-y-0 pt-[24px] pb-[10px] pr-[24px]">
                    <div className="w-[40px] h-[40px] rounded-full bg-[#F5C518]/10 flex items-center justify-center flex-shrink-0">
                        <ShieldAlert size={22} className="text-[#F5C518]" />
                    </div>
                    <div className="flex flex-col space-y-[4px]">
                        <CardTitle className="text-[18px] font-extrabold text-[#0D0D0D] tracking-tight">
                            {title}
                        </CardTitle>
                        <CardDescription className="text-[13px] text-gray-500 leading-normal">
                            {message}
                        </CardDescription>
                    </div>
                </CardHeader>

                {/* Core Live Countdown Panel */}
                <CardContent className="py-[0px] px-[54px]">
                    <div className="border border-gray-100 bg-gray-50/70 rounded-[10px] py-[8px] px-[14px] flex items-center justify-between w-full">
                        <div className="flex items-center space-x-[6px]">
                            <Timer size={14} className="text-[#F5C518]" />
                            <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Time Left</span>
                        </div>
                        <div className="text-[20px] font-black font-mono text-[#F5C518] tracking-tight tabular-nums">
                            {formatTime(timeLeft)}
                        </div>
                    </div>
                </CardContent>

                {/* Rigid Call to Action Footer */}
                <CardFooter className="pb-[24px] pt-[12px] bg-transparent border-t-0 shadow-none">
                    <Button
                        onClick={onActionClick}
                        className="w-full h-[42px] rounded-full bg-[#F5C518] text-[#0D0D0D] text-[14px] font-bold hover:bg-[#D4A900] transition-colors shadow-sm border-none"
                    >
                        {actionButtonText}
                    </Button>
                </CardFooter>
            </Card>
        </div>
    );
}