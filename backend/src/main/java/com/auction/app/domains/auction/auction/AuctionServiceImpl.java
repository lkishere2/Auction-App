package com.auction.app.domains.auction.auction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.app.domains.auction.auction.dtos.AuctionFindingRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionRequest;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.auction.model.Auction;
import com.auction.app.domains.auction.auction.model.AuctionStatus;
import com.auction.app.domains.auction.auction.redis.AuctionRedisPort;
import com.auction.app.domains.auction.exceptions.*;
import com.auction.app.domains.notifications.NotificationService;
import com.auction.app.domains.products.exceptions.ProductNotFoundException;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.infrastructure.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.app.domains.products.model.Product;
import com.auction.app.domains.products.ProductRepository;

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
    private final SecurityUtils securityUtils;

    @Transactional
    public AuctionResponse createAuction(AuctionRequest request) {

        // First, we fetch the current user
        User seller = securityUtils.getCurrentUser();

        // Then, we validate some info to start the auction
        validateTime(request.getStartTime(), request.getEndTime());
        Product product = findProductByIdAndOwnerId(request.getProductId(), seller.getId());
        validateQuantity(request.getQuantity(), product.getQuantity());

        // Update the quantity in the storage
        product.setQuantity(product.getQuantity() - request.getQuantity());
        productRepository.save(product);

        // Create new entity in DB, and save it
        Auction auction = mapToEntity(request, product, seller);
        Auction saved = auctionRepository.save(auction);

        // We also cache that for fast retrievement
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

        return AuctionResponse.from(saved);
    }

    @Transactional
    public AuctionResponse cancelAuction(Long auctionId) {

        // First, we fetch the user and the wanted auction
        User seller = securityUtils.getCurrentUser();
        Auction auction = findByIdWithDetails(auctionId);

        // Then, we validate some info
        validateUser(auction.getSeller(), seller);
        validateAuctionCancellation(auction);

        // Update the storage
        Product product = auction.getProduct();
        product.setQuantity(product.getQuantity() + auction.getAuctionedQuantity());
        productRepository.save(product);

        // Change the status of the auction to CANCELLED
        auction.setStatus(AuctionStatus.CANCELLED);
        AuctionResponse response = AuctionResponse.from(auction);
        auctionRepository.save(auction);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cache.cacheAuctionResponse(auctionId, response);
                        log.info("[Auction Service] Post-commit: Cache updated to CANCELLED for auction #{}", auctionId);
                    }
                }
        );

        return response;
    }

    public AuctionResponse getAuction(Long auctionId) {

        AuctionResponse cached = null;
        try {
            cached = cache.getAuctionResponse(auctionId);
            log.info("[Auction Service - Get Auction] Cache auction #{}", auctionId);
        } catch (Exception e) {
            log.error("[Auction Service - Get Auction] Failed to cache auction #{}, error: {}", auctionId, e.getMessage());
        }

        if (cached != null) return cached;

        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        AuctionResponse response = AuctionResponse.from(auction);
        log.info("[Auction Service - Get Auction] Cache miss, fallback to DB!");
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

    public Page<AuctionResponse> getMyAuctions(Pageable pageable) {
        long t0 = System.currentTimeMillis();

        long tSort = System.currentTimeMillis();
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "startTime")
        );
        log.info("[Auction Service - Get My Auctions] Sort pageable took {}ms",
                System.currentTimeMillis() - tSort);

        long tQuery = System.currentTimeMillis();
        Page<Long> idPage = auctionRepository.findIdsBySellerIdOrderByStartTime(
                securityUtils.getCurrentUserId(), sortedPageable);
        List<Long> ids = idPage.getContent();
        log.info("[Auction Service - Get My Auctions] ID query took {}ms, {} IDs returned",
                System.currentTimeMillis() - tQuery, ids.size());

        log.info("[Auction Service - Get My Auctions] ID query (from t0) took {}ms",
                System.currentTimeMillis() - t0);

        if (ids.isEmpty()) return Page.empty(pageable);

        long t1 = System.currentTimeMillis();
        List<AuctionResponse> cached = cache.getAuctionResponses(ids);
        log.info("[Auction Service - Get My Auctions] Redis MGET took {}ms",
                System.currentTimeMillis() - t1);

        Map<Long, Integer> idToIndex = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            if (cached.get(i) == null) missedIds.add(ids.get(i));
            idToIndex.put(ids.get(i), i);
        }
        log.info("[Auction Service - Get My Auctions] Cache hits: {}, misses: {}",
                ids.size() - missedIds.size(), missedIds.size());

        if (!missedIds.isEmpty()) {
            long t2 = System.currentTimeMillis();
            List<Auction> fetched = auctionRepository.findByIdsWithDetails(missedIds);
            log.info("[Auction Service - Get My Auctions] DB fallback took {}ms for {} misses",
                    System.currentTimeMillis() - t2, missedIds.size());

            Map<Long, AuctionResponse> toCache = new HashMap<>();
            for (Auction auction : fetched) {
                AuctionResponse response = AuctionResponse.from(auction);
                cached.set(idToIndex.get(auction.getId()), response);
                toCache.put(auction.getId(), response);
            }

            cache.cacheAuctionResponses(toCache);
        }

        log.info("[Auction Service - Get My Auctions] Total took {}ms",
                System.currentTimeMillis() - t0);
        return new PageImpl<>(cached, pageable, idPage.getTotalElements());
    }

    public void cacheAuctionResponse(Auction auction) {
        AuctionResponse response = AuctionResponse.from(auction);
        cache.cacheAuctionResponse(auction.getId(), response);
    }

    // Helpers
    private Auction findByIdWithDetails(Long auctionId) {
        return auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));
    }

    private Product findProductByIdAndOwnerId(Long productId, Long userId) {
        return productRepository.findByIdAndOwnerUserId(productId, userId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found in your inventory"));
    }

    private void validateTime(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidEndTimeException("End time must be after start time");
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