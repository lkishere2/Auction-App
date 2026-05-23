import type { Page } from './pagination';

export type Tag =
    | 'ELECTRONICS'
    | 'FOOD'
    | 'COLLECTIBLES'
    | 'FASHION'
    | 'JEWELRY'
    | 'ART'
    | 'VEHICLES'
    | 'SPORTS'
    | 'GARDENING'
    | 'GAMES'
    | 'ONLINE_ITEM'
    | 'OTHER';

export interface ProductRequest {
    productName: string;
    description?: string;
    quantity: number;
    productImageUrl?: string;
    tags?: Tag[] | Set<Tag>;
}

export interface ProductResponse {
    id: number;
    productName: string;
    description?: string;
    quantity: number;
    productImageUrl?: string;
    tags?: Tag[] | Set<Tag>;
    createdAt: string;
}

export type PagedProducts = Page<ProductResponse>;
