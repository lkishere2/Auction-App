import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

// Định nghĩa cấu trúc dữ liệu hiển thị mẫu
interface AccountData {
    balance: number;
    currency: string;
    auctions: Array<{ id: string; title: string; currentBid: number; status: string }>;
    products: Array<{ id: string; name: string; basePrice: number }>;
}

export default function AccountPage() {
    const navigate = useNavigate();
    const [accountInfo, setAccountInfo] = useState<AccountData | null>(null);
    const [loading, setLoading] = useState<boolean>(true);

    useEffect(() => {
        // Giả lập gọi API lấy dữ liệu tài khoản sau 500ms
        const timer = setTimeout(() => {
            setAccountInfo({
                balance: 2450.75,
                currency: "USD",
                auctions: [
                    { id: "a1", title: "Vintage Rolex Submariner", currentBid: 1200, status: "Active" },
                    { id: "a2", title: "PlayStation 5 Pro Edition", currentBid: 650, status: "Active" },
                    { id: "a3", title: "Charizard First Edition Card", currentBid: 450, status: "Ended" },
                    { id: "a4", title: "LeBron James Signed Jersey", currentBid: 300, status: "Active" },
                ],
                products: [
                    { id: "p1", name: "iPhone 15 Pro Max 256GB", basePrice: 999 },
                    { id: "p2", name: "AirPods Max Space Gray", basePrice: 450 },
                    { id: "p3", name: "Nike Air Jordan 1 Retro", basePrice: 180 },
                    { id: "p4", name: "Mechanical Keyboard Custom", basePrice: 220 },
                    { id: "p5", name: "Sony WH-1000XM5", basePrice: 350 },
                ]
            });
            setLoading(false);
        }, 500);

        return () => clearTimeout(timer);
    }, []);

    if (loading) {
        return (
            <div className="p-8 text-[#F5C518] animate-pulse font-medium">
                Loading account overview...
            </div>
        );
    }

    if (!accountInfo) return null;

    return (
        <div className="w-full max-w-6xl mx-auto px-8 py-6 flex flex-col gap-8 select-none text-left">
            
            {/* 1. Thẻ hiển thị số dư tiền */}
            <div className="w-full max-w-sm bg-white border border-[#E8E8E8] rounded-2xl p-6 shadow-sm flex flex-col gap-2">
                <span className="text-[12px] text-[#999] font-semibold uppercase tracking-wider">
                    Available Balance
                </span>
                <div className="flex items-baseline gap-1.5">
                    <span className="text-3xl font-black text-[#0D0D0D]">
                        {accountInfo.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </span>
                    <span className="text-sm font-bold text-[#F5C518] uppercase">
                        {accountInfo.currency}
                    </span>
                </div>
                <button 
                    onClick={() => navigate('/wallet/deposit')}
                    className="mt-2 w-full bg-[#0D0D0D] hover:bg-[#2A2A2A] text-white text-xs font-bold py-2 px-4 rounded-full transition-all tracking-wide text-center"
                >
                    + Deposit Funds
                </button>
            </div>

            <hr className="border-[#F0F0F0]" />

            {/* 2. Section: All my auctions (Cuộn hàng ngang nếu tràn) */}
            <div className="flex flex-col gap-4">
                <div className="flex justify-between items-center">
                    <h2 className="text-lg font-extrabold tracking-tight text-[#0D0D0D]">
                        All my auctions
                    </h2>
                    <span className="text-xs text-[#888] font-medium">
                        {accountInfo.auctions.length} items
                    </span>
                </div>

                {/* Hàng ngang hỗ trợ cuộn chuột sang phải */}
                <div className="w-full flex gap-4 overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-gray-200 snap-x">
                    {accountInfo.auctions.map((auction) => (
                        <div 
                            key={auction.id} 
                            className="w-[260px] shrink-0 bg-white border border-[#E8E8E8] rounded-xl p-4 flex flex-col gap-3 snap-start hover:border-[#F5C518] transition-all group cursor-pointer"
                        >
                            <div className="w-full h-32 bg-[#F9F9F9] rounded-lg border border-[#F0F0F0] flex items-center justify-center text-xs text-gray-400">
                                [ Auction Image ]
                            </div>
                            <div className="flex flex-col gap-1">
                                <h3 className="font-bold text-sm text-[#0D0D0D] truncate group-hover:text-[#F5C518] transition-colors">
                                    {auction.title}
                                </h3>
                                <div className="flex justify-between items-center mt-1">
                                    <div className="flex flex-col">
                                        <span className="text-[10px] text-[#999] uppercase font-bold">Current Bid</span>
                                        <span className="text-sm font-black text-[#0D0D0D]">${auction.currentBid}</span>
                                    </div>
                                    <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${auction.status === 'Active' ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-500'}`}>
                                        {auction.status}
                                    </span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <hr className="border-[#F0F0F0]" />

            {/* 3. Section: All my products (Cuộn hàng ngang nếu tràn) */}
            <div className="flex flex-col gap-4">
                <div className="flex justify-between items-center">
                    <h2 className="text-lg font-extrabold tracking-tight text-[#0D0D0D]">
                        All my products
                    </h2>
                </div>

                {/* Hàng ngang hỗ trợ cuộn chuột sang phải */}
                <div className="w-full flex gap-4 overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-gray-200 snap-x">
                    {accountInfo.products.map((product) => (
                        <div 
                            key={product.id} 
                            className="w-[220px] shrink-0 bg-white border border-[#E8E8E8] rounded-xl p-4 flex flex-col gap-3 snap-start hover:shadow-sm transition-all"
                        >
                            <div className="w-full h-28 bg-[#F9F9F9] rounded-lg border border-[#F0F0F0] flex items-center justify-center text-xs text-gray-400">
                                [ Product Image ]
                            </div>
                            <div className="flex flex-col gap-2">
                                <h3 className="font-bold text-xs text-[#0D0D0D] truncate">
                                    {product.name}
                                </h3>
                                <div className="flex flex-col">
                                    <span className="text-[10px] text-[#999] uppercase font-bold">Base Price</span>
                                    <span className="text-xs font-bold text-gray-700">${product.basePrice}</span>
                                </div>
                                <button 
                                    onClick={() => navigate(`/auctions/create?product=${product.id}`)}
                                    className="w-full mt-1 bg-white hover:bg-[#0D0D0D] hover:text-white text-[#0D0D0D] border border-[#0D0D0D] text-[11px] font-bold py-1.5 px-3 rounded-md transition-all text-center"
                                >
                                    Create Auction
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

        </div>
    );
}