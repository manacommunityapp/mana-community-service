package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.MultiTenderSettlement;
import com.manacommunity.api.homeservices.entity.SettlementLedgerEntry;
import com.manacommunity.api.homeservices.repository.MultiTenderSettlementRepository;
import com.manacommunity.api.homeservices.repository.SettlementLedgerEntryRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Tender Orchestration & Double-Entry Split-Settlement Accounting Service.
 * Reconciles Reward Points + Brand Coupons + UPI Gateway tenders into balanced Debit/Credit
 * journal lines for Vendor Payable, Platform Commission, and 18% GST Output Tax.
 */
@Service
@RequiredArgsConstructor
public class SplitSettlementLedgerService {

    public static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.15"); // 15%
    public static final BigDecimal GST_RATE = new BigDecimal("0.18"); // 18% on platform commission

    private final MultiTenderSettlementRepository settlementRepo;
    private final SettlementLedgerEntryRepository ledgerRepo;

    @Builder
    public record MultiTenderQuoteRequest(
            String orderRef,
            Long bookingId,
            Long communityId,
            Long vendorId,
            Long userId,
            BigDecimal grossAmount,
            BigDecimal pointsUsed,
            String couponCode,
            BigDecimal couponDiscount,
            BigDecimal upiAmount,
            BigDecimal commissionRate
    ) {}

    @Builder
    public record SettlementWithLedger(
            MultiTenderSettlement settlement,
            List<SettlementLedgerEntry> entries,
            boolean balanced
    ) {}

