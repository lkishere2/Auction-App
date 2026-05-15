package com.auction.app.domains.auction.auction;

import com.auction.app.domains.products.Product;
import com.auction.app.domains.products.ProductRepository;
import com.auction.app.domains.users.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String STATE_PREFIX = "auction:state:";
    private static final String QUEUE_PREFIX = "auction:queue:";

    @Transactional
    public AuctionResponse createAuction(AuctionRequest request) {

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient product stock available. Requested: "
                    + request.getQuantity() + ", Available: " + product.getQuantity());
        }

        product.setQuantity(product.getQuantity() - request.getQuantity());
        productRepository.save(product);

        User seller = getCurrentUser();

        Auction newAuction = new Auction();
        newAuction.setSeller(seller);
        newAuction.setProduct(product);
        newAuction.setQuantity(request.getQuantity());
        newAuction.setStartingPrice(request.getStartingPrice());
        newAuction.setCurrentPrice(request.getStartingPrice());
        newAuction.setStartTime(request.getStartTime());
        newAuction.setEndTime(request.getEndTime());
        newAuction.setStatus(AuctionStatus.UPCOMING);
        newAuction.setBidCount(0);
        newAuction.recalculateMinBidIncrement();

        Auction savedAuction = auctionRepository.save(newAuction);

        return mapToResponse(savedAuction);
    }

    @Transactional
    public AuctionResponse cancelAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (!auction.getSeller().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("You are not the seller of this auction");
        }

        if (auction.getStatus() != AuctionStatus.UPCOMING) {
            throw new RuntimeException("Only UPCOMING auctions can be cancelled");
        }

        Product product = auction.getProduct();
        if (product != null) {
            product.setQuantity(product.getQuantity() + auction.getQuantity());
            productRepository.save(product);
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);

        return mapToResponse(saved);
    }





    private AuctionResponse mapToResponse(Auction auction) {
        Product product = auction.getProduct();

        return new AuctionResponse(
                auction.getId(),
                auction.getSeller() != null ? auction.getSeller().getDisplayName() : null,
                product != null ? product.getId() : null,
                product != null ? product.getProductName() : null,
                product != null ? product.getTags() : null,
                auction.getQuantity(),
                auction.getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getMinBidIncrement(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus(),
                auction.getWinner() != null ? auction.getWinner().getDisplayName() : null,
                auction.getBidCount()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

}
