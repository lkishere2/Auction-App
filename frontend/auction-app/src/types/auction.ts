import type { Tag } from './product';

export type AuctionStatus = 'UPCOMING' | 'ACTIVE' | 'ENDED' | 'CANCELLED';

export interface AuctionRequest {
    productId: number;
    quantity: number;
    // BigDecimal -> string
    startingPrice: string;
    startTime: string; // ISO instant
    endTime: string; // ISO instant
}

export interface AuctionResponse {
    id: number;
    sellerLabel: string;
    productId: number;
    productName: string;
    productTags?: Tag[];
    productDescription?: string;
    productImageUrl?: string;
    auctionedQuantity: number;
    startingPrice: string;
    currentPrice?: string;
    minBidIncrement?: string;
    startTime: string;
    endTime: string;
    extended: boolean;
    status: AuctionStatus;
    winnerLabel?: string;
    bidCount?: number;
}
