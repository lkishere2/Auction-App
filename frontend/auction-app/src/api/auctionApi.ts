import api from './axios';
import type { AuctionRequest, AuctionResponse } from '../types/auction';

export const auctionApi = {

    createAuction: (data: AuctionRequest) => api.post<AuctionResponse>('/auctions', data),

    cancelAuction: (auctionId: number) => api.delete<AuctionResponse>(`/auctions/${auctionId}`),

    getAuction: (auctionId: number) => api.get<AuctionResponse>(`/auctions/${auctionId}`),

    getActive: () => api.get<AuctionResponse[]>('/auctions/active'),

    getUpcoming: () => api.get<AuctionResponse[]>('/auctions/upcoming'),

    getMyAuctions: () => api.get<AuctionResponse[]>('/auctions/my'),

};

export default auctionApi;
