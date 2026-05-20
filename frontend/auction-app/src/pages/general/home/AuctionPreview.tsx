import CardDeck from './CardDesk';
import type { AuctionItem } from './AuctionCard';

const MOCK_LIVE_AUCTIONS: AuctionItem[] = [
    {
        id: '1',
        title: 'Vintage Rolex Submariner Ref. 1680 "Red Sub" (1974)',
        currentBid: 14200,
        timeLeft: '02h 14m 45s',
        image: 'https://images.unsplash.com/photo-1547996160-81dfa63595aa?w=600&q=80',
        bidsCount: 24,
    },
    {
        id: '2',
        title: 'Original Contemporary Abstract Oil Painting on Canvas',
        currentBid: 3850,
        timeLeft: '05h 32m 10s',
        image: 'https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=600&q=80',
        bidsCount: 11,
    },
    {
        id: '3',
        title: 'First Edition Signed Fantasy Novel Hardcover (1997)',
        currentBid: 1200,
        timeLeft: '00h 42m 18s',
        image: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600&q=80',
        bidsCount: 19,
    },
    {
        id: '4',
        title: 'Restored Retro Arcade Cabinet Classic Multi-Game System',
        currentBid: 2450,
        timeLeft: '08h 11m 02s',
        image: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&q=80',
        bidsCount: 8,
    },
    {
        id: '5',
        title: 'Limited Edition Carbon Fiber Racing Drone Frame & Kit',
        currentBid: 920,
        timeLeft: '01h 05m 50s',
        image: 'https://images.unsplash.com/photo-1527977966376-1c8408f9f108?w=600&q=80',
        bidsCount: 15,
    },
    {
        id: '6',
        title: '1999 Base Set First Edition Holographic Charizard #4 (PSA 9)',
        currentBid: 18400,
        timeLeft: '03h 05m 12s',
        image: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&q=80',
        bidsCount: 42,
    },
    {
        id: '7',
        title: 'Classic Leica M6 35mm Rangefinder Film Camera (Black Chrome)',
        currentBid: 3100,
        timeLeft: '06h 19m 30s',
        image: 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&q=80',
        bidsCount: 14,
    },
    {
        id: '8',
        title: 'Authentic Mid-Century Eames Lounge Chair & Ottoman (Walnut)',
        currentBid: 5200,
        timeLeft: '11h 40m 55s',
        image: 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600&q=80',
        bidsCount: 21,
    },
    {
        id: '9',
        title: 'Autographed Michael Jordan 1996 Chicago Bulls Jersey (UDA COA)',
        currentBid: 8900,
        timeLeft: '00h 15m 22s',
        image: 'https://images.unsplash.com/photo-1519766304817-4f37bda74a27?w=600&q=80',
        bidsCount: 37,
    },
    {
        id: '10',
        title: 'Vintage Schott Perfecto Leather Motorcycle Jacket (Size 40)',
        currentBid: 650,
        timeLeft: '04h 50m 01s',
        image: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600&q=80',
        bidsCount: 9,
    }
];

export default function AuctionPreview() {
    return (
        <section className="relative z-10 pt-32 pb-20 bg-gradient-to-b from-transparent via-white to-white">
            <div className="max-w-7xl mx-auto px-4">
                <div className="text-center mb-12">
                    <p className="text-[11px] font-bold tracking-[0.12em] uppercase text-[#F5C518] mb-2">
                        Discover items
                    </p>
                    <h2 className="text-3xl font-extrabold text-[#0D0D0D] tracking-tight">
                        Live Showroom Auctions
                    </h2>
                    <div className="w-12 h-[2px] bg-[#F5C518] mx-auto mt-4" />
                </div>

                <CardDeck items={MOCK_LIVE_AUCTIONS} />
            </div>
        </section>
    );
}