package com.auction.app.domains.auction.auction.scheduler;

import java.time.Instant;
import java.util.List;

import com.auction.app.domains.auction.auction.*;
import com.auction.app.domains.auction.auction.dtos.AuctionResponse;
import com.auction.app.domains.auction.auction.notification.AuctionPublisher;
import com.auction.app.domains.auction.auction.redis.AuctionCacheAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.auction.app.domains.auction.bids.Bid;
import com.auction.app.domains.auction.bids.BidRepository;
import com.auction.app.domains.auction.bids.BidStatus;
import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionHandler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuctionCacheAdapter auctionCacheAdapter;
    private final AuctionService auctionService;
    private final AuctionPublisher publisher;

    @Scheduled(fixedRate = 10000)
    public void activateUpcomingAuctions() {

        List<Auction> toActivate = auctionRepository.findUpcomingToActivate(AuctionStatus.UPCOMING, Instant.now());

        // Return immediately if is empty
        if (toActivate.isEmpty()) {
            return;
        }

        for (Auction auction : toActivate) {
            try {
                processActiveAuction(auction);
            } catch (Exception e) {
                log.error("Failed to activate upcoming auction #{}: {}", auction.getId(), e.getMessage(), e);
            }
        }
    }

    // Same logic as the active
    @Scheduled(fixedRate = 10000)
    public void endActiveAuctions() {

        List<Auction> toEnd = auctionRepository.findActiveToEnd(AuctionStatus.ACTIVE, Instant.now());

        if (toEnd.isEmpty()) {
            return;
        }

        for (Auction auction : toEnd) {
            try {
                processEndedAuction(auction);
            } catch (Exception e) {
                log.error("Failed to process ending for auction #{}: {}", auction.getId(), e.getMessage(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processActiveAuction(Auction auction) {
        auction.setStatus(AuctionStatus.ACTIVE);
        auctionRepository.save(auction);

        // Update or create cache response, flip to ACTIVE
        AuctionResponse response = auctionCacheAdapter.getAuctionResponse(auction.getId());

        if (response != null) {
            // Cache exists, update it
            response.setStatus(AuctionStatus.ACTIVE);
            auctionCacheAdapter.updateAuctionResponse(auction.getId(), response);
        }
        else {
            // Cache doesn't exist.
            // Since 'auction' was just set to ACTIVE on the line above,
            // caching it now will automatically create it with an ACTIVE status!
            auctionService.cacheAuctionResponse(auction);
        }

        // Notify all subscribers that auction is now ACTIVE
        publisher.publishAuctionStarted(auction);
        log.info("Auction #{} is now ACTIVE", auction.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEndedAuction(Auction auction) {
        // Sync final result
        AuctionResponse response = auctionCacheAdapter.getAuctionResponse(auction.getId());
        if (response != null) {
            auction.setCurrentPrice(response.getCurrentPrice());
            auction.setBidCount(response.getBidCount());
            auction.setEndTime(response.getEndTime());
        }

        // Handle winner or no bids
        if (auction.getBidCount() == 0) {
            handleNoBids(auction);
        }
        else {
            handleWinner(auction);
        }

        // Update and save
        auction.setStatus(AuctionStatus.ENDED);
        auctionRepository.save(auction);

        // Clear the cache post-commit
        Long auctionId = auction.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auctionCacheAdapter.clearAuctionCache(auctionId);
            }
        });

        // And finally, notify to user
        String winnerLabel = auction.getWinner() != null
                ? auction.getWinner().getDisplayName() + " #" + auction.getWinner().getId()
                : null;
        publisher.publishAuctionEnded(auction.getId(), winnerLabel, auction.getCurrentPrice(), auction.getBidCount(), auction.getEndTime());

        log.info("Auction #{} ENDED — winner: {}", auction.getId(), winnerLabel);
    }

    // Helpers

    private void handleNoBids(Auction auction) {
        // Restore quantity back to the seller's product
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