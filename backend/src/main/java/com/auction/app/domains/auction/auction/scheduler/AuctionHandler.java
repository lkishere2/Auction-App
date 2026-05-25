package com.auction.app.domains.auction.auction.scheduler;

import java.time.Instant;
import java.util.List;

import com.auction.app.domains.auction.auction.AuctionRepository;
import com.auction.app.domains.auction.auction.AuctionStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionHandler {

    private final AuctionRepository auctionRepository;
    private final AuctionExecutionService auctionExecutionService;

    @Scheduled(fixedRate = 100000)
    public void activateUpcomingAuctions() {

        List<Long> toActivateIds = auctionRepository.findUpcomingIdsToActivate(AuctionStatus.UPCOMING, Instant.now());
        if (toActivateIds.isEmpty()) {
            return;
        }

        // Efficiently update database statuses using a single UPDATE query
        auctionRepository.updateStatusForIds(toActivateIds, AuctionStatus.ACTIVE);

        for (Long auctionId : toActivateIds) {
            try {
                auctionExecutionService.processActiveAuctionById(auctionId);
            } catch (Exception e) {
                log.error("Failed to activate upcoming auction #{}: {}", auctionId, e.getMessage(), e);
            }
        }
    }

    // Same logic as the active
    @Scheduled(fixedRate = 100000)
    public void endActiveAuctions() {

        List<Long> toEndIds = auctionRepository.findActiveIdsToEnd(AuctionStatus.ACTIVE, Instant.now());
        if (toEndIds.isEmpty()) {
            return;
        }

        for (Long auctionId : toEndIds) {
            try {
                auctionExecutionService.processEndedAuctionById(auctionId);
            } catch (Exception e) {
                log.error("Failed to process ending for auction #{}: {}", auctionId, e.getMessage(), e);
            }
        }
    }
}