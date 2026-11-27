package com.manacommunity.api.governance.service;

import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Evaluates unit-scoped voting eligibility (ownership vs tenant, 90-day maintenance defaulter gate)
 * and computes voting weight (ONE_FLAT_ONE_VOTE, UDS_AREA_WEIGHTED, SHARE_CERTIFICATE).
 */
@Service
@RequiredArgsConstructor
public class VotingEligibilityService {

    public static final String MODE_ONE_FLAT_ONE_VOTE = "ONE_FLAT_ONE_VOTE";
    public static final String MODE_UDS_AREA_WEIGHTED = "UDS_AREA_WEIGHTED";
    public static final String MODE_SHARE_CERTIFICATE = "SHARE_CERTIFICATE";

    private static final Set<String> OPEN_INVOICE_STATUSES = Set.of("UNPAID", "OVERDUE", "PARTIAL");

    private final CommunityFlatRepository flatRepository;
    private final FlatMembershipRepository membershipRepository;
    private final InvoiceRepository invoiceRepository;

    @Builder
    public record EligibilityResult(
            boolean eligible,
            String reasonCode,
            String message,
            Long flatId,
            String flatLabel,
            String residentType,
            BigDecimal weightApplied,
            String votingMode,
            int overdueInvoiceCount,
            BigDecimal overdueAmount,
            LocalDate oldestOverdueDate
    ) {}

    public EligibilityResult evaluate(
            Long communityId,
            Long flatId,
            Long userId,
            String votingMode,
            int excludeDefaultersOverDays,
            boolean ownersOnly,
            LocalDate asOfDate
    ) {
        LocalDate refDate = asOfDate != null ? asOfDate : LocalDate.now();
        String mode = (votingMode != null && !votingMode.isBlank()) ? votingMode : MODE_ONE_FLAT_ONE_VOTE;

        CommunityFlat flat = flatRepository.findById(flatId).orElse(null);
        if (flat == null || (communityId != null && flat.getCommunity() != null
                && !communityId.equals(flat.getCommunity().getId()))) {
            return EligibilityResult.builder()
                    .eligible(false)
                    .reasonCode("FLAT_NOT_FOUND")
                    .message("Flat does not belong to this community")
                    .flatId(flatId)
                    .weightApplied(BigDecimal.ZERO)
                    .votingMode(mode)
                    .build();
        }

        FlatMembership membership = membershipRepository.findByUserIdAndFlatId(userId, flatId).orElse(null);
        if (membership == null || "REVOKED".equalsIgnoreCase(membership.getAppAccessStatus())) {
            return EligibilityResult.builder()
                    .eligible(false)
                    .reasonCode("NOT_FLAT_MEMBER")
                    .message("User is not an active member of this flat")
                    .flatId(flatId)
                    .flatLabel(flat.getTowerBlock() + "-" + flat.getFlatNumber())
                    .weightApplied(BigDecimal.ZERO)
                    .votingMode(mode)
                    .build();
        }

        String residentType = membership.getResidentType() != null ? membership.getResidentType().toUpperCase() : "OWNER";
        if (ownersOnly && !"OWNER".equals(residentType)) {
            return EligibilityResult.builder()
                    .eligible(false)
                    .reasonCode("OWNER_REQUIRED")
                    .message("Only registered owners or approved proxies may vote on statutory resolutions")
                    .flatId(flatId)
                    .flatLabel(flat.getTowerBlock() + "-" + flat.getFlatNumber())
                    .residentType(residentType)
                    .weightApplied(BigDecimal.ZERO)
                    .votingMode(mode)
                    .build();
        }

        if (excludeDefaultersOverDays > 0) {
            LocalDate cutoff = refDate.minusDays(excludeDefaultersOverDays);
            List<Invoice> invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(userId);
            List<Invoice> defaulting = invoices.stream()
                    .filter(inv -> inv.getCommunity() == null || communityId == null || communityId.equals(inv.getCommunity().getId()))
                    .filter(inv -> inv.getStatus() != null && OPEN_INVOICE_STATUSES.contains(inv.getStatus().toUpperCase()))
                    .filter(inv -> inv.getDueDate() != null && inv.getDueDate().isBefore(cutoff))
                    .toList();

            if (!defaulting.isEmpty()) {
                BigDecimal overdueSum = defaulting.stream()
                        .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                LocalDate oldest = defaulting.stream()
                        .map(Invoice::getDueDate)
                        .min(LocalDate::compareTo)
                        .orElse(cutoff);

                return EligibilityResult.builder()
                        .eligible(false)
                        .reasonCode("DEFAULTED_OVER_" + excludeDefaultersOverDays + "_DAYS")
                        .message(String.format("Voting suspended: %d maintenance invoice(s) overdue beyond %d days (Rs %s). Clear dues to unlock.",
                                defaulting.size(), excludeDefaultersOverDays, overdueSum.toPlainString()))
                        .flatId(flatId)
                        .flatLabel(flat.getTowerBlock() + "-" + flat.getFlatNumber())
                        .residentType(residentType)
                        .weightApplied(BigDecimal.ZERO)
                        .votingMode(mode)
                        .overdueInvoiceCount(defaulting.size())
                        .overdueAmount(overdueSum)
                        .oldestOverdueDate(oldest)
                        .build();
            }
        }

        BigDecimal weight = resolveWeight(flat, mode);
        return EligibilityResult.builder()
                .eligible(true)
                .reasonCode("ELIGIBLE")
                .message("Eligible to vote")
                .flatId(flatId)
                .flatLabel(flat.getTowerBlock() + "-" + flat.getFlatNumber())
                .residentType(residentType)
                .weightApplied(weight)
                .votingMode(mode)
                .overdueInvoiceCount(0)
                .overdueAmount(BigDecimal.ZERO)
                .build();
    }

    public BigDecimal resolveWeight(CommunityFlat flat, String votingMode) {
        if (MODE_UDS_AREA_WEIGHTED.equalsIgnoreCase(votingMode)) {
            return flat.getUdsSqft() != null ? flat.getUdsSqft() : BigDecimal.ONE;
        }
        if (MODE_SHARE_CERTIFICATE.equalsIgnoreCase(votingMode)) {
            return flat.getShareUnits() != null ? BigDecimal.valueOf(flat.getShareUnits()) : BigDecimal.ONE;
        }
        return BigDecimal.ONE;
    }
}
