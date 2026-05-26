export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL';
export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

export interface TransactionRequest {
    amount: string;
    type: TransactionType;
}

export interface TransactionResponse {
    amount: string;
    type: TransactionType;
    status: TransactionStatus;
    createdAt: string;
}

export interface ClientRequest {
    transactionId: number;
    userId: number;
    username: string;
    email: string;
    amount: string;
    type: TransactionType;
    createdAt: string;
}
