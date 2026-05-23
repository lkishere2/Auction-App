export interface BidRequest {
    amount: string; // BigDecimal -> string
}

export interface BidResponse {
    bidId: number;
    auctionId: number;
    bidderLabel: string;
    amount: string;
    placedAt: string; // ISO instant
}

export interface BidNotificationPayload {
    auctionId: number;
    currentPrice: string; // BigDecimal -> string
    minNextBid: string; // BigDecimal -> string
    bidderLabel: string;
    endTime: string; // ISO instant
    extended: boolean;
    bidCount: number;
    ended: boolean;
}

export interface PendingBid {
    bidId: number;
    auctionId: number;
    bidderId: number;
    bidderLabel: string;
    amount: string; // BigDecimal -> string
    placedAt: string; // ISO instant
}
