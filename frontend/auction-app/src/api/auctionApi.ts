import api from './axios';
import type { AuctionRequest, AuctionResponse, AuctionFindingRequest } from '../types/auction';
import type { Page } from '../types/pagination';

export const auctionApi = {

    createAuction: (data: AuctionRequest) =>
        api.post<AuctionResponse>('/auctions/create', data),

    cancelAuction: (auctionId: number) =>
        api.delete<AuctionResponse>(`/auctions/cancel/${auctionId}`),

    getAuction: (auctionId: number) =>
        api.get<AuctionResponse>(`/auctions/get/${auctionId}`),

    getDiscoverableAuctions: (data: AuctionFindingRequest, page: number, size: number) =>
        api.get<Page<AuctionResponse>>(`/auctions/discover?page=${page}&size=${size}`, {
            data: data
        }),

    getMyAuctions: () =>
        api.get<AuctionResponse[]>('/auctions/me'),

};

export default auctionApi;
