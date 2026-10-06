package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.transaction.model.TransactionIntent;
import com.manacommunity.api.transaction.model.TransactionSplitDetail;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsVendorWallet;
import com.manacommunity.api.vendor.entity.VmsWalletTransaction;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsVendorWalletRepository;
import com.manacommunity.api.vendor.repository.VmsWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSettlementPillar {

    private final VmsVendorRepository vendorRepository;
    private final VmsVendorWalletRepository vendorWalletRepository;
    private final VmsWalletTransactionRepository walletTransactionRepository;
    private final CommunityRepository communityRepository;

    public TransactionSplitDetail calculateSplit(TransactionIntent intent) {
        BigDecimal gross = intent.getAmount() != null ? intent.getAmount() : BigDecimal.ZERO;
        BigDecimal tax = intent.getTaxAmount() != null ? intent.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal platformFee = intent.getPlatformFee() != null ? intent.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal tds = intent.getTdsAmount() != null ? intent.getTdsAmount() : BigDecimal.ZERO;

        // Default platform commission: 5% if not provided and payee exists
        if (platformFee.compareTo(BigDecimal.ZERO) == 0 && intent.getPayeeId() != null && intent.getPayeeId() > 0) {
            platformFee = gross.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        }

        // Default TDS: 1% under 194-O for e-commerce/marketplace if not provided
        if (tds.compareTo(BigDecimal.ZERO) == 0 && intent.getPayeeId() != null && intent.getPayeeId() > 0) {
            tds = gross.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal vendorPayout = gross.subtract(platformFee).subtract(tds).subtract(tax);
        if (vendorPayout.compareTo(BigDecimal.ZERO) < 0) {
            vendorPayout = BigDecimal.ZERO;
        }

        return TransactionSplitDetail.builder()
                .grossAmount(gross)
                .platformFee(platformFee)
                .platformFeeTax(platformFee.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP))
                .tdsAmount(tds)
                .vendorPayout(vendorPayout)
                .taxAmount(tax)
                .build();
    }

    @Transactional
    public void creditVendorSettlement(Long vendorId, Long communityId, BigDecimal payoutAmount, String transactionNumber) {
        if (vendorId == null || payoutAmount == null || payoutAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        VmsVendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            log.info("Payee {} is not registered as a VMS vendor; skipping vendor wallet credit", vendorId);
            return;
        }

        Community community = communityId != null ? communityRepository.findById(communityId).orElse(null) : null;
        if (community == null && vendor.getCommunity() != null) {
            community = vendor.getCommunity();
        }

        if (community == null) {
            log.warn("Cannot credit vendor wallet without community association for vendor {}", vendorId);
            return;
        }

        Community finalCommunity = community;
        VmsVendorWallet wallet = vendorWalletRepository.findByVendorIdAndCommunityId(vendorId, community.getId())
                .orElseGet(() -> vendorWalletRepository.save(
                        VmsVendorWallet.builder()
                                .vendor(vendor)
                                .community(finalCommunity)
                                .balance(BigDecimal.ZERO)
                                .totalEarned(BigDecimal.ZERO)
                                .totalWithdrawn(BigDecimal.ZERO)
                                .totalCommission(BigDecimal.ZERO)
                                .build()
                ));

        BigDecimal newBalance = wallet.getBalance().add(payoutAmount);
        wallet.setBalance(newBalance);
        wallet.setTotalEarned(wallet.getTotalEarned().add(payoutAmount));
        vendorWalletRepository.save(wallet);

        VmsWalletTransaction txn = VmsWalletTransaction.builder()
                .wallet(wallet)
                .type("EARNING")
                .amount(payoutAmount)
                .balanceAfter(newBalance)
                .referenceType("TRANSACTION_CORE")
                .description("Settlement payout for transaction " + transactionNumber)
                .build();

        walletTransactionRepository.save(txn);
        log.info("Credited vendor wallet {} with amount {} for txn {}", wallet.getId(), payoutAmount, transactionNumber);
    }
}