    @Transactional
    public SettlementWithLedger initiateAndReserve(MultiTenderQuoteRequest req) {
        BigDecimal gross = scale(req.grossAmount());
        BigDecimal points = scale(req.pointsUsed());
        BigDecimal coupon = scale(req.couponDiscount());
        BigDecimal upi = scale(req.upiAmount());

        if (points.add(coupon).add(upi).compareTo(gross) != 0) {
            throw new IllegalArgumentException("Tender split sum must equal gross order amount (" + gross + ")");
        }

        BigDecimal commRate = req.commissionRate() != null ? req.commissionRate() : DEFAULT_COMMISSION_RATE;
        BigDecimal commission = gross.multiply(commRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstOnComm = commission.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vendorNet = gross.subtract(commission).subtract(gstOnComm);

        MultiTenderSettlement settlement = settlementRepo.save(MultiTenderSettlement.builder()
                .orderRef(req.orderRef())
                .bookingId(req.bookingId())
                .communityId(req.communityId())
                .vendorId(req.vendorId())
                .userId(req.userId())
                .grossAmount(gross)
                .pointsUsed(points)
                .couponCode(req.couponCode())
                .couponDiscount(coupon)
                .upiAmount(upi)
                .platformCommission(commission)
                .commissionGst(gstOnComm)
                .vendorNetPayout(vendorNet)
                .state("FUNDS_RESERVED")
                .build());

        List<SettlementLedgerEntry> entries = new ArrayList<>();
        // Funding Legs (Debits totaling grossAmount)
        if (upi.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(saveEntry(settlement.getId(), "TENDER_UPI_ESCROW", "DEBIT", upi, "UPI tender captured/escrowed"));
        }
        if (points.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(saveEntry(settlement.getId(), "REWARD_POINTS_LIABILITY", "DEBIT", points, "Mygate reward points redeemed"));
        }
        if (coupon.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(saveEntry(settlement.getId(), "BRAND_COUPON_RECEIVABLE:" + (req.couponCode() != null ? req.couponCode() : "PROMO"),
                    "DEBIT", coupon, "Co-funded brand coupon contribution"));
        }

        // Settlement Allocation Legs (Credits totaling grossAmount)
        entries.add(saveEntry(settlement.getId(), "VENDOR_PAYABLE:" + req.vendorId(), "CREDIT", vendorNet, "Net vendor payout in escrow"));
        entries.add(saveEntry(settlement.getId(), "PLATFORM_COMMISSION_REVENUE", "CREDIT", commission, "Platform marketplace fee"));
        entries.add(saveEntry(settlement.getId(), "GST_OUTPUT_PAYABLE", "CREDIT", gstOnComm, "18% GST on platform commission (SAC 9997)"));

        return SettlementWithLedger.builder()
                .settlement(settlement)
                .entries(entries)
                .balanced(isLedgerBalanced(entries))
                .build();
    }

    @Transactional
    public MultiTenderSettlement markCapturedAndEligibleOnCompletion(String orderRef, boolean jobCompleted) {
        MultiTenderSettlement s = settlementRepo.findByOrderRef(orderRef)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + orderRef));
        if (!"FUNDS_RESERVED".equals(s.getState()) && !"CAPTURED_IN_ESCROW".equals(s.getState())) {
            throw new IllegalStateException("Invalid state transition from " + s.getState());
        }
        s.setState(jobCompleted ? "SETTLEMENT_ELIGIBLE" : "CAPTURED_IN_ESCROW");
        s.setUpdatedAt(Instant.now());
        return settlementRepo.save(s);
    }

    @Transactional
    public SettlementWithLedger compensateOnUpiFailure(String orderRef, String reason) {
        MultiTenderSettlement s = settlementRepo.findByOrderRef(orderRef)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + orderRef));
        if ("COMPENSATED".equals(s.getState())) {
            List<SettlementLedgerEntry> existing = ledgerRepo.findBySettlementIdOrderByIdAsc(s.getId());
            return SettlementWithLedger.builder().settlement(s).entries(existing).balanced(isLedgerBalanced(existing)).build();
        }
        if ("SETTLEMENT_ELIGIBLE".equals(s.getState())) {
            throw new IllegalStateException("Cannot compensate already settled order");
        }

        List<SettlementLedgerEntry> reversal = new ArrayList<>();
        if (s.getUpiAmount().compareTo(BigDecimal.ZERO) > 0) {
            reversal.add(saveEntry(s.getId(), "TENDER_UPI_ESCROW", "CREDIT", s.getUpiAmount(), "UPI failure/reversal: " + reason));
        }
        if (s.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            reversal.add(saveEntry(s.getId(), "REWARD_POINTS_LIABILITY", "CREDIT", s.getPointsUsed(), "Release reserved points back to user"));
        }
        if (s.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0) {
            reversal.add(saveEntry(s.getId(), "BRAND_COUPON_RECEIVABLE:" + (s.getCouponCode() != null ? s.getCouponCode() : "PROMO"),
                    "CREDIT", s.getCouponDiscount(), "Restore coupon redemption quota"));
        }
        reversal.add(saveEntry(s.getId(), "VENDOR_PAYABLE:" + s.getVendorId(), "DEBIT", s.getVendorNetPayout(), "Cancel vendor escrow"));
        reversal.add(saveEntry(s.getId(), "PLATFORM_COMMISSION_REVENUE", "DEBIT", s.getPlatformCommission(), "Reverse commission"));
        reversal.add(saveEntry(s.getId(), "GST_OUTPUT_PAYABLE", "DEBIT", s.getCommissionGst(), "Reverse output GST"));

        s.setState("COMPENSATED");
        s.setUpdatedAt(Instant.now());
        settlementRepo.save(s);

        List<SettlementLedgerEntry> allEntries = ledgerRepo.findBySettlementIdOrderByIdAsc(s.getId());
        return SettlementWithLedger.builder()
                .settlement(s)
                .entries(allEntries.isEmpty() ? reversal : allEntries)
                .balanced(isLedgerBalanced(allEntries.isEmpty() ? reversal : allEntries))
                .build();
    }

    public boolean isLedgerBalanced(List<SettlementLedgerEntry> entries) {
        BigDecimal debits = entries.stream()
                .filter(e -> "DEBIT".equals(e.getEntryType()))
                .map(SettlementLedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = entries.stream()
                .filter(e -> "CREDIT".equals(e.getEntryType()))
                .map(SettlementLedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return debits.compareTo(credits) == 0;
    }

    private SettlementLedgerEntry saveEntry(Long settlementId, String account, String type, BigDecimal amount, String desc) {
        return ledgerRepo.save(SettlementLedgerEntry.builder()
                .settlementId(settlementId)
                .accountCode(account)
                .entryType(type)
                .amount(amount)
                .description(desc)
                .build());
    }

    private BigDecimal scale(BigDecimal val) {
        return (val != null ? val : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
