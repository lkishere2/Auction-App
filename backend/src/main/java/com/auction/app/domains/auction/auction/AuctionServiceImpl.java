package com.auction.app.domains.auction.auction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.auction.redis.AuctionRedisPort;
import com.auction.app.domains.auction.exceptions.*;
import com.auction.app.domains.notifications.NotificationService;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.users.users.User;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AuctionRedisPort cache;
    private final NotificationService notificationService;

    @Transactional
    public AuctionResponse createAuction(AuctionRequest request) {
        User seller = currentUser();

        // Validate every info first
        validateTime(request.getStartTime(), request.getEndTime());
        Product product = findProductByIdAndOwnerId(request.getProductId(), seller.getId());
        validateProductAvailability(product);
        validateQuantity(request.getQuantity(), product.getQuantity());

        // After that, we update the item in our storage
        product.setQuantity(product.getQuantity() - request.getQuantity());
        productRepository.save(product);

        // Create new auction, and save to DB
        Auction auction = mapToEntity(request, product, seller);
        Auction saved = auctionRepository.save(auction);

        // This helper convert DB's entity to Redis's entity, and then cache it
        cacheAuctionResponse(saved);

        // Notify to all user
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.notifyFollowersOfNewAuction(seller);
                    }
                }
        );

        // Return the response to the user
        return AuctionResponse.from(saved);
    }

    @Transactional
    public AuctionResponse cancelAuction(Long auctionId) {
        User seller = currentUser();
        Auction auction = findByIdWithDetails(auctionId);

        // Validation
        validateUser(auction.getSeller(), seller);
        validateAuctionCancellation(auction);

        // Update the storage
        Product product = auction.getProduct();
        product.setQuantity(product.getQuantity() + auction.getAuctionedQuantity());
        productRepository.save(product);

        // Change the status of the auction to CANCELLED
        auction.setStatus(AuctionStatus.CANCELLED);
        AuctionResponse response = AuctionResponse.from(auction);

        auction.setProduct(null);
        auctionRepository.saveAndFlush(auction);

        cache.clearAuctionCache(auctionId);

        return response;
    }

    @Transactional(readOnly = true)
    public AuctionResponse getAuction(Long auctionId) {

        AuctionResponse cached = cache.getAuctionResponse(auctionId);

        if (cached != null) {
            log.info("Auction has been cached for {}", auctionId);
            return cached;
        }

        log.info("Cache miss, fallback to DB");

        // If the cache miss, then map from DB's entity to response
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        AuctionResponse response = AuctionResponse.from(auction);
        cache.cacheAuctionResponse(auctionId, response);

        return response;
    }

    public List<AuctionResponse> getAuctionsBidOnByCurrentUser(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) return List.of();

        return auctionRepository.findByIdsWithDetails(auctionIds)
                .stream()
                .map(AuctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuctionResponse> getDiscoverableAuctions(AuctionFindingRequest request, Pageable pageable) {

        // Map tag enums to string values for native query execution
        List<String> tagStrings = (request.getTags() != null) ? request.getTags().stream().map(Enum::name).toList() : List.of();

        boolean hasTags = !tagStrings.isEmpty();
        String statusString = (request.getStatus() != null) ? request.getStatus().name() : null;

        return auctionRepository.findAuctions(request.getProductName(), tagStrings, hasTags,
                request.getStartTime(), request.getEndTime(), request.getMinStartingPrice(),
                statusString, pageable
        ).map(AuctionResponse::from);
    }

    public List<AuctionResponse> getMyAuctions() {
        return auctionRepository.findBySellerIdWithDetails(currentUser().getId())
                .stream()
                .map(AuctionResponse::from)
                .toList();
    }

    public void cacheAuctionResponse(Auction auction) {
        AuctionResponse response = AuctionResponse.from(auction);
        cache.cacheAuctionResponse(auction.getId(), response);
    }

    // Helpers
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User is not authenticated");
        }
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

    private void validateUser(User seller, User currentUser) {
        if (!seller.getId().equals(currentUser.getId())) {
            throw new BadCredentialsException("You are not the seller of this auction");
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

    private Auction mapToEntity(AuctionRequest request, Product product, User seller) {
        BigDecimal startPrice = request.getStartingPrice();
        BigDecimal initialIncrement = startPrice.multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);

        Auction auction = Auction.builder()
                .seller(seller)
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