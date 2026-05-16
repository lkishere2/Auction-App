package com.auction.app.domains.auction.bids;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.auction.app.domains.auction.reservedFunds.ReservedFundStatus;
import com.auction.app.domains.auction.reservedFunds.ReservedFunds;
import com.auction.app.domains.auction.reservedFunds.ReservedFundsRepository;
import com.ltnc.auction.domain.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpendableBalanceService {

    private final ReservedFundsRepository reservedFundsRepository;

    public BigDecimal getSpendableBalance(User user) {
        List<ReservedFunds> heldFunds = reservedFundsRepository
                .findByUserUserIdAndStatus(user.getUserId(), ReservedFundStatus.HELD);

        BigDecimal totalHeld = heldFunds.stream()
                .map(ReservedFunds::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return user.getBalance().subtract(totalHeld);
    }
}