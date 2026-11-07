package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.socialcircle.dto.KittyPartyLedgerDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyContribution;
import com.manacommunity.api.socialcircle.entity.KittyPartyCycle;
import com.manacommunity.api.socialcircle.entity.KittyPartyRotation;
import com.manacommunity.api.socialcircle.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyLedgerService {

    private final KittyPartyCycleRepository cycleRepository;
    private final KittyPartyContributionRepository contributionRepository;
    private final KittyPartyRotationRepository rotationRepository;
    private final KittyPartyCircleRepository circleRepository;
    private final KittyPartyMemberRepository memberRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public CycleSummaryResponse createCycle(Long circleId, AppUser creator, CreateCycleRequest request) {
        assertAdminOrOwner(circleId, creator.getId());

        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        LocalDate endDate = request.getStartDate().plusMonths(request.getMemberCount());

        KittyPartyCycle cycle = KittyPartyCycle.builder()
                .circle(circle)
                .cycleName(request.getCycleName().trim())
                .contributionAmount(request.getContributionAmount())
                .frequency(request.getFrequency() != null ? request.getFrequency() : "MONTHLY")
                .memberCount(request.getMemberCount())
                .startDate(request.getStartDate())
                .endDate(endDate)
                .financialMode("RECORD_ONLY") // Chit Funds Act 1982 compliance
                .legalConsentVer("2026_V1")
                .status("ACTIVE")
                .createdBy(creator)
                .build();

        KittyPartyCycle saved = cycleRepository.save(cycle);

        // Populate rotation order if user IDs provided
        if (request.getBeneficiaryRotationUserIds() != null) {
            int period = 1;
            for (Long beneficiaryId : request.getBeneficiaryRotationUserIds()) {
                AppUser beneficiary = userRepository.findById(beneficiaryId).orElse(null);
                if (beneficiary != null) {
                    KittyPartyRotation rotation = KittyPartyRotation.builder()
                            .cycle(saved)
                            .beneficiaryUser(beneficiary)
                            .periodNumber(period)
                            .rotationStatus("PLANNED")
                            .build();
                    rotationRepository.save(rotation);
                }
                period++;
            }
        }

        log.info("Created KittyPartyCycle id={} circleId={} amount={}", saved.getId(), circleId, saved.getContributionAmount());
        return getCycleSummary(saved.getId());
    }

    @Transactional(readOnly = true)
    public CycleSummaryResponse getActiveCycleForCircle(Long circleId) {
        KittyPartyCycle cycle = cycleRepository.findFirstByCircleIdAndStatusOrderByStartDateDesc(circleId, "ACTIVE")
                .or(() -> cycleRepository.findByCircleIdOrderByStartDateDesc(circleId).stream().findFirst())
                .orElse(null);
        if (cycle == null) return null;
        return getCycleSummary(cycle.getId());
    }

    @Transactional(readOnly = true)
    public CycleSummaryResponse getCycleSummary(Long cycleId) {
        KittyPartyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found"));

        List<KittyPartyRotation> rotations = rotationRepository.findByCycleIdOrderByPeriodNumberAsc(cycleId);
        List<RotationEntryDto> rotationDtos = rotations.stream().map(r -> RotationEntryDto.builder()
                .periodNumber(r.getPeriodNumber())
                .beneficiaryUserId(r.getBeneficiaryUser().getId())
                .beneficiaryName(r.getBeneficiaryUser().getFullName())
                .rotationStatus(r.getRotationStatus())
                .disbursedAt(r.getDisbursedAt())
                .externalPayoutRef(r.getExternalPayoutRef())
                .build()).collect(Collectors.toList());

        // Determine current period
        long monthsDiff = java.time.temporal.ChronoUnit.MONTHS.between(cycle.getStartDate(), LocalDate.now());
        int currentPeriod = (int) Math.max(1, Math.min(monthsDiff + 1, cycle.getMemberCount()));

        Long currentBeneficiaryId = null;
        String currentBeneficiaryName = null;
        Long nextBeneficiaryId = null;
        String nextBeneficiaryName = null;
        int previousPayoutsCount = 0;

        for (KittyPartyRotation r : rotations) {
            if (r.getPeriodNumber().equals(currentPeriod)) {
                currentBeneficiaryId = r.getBeneficiaryUser().getId();
                currentBeneficiaryName = r.getBeneficiaryUser().getFullName();
            } else if (r.getPeriodNumber().equals(currentPeriod + 1)) {
                nextBeneficiaryId = r.getBeneficiaryUser().getId();
                nextBeneficiaryName = r.getBeneficiaryUser().getFullName();
            }
            if (r.getPeriodNumber() < currentPeriod || "DISBURSED".equalsIgnoreCase(r.getRotationStatus())) {
                previousPayoutsCount++;
            }
        }

        // Expected Collections
        BigDecimal totalExpectedPerPeriod = cycle.getContributionAmount().multiply(BigDecimal.valueOf(cycle.getMemberCount()));
        BigDecimal totalExpectedFullCycle = totalExpectedPerPeriod.multiply(BigDecimal.valueOf(cycle.getMemberCount()));

        // Calculate Actual Collections & Pending from Contribution Ledger
        List<KittyPartyContribution> contributions = contributionRepository.findByCycleIdOrderByPeriodNumberAsc(cycleId);
        BigDecimal totalCollectedCurrentPeriod = BigDecimal.ZERO;
        BigDecimal totalCollectedFullCycle = BigDecimal.ZERO;
        int paidCountCurrentPeriod = 0;

        for (KittyPartyContribution c : contributions) {
            boolean isPaid = "PAID".equalsIgnoreCase(c.getPaymentStatus()) || "VERIFIED".equalsIgnoreCase(c.getPaymentStatus());
            if (isPaid && c.getRecordedAmount() != null) {
                totalCollectedFullCycle = totalCollectedFullCycle.add(c.getRecordedAmount());
                if (c.getPeriodNumber().equals(currentPeriod)) {
                    totalCollectedCurrentPeriod = totalCollectedCurrentPeriod.add(c.getRecordedAmount());
                    paidCountCurrentPeriod++;
                }
            }
        }

        BigDecimal pendingAmount = totalExpectedPerPeriod.subtract(totalCollectedCurrentPeriod).max(BigDecimal.ZERO);
        int pendingCount = Math.max(0, cycle.getMemberCount() - paidCountCurrentPeriod);
        BigDecimal previousPayoutsTotal = totalExpectedPerPeriod.multiply(BigDecimal.valueOf(previousPayoutsCount));
        double cycleProgressPercent = Math.min(100.0, Math.round(((double) currentPeriod / cycle.getMemberCount()) * 1000.0) / 10.0);

        return CycleSummaryResponse.builder()
                .id(cycle.getId())
                .circleId(cycle.getCircle().getId())
                .cycleName(cycle.getCycleName())
                .contributionAmount(cycle.getContributionAmount())
                .frequency(cycle.getFrequency())
                .memberCount(cycle.getMemberCount())
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .financialMode(cycle.getFinancialMode())
                .status(cycle.getStatus())
                .totalExpectedPerPeriod(totalExpectedPerPeriod)
                .totalExpectedFullCycle(totalExpectedFullCycle)
                .totalCollectedCurrentPeriod(totalCollectedCurrentPeriod)
                .totalCollectedFullCycle(totalCollectedFullCycle)
                .pendingContributionsAmount(pendingAmount)
                .pendingContributionsCount(pendingCount)
                .paidContributionsCount(paidCountCurrentPeriod)
                .currentPeriod(currentPeriod)
                .currentBeneficiaryUserId(currentBeneficiaryId)
                .currentBeneficiaryName(currentBeneficiaryName)
                .nextBeneficiaryUserId(nextBeneficiaryId)
                .nextBeneficiaryName(nextBeneficiaryName)
                .previousPayoutsCount(previousPayoutsCount)
                .previousPayoutsTotalAmount(previousPayoutsTotal)
                .cycleProgressPercent(cycleProgressPercent)
                .rotations(rotationDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ContributionEntryDto> getContributionsForPeriod(Long cycleId, Integer periodNumber) {
        List<KittyPartyContribution> list = (periodNumber != null)
                ? contributionRepository.findByCycleIdAndPeriodNumber(cycleId, periodNumber)
                : contributionRepository.findByCycleIdOrderByPeriodNumberAsc(cycleId);

        return list.stream().map(c -> ContributionEntryDto.builder()
                .id(c.getId())
                .cycleId(c.getCycle().getId())
                .memberUserId(c.getMemberUser().getId())
                .memberName(c.getMemberUser().getFullName())
                .periodNumber(c.getPeriodNumber())
                .expectedAmount(c.getExpectedAmount())
                .recordedAmount(c.getRecordedAmount())
                .paymentStatus(c.getPaymentStatus())
                .externalRef(c.getExternalRef())
                .receiptUrl(c.getReceiptUrl())
                .notes(c.getNotes())
                .verifiedByName(c.getVerifiedBy() != null ? c.getVerifiedBy().getFullName() : null)
                .verifiedAt(c.getVerifiedAt())
                .updatedAt(c.getUpdatedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public void recordContribution(Long cycleId, AppUser member, RecordContributionRequest request) {
        KittyPartyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found"));

        KittyPartyContribution contribution = contributionRepository
                .findByCycleIdAndMemberUserIdAndPeriodNumber(cycleId, member.getId(), request.getPeriodNumber())
                .orElseGet(() -> KittyPartyContribution.builder()
                        .cycle(cycle)
                        .memberUser(member)
                        .periodNumber(request.getPeriodNumber())
                        .expectedAmount(cycle.getContributionAmount())
                        .build());

        contribution.setRecordedAmount(request.getRecordedAmount());
        contribution.setExternalRef(request.getExternalRef());
        contribution.setReceiptUrl(request.getReceiptUrl());
        contribution.setNotes(request.getNotes());
        contribution.setPaymentStatus("PAID");
        contributionRepository.save(contribution);
        log.info("Recorded contribution for user {} cycle {} period {}", member.getId(), cycleId, request.getPeriodNumber());
    }

    @Transactional
    public void verifyContribution(Long contributionId, AppUser verifier, VerifyContributionRequest request) {
        KittyPartyContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new IllegalArgumentException("Contribution record not found"));

        assertAdminOrTreasurer(contribution.getCycle().getCircle().getId(), verifier.getId());

        contribution.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : "VERIFIED");
        contribution.setVerifiedBy(verifier);
        contribution.setVerifiedAt(LocalDateTime.now());
        contributionRepository.save(contribution);
    }

    @Transactional
    public void recordPayoutDisbursement(Long cycleId, AppUser adminUser, DisbursePayoutRequest request) {
        KittyPartyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found"));

        assertAdminOrTreasurer(cycle.getCircle().getId(), adminUser.getId());

        KittyPartyRotation rotation = rotationRepository.findByCycleIdAndPeriodNumber(cycleId, request.getPeriodNumber())
                .orElseThrow(() -> new IllegalArgumentException("Rotation not found for period " + request.getPeriodNumber()));

        rotation.setRotationStatus("DISBURSED");
        rotation.setDisbursedAt(LocalDateTime.now());
        rotation.setExternalPayoutRef(request.getExternalPayoutRef());
        rotation.setPayoutReceiptUrl(request.getPayoutReceiptUrl());
        rotationRepository.save(rotation);
        log.info("Recorded disbursement for cycle {} period {} to user {}", cycleId, request.getPeriodNumber(), rotation.getBeneficiaryUser().getId());
    }

    private void assertAdminOrOwner(Long circleId, Long userId) {
        var member = memberRepository.findByCircleIdAndUserId(circleId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this circle"));
        if (!"OWNER".equals(member.getRoleCode()) && !"ADMIN".equals(member.getRoleCode())) {
            throw new SecurityException("Requires circle Admin or Owner role");
        }
    }

    private void assertAdminOrTreasurer(Long circleId, Long userId) {
        var member = memberRepository.findByCircleIdAndUserId(circleId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this circle"));
        if (!"OWNER".equals(member.getRoleCode()) && !"ADMIN".equals(member.getRoleCode()) && !"TREASURER".equals(member.getRoleCode())) {
            throw new SecurityException("Requires circle Admin, Owner or Treasurer role");
        }
    }
}
