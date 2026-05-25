import api from './axios';
import type { TransactionRequest, TransactionResponse, ClientRequest } from '../types/transaction';
import type { Page } from '../types/pagination';

export const transactionApi = {

    getUserTransactions: (page = 0, size = 10) =>
        api.get<Page<TransactionResponse>>(`/transaction/me?page=${page}&size=${size}`),

    createTransaction: (data: TransactionRequest) =>
        api.post<TransactionResponse>('/transaction/create', data),

    deleteTransaction: (id: number) =>
        api.delete<void>(`/transaction/delete/${id}`),

    getAllTransactionRequests: (page = 0, size = 10) =>
        api.get<Page<TransactionResponse>>(`/transaction/all?page=${page}&size=${size}`),

    acceptTransaction: (data: ClientRequest) =>
        api.post<void>('/transaction/deposit', data),

    cancelTransaction: (id: number) =>
        api.put<void>(`/transaction/cancel/${id}`),

};

export default transactionApi;
