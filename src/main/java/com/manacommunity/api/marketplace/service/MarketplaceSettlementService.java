package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import com.manacommunity.api.marketplace.entity.MarketplaceSettlementRecord;
import com.manacommunity.api.marketplace.repository.MarketSellerRepository;
import com.manacommunity.api.marketplace.repository.MarketplaceSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceSettlementService {

    private final MarketplaceSettlementRepository settlementRepository;
    private final MarketSellerRepository sellerRepository;
    private final WalletEngine walletEngine;

    private static final BigDecimal DEFAULT_COMMISSION_PERCENTAGE = BigDecimal.valueOf(0.02); // 2% platform fee

    @Transactional
    public MarketplaceSettlementRecord createSettlementEntry(Long orderId, String orderNumber, Long sellerUserId, BigDecimal grossAmount) {
        BigDecimal platformFee = grossAmount.multiply(DEFAULT_COMMISSION_PERCENTAGE);
        BigDecimal netPayout = grossAmount.subtract(platformFee);

        String ref = "SETTLE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MarketplaceSettlementRecord settlement = MarketplaceSettlementRecord.builder()
                .settlementReference(ref)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .sellerId(sellerUserId)
                .grossAmount(grossAmount)
                .platformFee(platformFee)
                .taxDeduction(BigDecimal.ZERO)
                .netPayoutAmount(netPayout)
                .payoutMode(MarketplaceSettlementRecord.PayoutMode.WALLET)
                .status(MarketplaceSettlementRecord.SettlementStatus.PENDING_CLEARANCE)
                .build();

        log.info("Created marketplace settlement entry {} for order {} (gross={}, net={})",
                ref, orderNumber, grossAmount, netPayout);
        return settlementRepository.save(settlement);
    }

    @Transactional
    public MarketplaceSettlementRecord releaseSettlementToSeller(String orderNumber) {
        MarketplaceSettlementRecord settlement = settlementRepository.findByOrderNumber(orderNumber)
                .orElse(null);

        if (settlement == null) {
            log.warn("No settlement entry found for order {}", orderNumber);
            return null;
        }

        if (settlement.getStatus() == MarketplaceSettlementRecord.SettlementStatus.SETTLED) {
            log.info("Settlement {} already settled.", settlement.getSettlementReference());
            return settlement;
        }

        // Credit to Seller's CFBOS Resident Wallet
        try {
            walletEngine.creditWallet(
                    settlement.getSellerId(),
                    settlement.getNetPayoutAmount(),
                    WalletTransactionType.TOPUP,
                    "MARKETPLACE_SETTLEMENT",
                    settlement.getOrderId(),
                    "Marketplace Payout for Order " + settlement.getOrderNumber()
            );

            settlement.setStatus(MarketplaceSettlementRecord.SettlementStatus.SETTLED);
            settlement.setSettledAt(LocalDateTime.now());
            settlement.setPayoutReference("WALLET-CR-" + settlement.getSettlementReference());

            sellerRepository.findByUserId(settlement.getSellerId()).ifPresent(profile -> {
                profile.setTotalOrdersCompleted(profile.getTotalOrdersCompleted() + 1);
                sellerRepository.save(profile);
            });

            log.info("Successfully settled {} to seller {} wallet", settlement.getNetPayoutAmount(), settlement.getSellerId());
        } catch (Exception ex) {
            log.error("Failed to credit wallet for settlement {}: {}", settlement.getSettlementReference(), ex.getMessage());
            settlement.setStatus(MarketplaceSettlementRecord.SettlementStatus.FAILED);
        }

        return settlementRepository.save(settlement);
    }
}
