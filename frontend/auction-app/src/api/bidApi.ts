import api from './axios';
import type { BidResponse } from '../types/bid';
import type { AuctionResponse } from '../types/auction';

export const bidApi = {

    getBidHistory: (auctionId: number) =>
        api.get<BidResponse[]>(`/auctions/${auctionId}/bids`),

    getAuctionsBidOn: () =>
        api.get<AuctionResponse[]>('/auctions/bids/me'),

};

export default bidApi;
