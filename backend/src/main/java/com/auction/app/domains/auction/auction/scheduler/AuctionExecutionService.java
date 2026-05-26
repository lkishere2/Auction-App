package com.auction.app.domains.auction.auction.scheduler;

import com.auction.app.domains.auction.auction.*;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.auction.model.Auction;
import com.auction.app.domains.auction.auction.model.AuctionStatus;
import com.auction.app.domains.auction.auction.notification.AuctionPublisher;
import com.auction.app.domains.auction.auction.redis.AuctionCacheAdapter;
import com.auction.app.domains.auction.exceptions.AuctionNotFoundException;
import com.auction.app.domains.auction.bids.model.Bid;
import com.auction.app.domains.auction.bids.BidRepository;
import com.auction.app.domains.auction.bids.model.BidStatus;
import com.auction.app.domains.products.model.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionExecutionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuctionCacheAdapter auctionCacheAdapter;
    private final AuctionService auctionService;
    private final AuctionPublisher publisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processActiveAuctionById(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found: " + auctionId));

        AuctionResponse response = null;
        try {
            response = auctionCacheAdapter.getAuctionResponse(auction.getId());
            log.info("[Auction Execution Service - Activate Auction] Cache auction #{}", auctionId);
        } catch (Exception e) {
            log.error("[Auction Execution Service - Activate Auction] Failed to cache auction #{}, errors: {}", auctionId, e.getMessage());
        }


        if (response != null) {
            log.info("[Auction Execution Service - Activate Auction] Cache auction #{} to active", auctionId);
            response.setStatus(AuctionStatus.ACTIVE);
            auctionCacheAdapter.updateAuctionResponse(auction.getId(), response);
        }
        else {
            log.info("[Auction Execution Service - Activate Auction] Auction #{} not exist in cache, fall back to DB!", auctionId);
            auction.setStatus(AuctionStatus.ACTIVE);
            auctionService.cacheAuctionResponse(auction);
        }

        publisher.publishAuctionStarted(auction);
        log.info("Auction #{} is now ACTIVE", auction.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEndedAuctionById(Long auctionId) {

        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found: " + auctionId));

        AuctionResponse response = null;
        try {
            response = auctionCacheAdapter.getAuctionResponse(auction.getId());
            log.info("[Auction Execution Service - End Auction] Cache auction #{}", auctionId);
        } catch (Exception e) {
            log.error("[Auction Execution Service - End Auction] Failed to cache auction #{}, errors: {}", auctionId, e.getMessage());
        }

        if (response != null) {
            auction.setCurrentPrice(response.getCurrentPrice());
            auction.setBidCount(response.getBidCount());
            auction.setEndTime(response.getEndTime());
            response.setStatus(AuctionStatus.ENDED);
            auctionCacheAdapter.updateAuctionResponse(auction.getId(), response);
            log.info("[Auction Execution Service - End Auction] Cache auction #{} success to update", auctionId);
        }

        if (auction.getBidCount() == 0) {
            handleNoBids(auction);
        }
        else {
            handleWinner(auction);
        }

        auction.setStatus(AuctionStatus.ENDED);
        auctionRepository.save(auction);

        Long id = auction.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auctionCacheAdapter.clearAuctionCache(id);
            }
        });

        String winnerLabel = auction.getWinner() != null
                ? auction.getWinner().getDisplayName() + " #" + auction.getWinner().getId()
                : null;
        publisher.publishAuctionEnded(auction.getId(), winnerLabel, auction.getCurrentPrice(), auction.getBidCount(), auction.getEndTime());

        log.info("Auction #{} ENDED — winner: {}", auction.getId(), winnerLabel);
    }

    // Helpers

    private void handleNoBids(Auction auction) {
        Product product = auction.getProduct();
        product.setQuantity(product.getQuantity() + auction.getAuctionedQuantity());
        productRepository.save(product);
        log.info("Auction #{} ended with no bids — quantity restored to product #{}", auction.getId(), product.getId());
    }

    private void handleWinner(Auction auction) {

        Bid winningBid = bidRepository.findByAuctionIdAndStatus(auction.getId(), BidStatus.HELD).orElse(null);

        if (winningBid == null) {
            log.warn("Auction #{} has bidCount > 0 but no HELD bid found", auction.getId());
            handleNoBids(auction);
            return;
        }

        User winner = winningBid.getBidder();
        User seller = auction.getSeller();

        // Flip bid to WON and transfer balance
        winningBid.setStatus(BidStatus.WON);
        bidRepository.save(winningBid);

        winner.setBalance(winner.getBalance().subtract(winningBid.getAmount()));
        seller.setBalance(seller.getBalance().add(winningBid.getAmount()));
        userRepository.save(winner);
        userRepository.save(seller);

        // Transfer auctionedQuantity to winner's product inventory
        Product soldProduct = auction.getProduct();
        Product winnerProduct = productRepository
                .findByIdAndOwnerUserId(soldProduct.getId(), winner.getId())
                .orElseGet(() -> Product.builder()
                        // Winner doesn't own this product yet — create a new entry
                        .productName(soldProduct.getProductName())
                        .description(soldProduct.getDescription())
                        .tags(soldProduct.getTags())
                        .owner(winner)
                        .build()
                );

        winnerProduct.setQuantity(winnerProduct.getQuantity() + auction.getAuctionedQuantity());
        productRepository.save(winnerProduct);

        auction.setWinner(winner);

        log.info("Auction #{} — winner #{} paid ${}, seller #{} credited. {} units transferred.",
                auction.getId(), winner.getId(), winningBid.getAmount(),
                seller.getId(), auction.getAuctionedQuantity());
    }
}