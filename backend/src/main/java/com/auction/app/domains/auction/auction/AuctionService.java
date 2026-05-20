package com.auction.app.domains.auction.auction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
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
        // Validate every info first
        validateTime(request.getStartTime(), request.getEndTime());
        Product product = findProductByIdAndOwnerId(request.getProductId(), currentUser().getId());
        validateProductAvailability(product);
        validateQuantity(request.getQuantity(), product.getQuantity());

        // After that, we update the item in our storage
        product.setQuantity(product.getQuantity() - request.getQuantity());
        productRepository.save(product);

        // Create new auction, and save to DB
        Auction auction = mapToEntity(request, product);
        Auction saved = auctionRepository.save(auction);

        // This helper convert DB's entity to Redis's entity, and then cache it
        cacheAuctionResponse(saved);

        // Return the response to the user
        return AuctionResponse.from(saved);
    }

    @Transactional
    public AuctionResponse cancelAuction(Long auctionId) {

        Auction auction = findByIdWithDetails(auctionId);

        // Validation
        validateUser(auction.getSeller());
        validateAuctionCancellation(auction);

        // Update the storage
        Product product = auction.getProduct();
        product.setQuantity(product.getQuantity() + auction.getAuctionedQuantity());
        productRepository.save(product);

        // Change the status of the auction to CANCELLED
        auction.setProduct(null);
        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);

        // Clear the cache here
        auctionCacheAdapter.clearAuctionCache(auctionId);

        // Return the response to the user
        return AuctionResponse.from(saved);
    }

    public AuctionResponse getAuction(Long auctionId) {

        // We fetch from cache first for fast retrievement
        AuctionResponse cached = auctionCacheAdapter.getAuctionResponse(auctionId);
        if (cached != null) {
            return cached;
        }

        // If the cache miss, then map from DB's entity to response
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        AuctionResponse response = AuctionResponse.from(auction);
        auctionCacheAdapter.cacheAuctionResponse(auctionId, response);

        return response;
    }

    public List<AuctionResponse> getAuctionsBidOnByCurrentUser(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) return List.of();

        return auctionRepository.findByIdsWithDetails(auctionIds)
                .stream()
                .map(AuctionResponse::from)
                .toList();
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
                .map(AuctionResponse::from)
                .toList();
    }

    public List<AuctionResponse> getMyAuctions() {
        return auctionRepository.findBySellerIdWithDetails(currentUser().getId())
                .stream()
                .map(AuctionResponse::from)
                .toList();
    }

    public void cacheAuctionResponse(Auction auction) {
        AuctionResponse response = AuctionResponse.from(auction);
        auctionCacheAdapter.cacheAuctionResponse(auction.getId(), response);
    }

    // Helpers
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private void validateTime(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidEndTimeException("End time must be after start time");
        }
    }

    private Product findProductByIdAndOwnerId(Long productId, Long userId) {
        return productRepository.findByIdAndOwnerUserId(productId, userId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found in your inventory"));
    }

    private void validateProductAvailability(Product product) {
        boolean isListed = auctionRepository.existsByProduct_IdAndStatusIn(product.getId(), List.of(AuctionStatus.UPCOMING, AuctionStatus.ACTIVE));
        if (isListed) {
            throw new ListedProductException("Product is already listed in another active or upcoming auction");
        }
    }

    private void validateQuantity(Integer requestedQuantity, Integer currentQuantity) {
        if (requestedQuantity > currentQuantity) {
            throw new InvalidProductQuantity(
                    "Requested quantity (" + requestedQuantity + ") exceeds available stock (" + currentQuantity + ")"
            );
        }
    }

    private void validateUser(User seller) {
        if (seller.getId().equals(currentUser().getId())) {
            throw new AuthorizedException("You are not the seller of this auction");
        }
    }

    private void validateAuctionCancellation(Auction auction) {
        if (auction.getStatus() != AuctionStatus.UPCOMING) {
            throw new NotUpcommingAuctionException("Only UPCOMING auctions can be cancelled");
        }
    }

    private Auction findByIdWithDetails(Long auctionId) {
        return auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));
    }

    private Auction mapToEntity(AuctionRequest request, Product product) {
        BigDecimal startPrice = request.getStartingPrice();
        BigDecimal initialIncrement = startPrice.multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);

        Auction auction = Auction.builder()
                .seller(currentUser())
                .product(product)
                .auctionedQuantity(request.getQuantity())
                .startingPrice(startPrice)
                .currentPrice(startPrice)
                .minBidIncrement(initialIncrement)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        auction.recalculateMinBidIncrement();
        return auction;
    }
}