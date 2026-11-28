package com.manacommunity.api.clubhouse.service;

import com.manacommunity.api.clubhouse.dto.ClubhouseDtos.*;
import com.manacommunity.api.clubhouse.entity.*;
import com.manacommunity.api.clubhouse.repository.*;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/**
 * Paid guest access, monthly free quota tracking (2/month reserve-on-issue + release-on-expiry),
 * dynamic weekday (₹150) / weekend-holiday (₹300) pricing, and cryptographically signed turnstile QR validation.
 */
@Service
@RequiredArgsConstructor
public class GuestPassTurnstileService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final short DEFAULT_MONTHLY_FREE_LIMIT = 2;
    static final long DEFAULT_WEEKDAY_PRICE_PAISE = 15_000L; // ₹150
    static final long DEFAULT_WEEKEND_PRICE_PAISE = 30_000L; // ₹300
    static final int DEFAULT_POOL_CAPACITY = 60;

    private final FacilityMonthlyQuotaRepository quotaRepository;
    private final GuestPassPricingRuleRepository pricingRuleRepository;
    private final SocietyHolidayRepository holidayRepository;
    private final GuestAccessPassRepository passRepository;

    @Transactional
    public GuestPassQuoteResponse quoteGuestPasses(GuestPassQuoteRequest req) {
        if (req.guestCount() < 1 || req.guestCount() > 20) {
            throw new InvalidInputException("Guest count must be between 1 and 20");
        }
        String facilityCode = normalizeFacility(req.facilityCode());
        LocalDate visitDate = LocalDate.parse(req.visitDate());
        String billingMonth = visitDate.toString().substring(0, 7);

        reclaimExpiredPasses(req.societyId(), facilityCode, req.flatId(), billingMonth);
        FacilityMonthlyQuotaEntity quota = findOrCreateQuota(req.societyId(), facilityCode, req.flatId(), billingMonth);

        return buildQuote(req.societyId(), facilityCode, req.flatId(), billingMonth, visitDate, req.guestCount(), quota);
    }

    @Transactional
    public IssueGuestPassesResponse issueGuestPasses(IssueGuestPassesRequest req) {
        List<String> guests = req.guestNames() != null ? req.guestNames().stream()
                .map(String::trim).filter(s -> !s.isBlank()).toList() : List.of();
        if (guests.isEmpty()) {
            throw new InvalidInputException("At least one guest name is required");
        }
        String facilityCode = normalizeFacility(req.facilityCode());
        LocalDate visitDate = LocalDate.parse(req.visitDate());
        String billingMonth = visitDate.toString().substring(0, 7);

        reclaimExpiredPasses(req.societyId(), facilityCode, req.flatId(), billingMonth);
        FacilityMonthlyQuotaEntity quota = findOrCreateQuota(req.societyId(), facilityCode, req.flatId(), billingMonth);

        GuestPassQuoteResponse quote = buildQuote(
                req.societyId(), facilityCode, req.flatId(), billingMonth, visitDate, guests.size(), quota);

        if (quote.freeSlotsAllocated() > 0) {
            quota.setFreeReserved((short) (quota.getFreeReserved() + quote.freeSlotsAllocated()));
            quota.setUpdatedAt(Instant.now());
            quotaRepository.save(quota);
        }

        Instant validFrom = visitDate.atStartOfDay(IST).toInstant();
        Instant validUntil = visitDate.plusDays(1).atStartOfDay(IST).toInstant().minusSeconds(1);

        List<GuestAccessPassDto> issued = new ArrayList<>(guests.size());
        for (int i = 0; i < guests.size(); i++) {
            GuestSlotBreakdownDto slot = quote.breakdown().get(i);
            String jti = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String qrToken = signCompactQrToken(jti, req.societyId(), facilityCode, req.flatId(), slot.pricingTier(), validUntil);

            GuestAccessPassEntity pass = new GuestAccessPassEntity();
            pass.setPassId(UUID.randomUUID());
            pass.setSocietyId(req.societyId());
            pass.setFacilityCode(facilityCode);
            pass.setFlatId(req.flatId());
            pass.setUnitCode(req.unitCode() != null && !req.unitCode().isBlank() ? req.unitCode() : "Tower A - " + req.flatId());
            pass.setBillingMonth(billingMonth);
            pass.setVisitDate(visitDate);
            pass.setGuestName(guests.get(i));
            pass.setGuestPhone(req.guestPhone());
            pass.setPricingTier(slot.pricingTier());
            pass.setUnitPricePaise(slot.unitPricePaise());
            pass.setGstPaise(slot.gstPaise());
            pass.setTotalPaise(slot.totalPaise());
            pass.setJti(jti);
            pass.setQrToken(qrToken);
            pass.setStatus("ACTIVE");
            pass.setEntryCount(0);
            pass.setMaxEntries(1);
            pass.setValidFrom(validFrom);
            pass.setValidUntil(validUntil);
            passRepository.save(pass);
            issued.add(toPassDto(pass));
        }

        return new IssueGuestPassesResponse(quote, issued);
    }

    @Transactional
    public GuestAccessPassDto cancelGuestPass(UUID passId) {
        GuestAccessPassEntity pass = passRepository.findById(passId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest pass not found: " + passId));
        if (!"ACTIVE".equals(pass.getStatus())) {
            throw new InvalidInputException("Only ACTIVE unused passes can be cancelled");
        }
        pass.setStatus("CANCELLED");
        passRepository.save(pass);

        if ("FREE_QUOTA".equals(pass.getPricingTier())) {
            FacilityMonthlyQuotaEntity quota = findOrCreateQuota(
                    pass.getSocietyId(), pass.getFacilityCode(), pass.getFlatId(), pass.getBillingMonth());
            quota.setFreeReserved((short) Math.max(0, quota.getFreeReserved() - 1));
            quota.setUpdatedAt(Instant.now());
            quotaRepository.save(quota);
        }
        return toPassDto(pass);
    }

    @Transactional(readOnly = true)
    public List<GuestAccessPassDto> listFlatPasses(long societyId, long flatId, String billingMonth) {
        String month = billingMonth != null && !billingMonth.isBlank()
                ? billingMonth
                : LocalDate.now(IST).toString().substring(0, 7);
        return passRepository.findBySocietyIdAndFlatIdAndBillingMonthOrderByCreatedAtDesc(societyId, flatId, month)
                .stream().map(this::toPassDto).toList();
    }

    @Transactional
    public TurnstileValidateResponse validateTurnstileScan(TurnstileValidateRequest req) {
        String facilityCode = normalizeFacility(req.facilityCode());
        int currentOccupancy = passRepository
                .findBySocietyIdAndFacilityCodeAndStatusOrderByConsumedAtDesc(req.societyId(), facilityCode, "CONSUMED")
                .size();

        if (req.qrToken() == null || !req.qrToken().startsWith("v1.")) {
            return denied("MALFORMED_TOKEN", "Invalid QR format", null, null, null, null, null, currentOccupancy);
        }

        String[] parts = req.qrToken().split("\\.");
        if (parts.length != 3) {
            return denied("MALFORMED_TOKEN", "Malformed signed QR token", null, null, null, null, null, currentOccupancy);
        }

        String expectedSig = ClubhouseCryptoUtil.hmacSha256Hex("v1." + parts[1]).substring(0, 24);
        if (!ClubhouseCryptoUtil.constantTimeEquals(expectedSig, parts[2])) {
            return denied("INVALID_SIGNATURE", "Cryptographic QR signature verification failed", null, null, null, null, null, currentOccupancy);
        }

        String decodedClaims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String[] claimParts = decodedClaims.split("\\|");
        if (claimParts.length < 5) {
            return denied("MALFORMED_CLAIMS", "Incomplete QR claims", null, null, null, null, null, currentOccupancy);
        }
        String jti = claimParts[0];

        GuestAccessPassEntity pass = passRepository.findByJti(jti).orElse(null);
        if (pass == null) {
            return denied("PASS_NOT_FOUND", "Pass does not exist in society registry", null, jti, null, null, null, currentOccupancy);
        }
        if (!Objects.equals(pass.getSocietyId(), req.societyId()) || !facilityCode.equals(pass.getFacilityCode())) {
            return denied("FACILITY_MISMATCH", "Pass belongs to another facility or society",
                    pass.getPassId(), jti, pass.getUnitCode(), pass.getGuestName(), pass.getPricingTier(), currentOccupancy);
        }

        Instant now = Instant.now();
        if (now.isBefore(pass.getValidFrom()) || now.isAfter(pass.getValidUntil())) {
            if ("ACTIVE".equals(pass.getStatus()) && now.isAfter(pass.getValidUntil())) {
                expireAndRelease(pass);
            }
            return denied("OUTSIDE_VALID_WINDOW", "Pass is outside its allowed visit date window",
                    pass.getPassId(), jti, pass.getUnitCode(), pass.getGuestName(), pass.getPricingTier(), currentOccupancy);
        }

        if (!"ACTIVE".equals(pass.getStatus()) || pass.getEntryCount() >= pass.getMaxEntries()) {
            return denied("ALREADY_CONSUMED", "Anti-passback: QR already redeemed (" + pass.getStatus() + ")",
                    pass.getPassId(), jti, pass.getUnitCode(), pass.getGuestName(), pass.getPricingTier(), currentOccupancy);
        }

        if (currentOccupancy >= DEFAULT_POOL_CAPACITY) {
            return denied("POOL_AT_CAPACITY", "Facility at safety capacity (" + DEFAULT_POOL_CAPACITY + ")",
                    pass.getPassId(), jti, pass.getUnitCode(), pass.getGuestName(), pass.getPricingTier(), currentOccupancy);
        }

        // Admit guest: promote free_reserved -> free_consumed cleanly
        pass.setEntryCount(pass.getEntryCount() + 1);
        if (pass.getEntryCount() >= pass.getMaxEntries()) {
            pass.setStatus("CONSUMED");
        }
        pass.setConsumedAt(now);
        passRepository.save(pass);

        if ("FREE_QUOTA".equals(pass.getPricingTier())) {
            FacilityMonthlyQuotaEntity quota = findOrCreateQuota(
                    pass.getSocietyId(), pass.getFacilityCode(), pass.getFlatId(), pass.getBillingMonth());
            quota.setFreeReserved((short) Math.max(0, quota.getFreeReserved() - 1));
            quota.setFreeConsumed((short) (quota.getFreeConsumed() + 1));
            quota.setUpdatedAt(now);
            quotaRepository.save(quota);
        }

        return new TurnstileValidateResponse(
                true,
                "ACCESS_GRANTED",
                "Welcome " + pass.getGuestName() + " (" + pass.getPricingTier() + ") via " + req.turnstileId(),
                pass.getPassId(),
                pass.getJti(),
                pass.getUnitCode(),
                pass.getGuestName(),
                pass.getPricingTier(),
                currentOccupancy + 1,
                DEFAULT_POOL_CAPACITY
        );
    }

    private GuestPassQuoteResponse buildQuote(
            long societyId,
            String facilityCode,
            long flatId,
            String billingMonth,
            LocalDate visitDate,
            int guestCount,
            FacilityMonthlyQuotaEntity quota) {
        String dayCategory = resolveDayCategory(societyId, visitDate);
        long[] priceAndGst = resolveRulePrice(societyId, facilityCode, dayCategory, visitDate);
        long paidUnitPrice = priceAndGst[0];
        int gstBps = (int) priceAndGst[1];
        long paidGst = Math.round((paidUnitPrice * (double) gstBps) / 10_000.0);

        int freeRemainingBefore = quota.freeRemaining();
        int freeAllocated = Math.min(guestCount, freeRemainingBefore);
        int paidNeeded = guestCount - freeAllocated;

        String paidTier = "PAID_" + dayCategory;
        List<GuestSlotBreakdownDto> breakdown = new ArrayList<>(guestCount);
        long totalPayable = 0L;

        for (int i = 1; i <= guestCount; i++) {
            if (i <= freeAllocated) {
                breakdown.add(new GuestSlotBreakdownDto(i, "FREE_QUOTA", 0L, 0L, 0L));
            } else {
                long slotTotal = paidUnitPrice + paidGst;
                totalPayable += slotTotal;
                breakdown.add(new GuestSlotBreakdownDto(i, paidTier, paidUnitPrice, paidGst, slotTotal));
            }
        }

        return new GuestPassQuoteResponse(
                societyId,
                facilityCode,
                flatId,
                billingMonth,
                visitDate.toString(),
                dayCategory,
                quota.getFreeLimit(),
                quota.getFreeConsumed(),
                quota.getFreeReserved(),
                freeRemainingBefore,
                freeAllocated,
                paidNeeded,
                paidUnitPrice,
                totalPayable,
                breakdown
        );
    }

    private String resolveDayCategory(long societyId, LocalDate visitDate) {
        if (holidayRepository.existsById(new SocietyHolidayId(societyId, visitDate))) {
            return "HOLIDAY";
        }
        DayOfWeek dow = visitDate.getDayOfWeek();
        return (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? "WEEKEND" : "WEEKDAY";
    }

    private long[] resolveRulePrice(long societyId, String facilityCode, String dayCategory, LocalDate visitDate) {
        List<GuestPassPricingRuleEntity> rules = pricingRuleRepository
                .findBySocietyIdAndFacilityCodeAndDayCategory(societyId, facilityCode, dayCategory);
        for (GuestPassPricingRuleEntity r : rules) {
            boolean afterStart = !visitDate.isBefore(r.getEffectiveFrom());
            boolean beforeEnd = r.getEffectiveTo() == null || !visitDate.isAfter(r.getEffectiveTo());
            if (afterStart && beforeEnd) {
                return new long[]{r.getUnitPricePaise(), r.getGstBps()};
            }
        }
        long fallback = "WEEKDAY".equals(dayCategory) ? DEFAULT_WEEKDAY_PRICE_PAISE : DEFAULT_WEEKEND_PRICE_PAISE;
        return new long[]{fallback, 0L};
    }

    private void reclaimExpiredPasses(long societyId, String facilityCode, long flatId, String billingMonth) {
        Instant now = Instant.now();
        List<GuestAccessPassEntity> passes = passRepository
                .findBySocietyIdAndFlatIdAndBillingMonthOrderByCreatedAtDesc(societyId, flatId, billingMonth);
        for (GuestAccessPassEntity p : passes) {
            if (facilityCode.equals(p.getFacilityCode())
                    && "ACTIVE".equals(p.getStatus())
                    && now.isAfter(p.getValidUntil())) {
                expireAndRelease(p);
            }
        }
    }

    private void expireAndRelease(GuestAccessPassEntity pass) {
        pass.setStatus("EXPIRED");
        passRepository.save(pass);
        if ("FREE_QUOTA".equals(pass.getPricingTier())) {
            FacilityMonthlyQuotaEntity quota = findOrCreateQuota(
                    pass.getSocietyId(), pass.getFacilityCode(), pass.getFlatId(), pass.getBillingMonth());
            quota.setFreeReserved((short) Math.max(0, quota.getFreeReserved() - 1));
            quota.setUpdatedAt(Instant.now());
            quotaRepository.save(quota);
        }
    }

    private FacilityMonthlyQuotaEntity findOrCreateQuota(
            long societyId, String facilityCode, long flatId, String billingMonth) {
        FacilityMonthlyQuotaId id = new FacilityMonthlyQuotaId(societyId, facilityCode, flatId, billingMonth);
        return quotaRepository.findById(id).orElseGet(() -> {
            FacilityMonthlyQuotaEntity q = new FacilityMonthlyQuotaEntity();
            q.setId(id);
            q.setFreeLimit(DEFAULT_MONTHLY_FREE_LIMIT);
            q.setFreeConsumed((short) 0);
            q.setFreeReserved((short) 0);
            return quotaRepository.save(q);
        });
    }

    private String signCompactQrToken(
            String jti, long societyId, String facilityCode, long flatId, String tier, Instant validUntil) {
        String claims = jti + "|" + societyId + "|" + facilityCode + "|" + flatId + "|" + tier + "|" + validUntil.getEpochSecond();
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        String sig = ClubhouseCryptoUtil.hmacSha256Hex("v1." + b64).substring(0, 24);
        return "v1." + b64 + "." + sig;
    }

    private static String normalizeFacility(String facilityCode) {
        if (facilityCode == null || facilityCode.isBlank()) return "SWIMMING_POOL";
        return facilityCode.trim().toUpperCase(Locale.ROOT);
    }

    private TurnstileValidateResponse denied(
            String code, String msg, UUID passId, String jti, String unitCode, String guestName, String tier, int occupancy) {
        return new TurnstileValidateResponse(
                false, code, msg, passId, jti, unitCode, guestName, tier, occupancy, DEFAULT_POOL_CAPACITY);
    }

    private GuestAccessPassDto toPassDto(GuestAccessPassEntity p) {
        return new GuestAccessPassDto(
                p.getPassId(), p.getSocietyId(), p.getFacilityCode(), p.getFlatId(), p.getUnitCode(),
                p.getBillingMonth(), p.getVisitDate().toString(), p.getGuestName(), p.getGuestPhone(),
                p.getPricingTier(), p.getUnitPricePaise(), p.getGstPaise(), p.getTotalPaise(),
                p.getJti(), p.getQrToken(), p.getStatus(), p.getEntryCount(), p.getMaxEntries(),
                p.getValidFrom(), p.getValidUntil(), p.getConsumedAt()
        );
    }
}
