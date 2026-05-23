import type { Tag } from './product';

export type AuctionStatus = 'UPCOMING' | 'ACTIVE' | 'ENDED' | 'CANCELLED';

export interface AuctionRequest {
    productId: number;
    quantity: number;
    startingPrice: string;
    startTime: string;
    endTime: string;
}

export interface AuctionResponse {
    id: number;
    sellerLabel: string;
    productId: number;
    productName: string;
    productTags: Tag[];
    productDescription?: string;
    productImageUrl?: string;
    auctionedQuantity: number;
    startingPrice: string;
    currentPrice: string;
    minBidIncrement: string;
    startTime: string;
    endTime: string;
    extended: boolean;
    status: AuctionStatus;
    winnerLabel?: string;
    bidCount: number;
}