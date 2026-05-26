import CardDeck from "./CardDesk";

const MOCK_LIVE_AUCTIONS = [
  {
    id: "1",
    title: "Rolex Submariner Date",
    image: "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&q=80&w=600",
    currentBid: 14200,
    endTime: "2h 15m",
    bidCount: 24,
    timeLeft: "2h 15m",
    bidsCount: 24,
  },
  {
    id: "2",
    title: "2021 Porsche 911 GT3",
    image: "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?auto=format&fit=crop&q=80&w=600",
    currentBid: 185000,
    endTime: "4h 45m",
    bidCount: 42,
    timeLeft: "4h 45m",
    bidsCount: 42,
  },
  {
    id: "3",
    title: "Hermès Birkin 30 Black Togo",
    image: "https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&q=80&w=600",
    currentBid: 21500,
    endTime: "1h 30m",
    bidCount: 18,
    timeLeft: "1h 30m",
    bidsCount: 18,
  },
];

export default function AuctionPreview() {
  return (
    // Bổ sung thuộc tính id tại đây để Welcome component có thể scroll tới
    <section id="live-showroom-auctions" className="relative z-10 pt-32 pb-20 bg-gradient-to-b from-transparent via-white to-white">
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