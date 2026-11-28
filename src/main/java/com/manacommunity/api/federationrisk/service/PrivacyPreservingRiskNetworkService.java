package com.manacommunity.api.federationrisk.service;

import com.manacommunity.api.federationrisk.entity.CityRiskSignal;
import com.manacommunity.api.federationrisk.repository.CityRiskSignalRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Privacy-Preserving City-Wide Fraud Intelligence Network:
 * - Zero raw phone/Aadhaar/resident PII shared across independent RWAs.
 * - Uses HMAC-SHA256 blinded subject & reporter tokens + minimum K>=2 independent corroboration threshold.
 * - Automatically suppresses LOCAL_DISPUTE entries and expired/appealed signals.
 */
@Service
@RequiredArgsConstructor
public class PrivacyPreservingRiskNetworkService {

    private static final String DEFAULT_CITY_PEPPER = "mana-city-risk-kms-pepper-v1";
    private static final Set<String> CITY_SHAREABLE_CATEGORIES = Set.of(
            "DELIVERY_IMPERSONATION", "THEFT_FIR", "ASSAULT_THREAT"
    );

    private final CityRiskSignalRepository signalRepo;

    @Builder
    public record RiskAssessment(
            String cityCode,
            String riskTier, // NONE, HIGH_RISK
            String topCategory,
            long corroboratedSocietiesCount,
            String actionGuidance
    ) {}

    @Transactional
    public CityRiskSignal reportRiskSignal(
            String cityCode,
            Long reportingSocietyId,
            String rawIdentifier,
            String category,
            boolean verifiedByMc,
            Instant expiresAt
    ) {
        String normCity = cityCode.trim().toUpperCase(Locale.ROOT);
        String normCat = category.trim().toUpperCase(Locale.ROOT);
        String subjectHash = computeSubjectTokenHash(normCity, rawIdentifier);
        String societyBlindHash = computeSocietyBlindHash(subjectHash, reportingSocietyId);

        boolean shareable = verifiedByMc && CITY_SHAREABLE_CATEGORIES.contains(normCat);
        String visibility = shareable ? "CITY_ELIGIBLE" : "LOCAL_ONLY";

        CityRiskSignal signal = signalRepo
                .findByCityCodeAndSubjectTokenHashAndSocietyBlindHash(normCity, subjectHash, societyBlindHash)
                .orElseGet(() -> CityRiskSignal.builder()
                        .cityCode(normCity)
                        .subjectTokenHash(subjectHash)
                        .societyBlindHash(societyBlindHash)
                        .build());

        signal.setCategory(normCat);
        signal.setVisibility(visibility);
        signal.setVerifiedByMc(verifiedByMc);
        signal.setRevokedOnAppeal(false);
        signal.setExpiresAt(expiresAt != null ? expiresAt : Instant.now().plusSeconds(180L * 86400L));
        return signalRepo.save(signal);
    }

    @Transactional(readOnly = true)
    public RiskAssessment assessCityRisk(
            String cityCode,
            String rawIdentifier,
            int corroborationThreshold,
            Instant nowOverride
    ) {
        String normCity = cityCode.trim().toUpperCase(Locale.ROOT);
        int threshold = Math.max(2, corroborationThreshold); // Never allow unilateral single-RWA city blacklist
        Instant now = nowOverride != null ? nowOverride : Instant.now();

        String subjectHash = computeSubjectTokenHash(normCity, rawIdentifier);
        List<CityRiskSignal> activeSignals = signalRepo.findByCityCodeAndSubjectTokenHash(normCity, subjectHash).stream()
                .filter(s -> "CITY_ELIGIBLE".equals(s.getVisibility()))
                .filter(CityRiskSignal::isVerifiedByMc)
                .filter(s -> !s.isRevokedOnAppeal())
                .filter(s -> s.getExpiresAt() != null && now.isBefore(s.getExpiresAt()))
                .toList();

        long count = activeSignals.size();
        if (count < threshold) {
            return RiskAssessment.builder()
                    .cityCode(normCity)
                    .riskTier("NONE")
                    .topCategory(null)
                    .corroboratedSocietiesCount(count)
                    .actionGuidance("STANDARD_VERIFICATION")
                    .build();
        }

        String topCategory = activeSignals.get(0).getCategory();
        return RiskAssessment.builder()
                .cityCode(normCity)
                .riskTier("HIGH_RISK")
                .topCategory(topCategory)
                .corroboratedSocietiesCount(count)
                .actionGuidance("VERIFY_GOVT_ID_AND_HOST_CALLBACK")
                .build();
    }

    @Transactional
    public int revokeSubjectOnAppeal(String cityCode, String rawIdentifier) {
        String normCity = cityCode.trim().toUpperCase(Locale.ROOT);
        String subjectHash = computeSubjectTokenHash(normCity, rawIdentifier);
        List<CityRiskSignal> existing = signalRepo.findByCityCodeAndSubjectTokenHash(normCity, subjectHash);
        for (CityRiskSignal s : existing) {
            s.setRevokedOnAppeal(true);
            signalRepo.save(s);
        }
        return existing.size();
    }

    public static String computeSubjectTokenHash(String cityCode, String rawIdentifier) {
        String normId = rawIdentifier != null ? rawIdentifier.replaceAll("\\s+", "").trim().toUpperCase(Locale.ROOT) : "";
        return hmacSha256Hex(DEFAULT_CITY_PEPPER, cityCode.trim().toUpperCase(Locale.ROOT) + "|" + normId);
    }

    public static String computeSocietyBlindHash(String subjectHash, Long societyId) {
        return hmacSha256Hex(DEFAULT_CITY_PEPPER, subjectHash + "|SOC|" + societyId);
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException("HMAC blind index error", ex);
        }
    }
}
