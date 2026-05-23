export interface Page<T> {
    items: T[];
    totalItems: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNextPage: boolean;
    hasPreviousPage: boolean;
}

export interface Slice<T> {
    items: T[];
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
    nextCursor?: string | null;
    previousCursor?: string | null;
}