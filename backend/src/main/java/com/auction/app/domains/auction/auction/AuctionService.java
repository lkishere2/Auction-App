package com.auction.app.domains.auction.auction;

import java.util.List;

import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.auction.dtos.AuctionState;
import com.auction.app.domains.auction.auction.exception.*;
import com.auction.app.domains.auction.auction.redis.AuctionCacheAdapter;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import com.auction.app.domains.transaction.exceptions.AuthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.users.users.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AuctionCacheAdapter auctionCacheAdapter;

    @Transactional
    public AuctionResponse createAuction(AuctionRequest request) {
        // Validate the time
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidEndTimeException("End time must be after start time");
        }

        // Verify if the product is in user's inventory
        User currentSeller = getCurrentUser();
        Product product = productRepository.findByIdAndOwnerUserId(request.getProductId(), currentSeller.getId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found in your inventory"));

        // We can only create auction with one type of product once at a time
        // Also, we verify that if the auction is UPCOMING or ACTIVE
        boolean hasActiveOrUpcomingAuction = auctionRepository.existsByProduct_IdAndStatusIn(
                product.getId(), List.of(AuctionStatus.UPCOMING, AuctionStatus.ACTIVE));
        if (hasActiveOrUpcomingAuction) {
            throw new ListedProductException("Product is already listed in another active or upcoming auction");
        }

        // Validate the quantity
        if (request.getQuantity() > product.getQuantity()) {
            throw new InvalidProductQuantity(
                    "Requested quantity (" + request.getQuantity() + ") exceeds available stock (" + product.getQuantity() + ")"
            );
        }

        // After that, we update the item in our storage
        product.setQuantity(product.getQuantity() - request.getQuantity());
        productRepository.save(product);

        // Create new auction, and save to DB
        Auction auction = mapToEntity(request, currentSeller, product);
        Auction saved = auctionRepository.save(auction);

        // We use a helper here to convert DB's entity -> Redis's entity
        // And save that with auction id as a key and state as a value
        cacheAuctionState(saved);

        // Return the response to the user
        return AuctionResponse.from(saved);
    }

    @Transactional
    public AuctionResponse cancelAuction(Long auctionId) {
        // To cancel the auction, first we need to check if that auction exist or not
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        // After that, we need to verify if the current user is the one who hold the auction
        User currentSeller = getCurrentUser();
        if (!auction.getSeller().getId().equals(currentSeller.getId())) {
            throw new AuthorizedException("You are not the seller of this auction");
        }

        // We can only cancel UPCOMING auction only
        // When it's ACTIVE, the money go on, and we shouldn't cancel that
        if (auction.getStatus() != AuctionStatus.UPCOMING) {
            throw new NotUpcommingAuctionException("Only UPCOMING auctions can be cancelled");
        }

        // Update the storage
        Product product = auction.getProduct();
        product.setQuantity(product.getQuantity() + auction.getAuctionedQuantity());
        productRepository.save(product);

        // Change the status of the auction to CANCELLED
        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);

        // Clear the cache here for RAM saving
        auctionCacheAdapter.clearAuctionCache(auctionId);

        // Return the response to the user
        return AuctionResponse.from(saved);
    }

    // TODO: Ok, so the problem here is that we still hit the DB
    // We cannot fetch immediately from cache because the response depends on DB's Entity
    // I might think another solution later, lol
    public AuctionResponse getAuction(Long auctionId) {
        // First, we verify if the auction exist
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        // We fetch from cache first for fast retrievement
        AuctionState state = auctionCacheAdapter.getAuctionState(auctionId);

        if (state != null && state.getStatus() == AuctionStatus.ACTIVE) {
            return AuctionResponse.fromWithState(auction, state);
        }

        // If the cache miss, then map from DB's entity to response
        return AuctionResponse.from(auction);
    }

    public List<AuctionResponse> getActiveAuctions() {
        return getAuctionsByStatus(AuctionStatus.ACTIVE);
    }

    public List<AuctionResponse> getUpcomingAuctions() {
        return getAuctionsByStatus(AuctionStatus.UPCOMING);
    }

    private List<AuctionResponse> getAuctionsByStatus(AuctionStatus status) {
        return auctionRepository.findByStatusWithDetails(status)
                .stream()
                .map(auction -> {
                    AuctionState state = auctionCacheAdapter.getAuctionState(auction.getId());
                    return state != null
                            ? AuctionResponse.fromWithState(auction, state)
                            : AuctionResponse.from(auction);
                })
                .toList();
    }

    public List<AuctionResponse> getMyAuctions() {
        User currentSeller = getCurrentUser();
        return auctionRepository.findBySellerIdWithDetails(currentSeller.getId())
                .stream()
                .map(auction -> {
                    AuctionState state = auctionCacheAdapter.getAuctionState(auction.getId());
                    return state != null
                            ? AuctionResponse.fromWithState(auction, state)
                            : AuctionResponse.from(auction);
                })
                .toList();
    }

    public void cacheAuctionState(Auction auction) {
        AuctionState state = AuctionState.builder()
                .auctionId(auction.getId())
                .sellerId(auction.getSeller().getId())
                .currentPrice(auction.getCurrentPrice())
                .minBidIncrement(auction.getMinBidIncrement())
                .endTime(auction.getEndTime())
                .bidCount(auction.getBidCount())
                .winnerId(null)
                .winnerLabel(null)
                .status(auction.getStatus())
                .build();
        auctionCacheAdapter.cacheAuctionState(auction.getId(), state);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private Auction mapToEntity(AuctionRequest request, User currentSeller, Product product) {
        Auction auction = Auction.builder()
                .seller(currentSeller)
                .product(product)
                .auctionedQuantity(request.getQuantity())
                .startingPrice(request.getStartingPrice())
                .currentPrice(request.getStartingPrice())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        auction.recalculateMinBidIncrement();
        return auction;
    }
}