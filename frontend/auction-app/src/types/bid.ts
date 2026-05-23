export interface BidRequest {
    amount: string;
}

export interface BidResponse {
    bidId: number;
    auctionId: number;
    bidderLabel: string;
    amount: string;
    placedAt: string;
}

export interface BidNotificationPayload {
    auctionId: number;
    currentPrice: string;
    minNextBid: string;
    bidderLabel: string;
    endTime: string;
    extended: boolean;
    bidCount: number;
    ended: boolean;
}

export interface PendingBid {
    bidId: number;
    auctionId: number;
    bidderId: number;
    bidderLabel: string;
    amount: string;
    placedAt: string;
}
