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
