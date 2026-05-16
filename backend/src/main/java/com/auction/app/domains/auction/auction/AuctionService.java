package com.auction.app.domains.auction.auction;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.app.domains.exceptions.AuctionNotFoundException;
import com.auction.app.domains.exceptions.InvalidAuctionStateException;
import com.auction.app.domains.exceptions.ItemNotAvailableException;
import com.auction.app.domains.exceptions.UnauthorizedActionException;
import com.auction.app.domains.inventory.Item;
import com.auction.app.domains.inventory.ItemStatus;
import com.auction.app.domains.inventory.ItemStorageRepository;
import com.ltnc.auction.domain.user.User;
import com.ltnc.auction.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemStorageRepository itemStorageRepository;
    private final UserRepository userRepository;
    private final AuctionCacheAdapter auctionCacheAdapter; // ← new

    @Transactional
    public AuctionResponse createAuction(Long sellerId, AuctionRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidAuctionStateException("End time must be after start time");
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Item item = itemStorageRepository.findByIdAndOwnerUserId(request.itemId(), sellerId)
                .orElseThrow(() -> new ItemNotAvailableException("Item not found in your inventory"));

        boolean hasActiveOrUpcomingAuction = auctionRepository.existsByItem_IdAndStatusIn(
                item.getId(), List.of(AuctionStatus.UPCOMING, AuctionStatus.ACTIVE));

        if (hasActiveOrUpcomingAuction) {
            throw new ItemNotAvailableException("Item is already listed in another active or upcoming auction");
        }

        if (item.getStatus() != ItemStatus.AVAILABLE) {
            item.setStatus(ItemStatus.AVAILABLE);
        }

        item.setStatus(ItemStatus.LISTED);
        itemStorageRepository.save(item);

        Auction auction = new Auction();
        auction.setSeller(seller);
        auction.setItem(item);
        auction.setStartingPrice(request.startingPrice());
        auction.setCurrentPrice(request.startingPrice());
        auction.recalculateMinBidIncrement();
        auction.setStartTime(request.startTime());
        auction.setEndTime(request.endTime());

        Auction saved = auctionRepository.save(auction);

        // cache UPCOMING state in Redis immediately
        // so it's ready the moment scheduler flips it to ACTIVE
        cacheAuctionState(saved);

        return AuctionResponse.from(saved);
    }

    @Transactional
    public AuctionResponse cancelAuction(Long sellerId, Long auctionId) {
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        if (!auction.getSeller().getUserId().equals(sellerId)) {
            throw new UnauthorizedActionException("You are not the seller of this auction");
        }

        if (auction.getStatus() != AuctionStatus.UPCOMING) {
            throw new InvalidAuctionStateException("Only UPCOMING auctions can be cancelled");
        }

        auction.getItem().setStatus(ItemStatus.AVAILABLE);
        itemStorageRepository.save(auction.getItem());

        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);

        // clear from Redis — no longer needed
        auctionCacheAdapter.clearAuctionCache(auctionId);

        return AuctionResponse.from(saved);
    }

    public AuctionResponse getAuction(Long auctionId) {
        // check Redis first for ACTIVE auctions — faster
        AuctionState state = auctionCacheAdapter.getAuctionState(auctionId);

        if (state != null && state.getStatus() == AuctionStatus.ACTIVE) {
            // get base auction from DB but overlay hot fields from Redis
            Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                    .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));
            return AuctionResponse.fromWithState(auction, state);
        }

        // fallback to DB for UPCOMING/ENDED/CANCELLED
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));
        return AuctionResponse.from(auction);
    }

    public List<AuctionResponse> getActiveAuctions() {
        return auctionRepository.findByStatusWithDetails(AuctionStatus.ACTIVE)
                .stream()
                .map(auction -> {
                    AuctionState state = auctionCacheAdapter.getAuctionState(auction.getId());
                    return state != null
                        ? AuctionResponse.fromWithState(auction, state)
                        : AuctionResponse.from(auction);
                })
                .toList();
    }

    public List<AuctionResponse> getUpcomingAuctions() {
        return auctionRepository.findByStatusWithDetails(AuctionStatus.UPCOMING)
                .stream()
                .map(AuctionResponse::from)
                .toList();
    }

    public List<AuctionResponse> getMyAuctions(Long sellerId) {
        return auctionRepository.findBySellerUserIdWithDetails(sellerId)
                .stream()
                .map(auction -> {
                    AuctionState state = auctionCacheAdapter.getAuctionState(auction.getId());
                    return state != null
                        ? AuctionResponse.fromWithState(auction, state)
                        : AuctionResponse.from(auction);
                })
                .toList();
    }

    // ─────────────────────────────────────────
    // helper — build and cache AuctionState from entity
    // ─────────────────────────────────────────
    public void cacheAuctionState(Auction auction) {
        AuctionState state = AuctionState.builder()
                .auctionId(auction.getId())
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
}