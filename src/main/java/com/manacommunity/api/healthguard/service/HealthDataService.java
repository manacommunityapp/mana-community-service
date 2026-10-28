package com.manacommunity.api.healthguard.service;

import com.manacommunity.api.healthguard.dto.HealthDataConsentDto;
import com.manacommunity.api.healthguard.dto.HealthDataConsentRequest;
import com.manacommunity.api.healthguard.dto.HealthObservationBatchRequest;
import com.manacommunity.api.healthguard.dto.HealthObservationDto;
import com.manacommunity.api.healthguard.entity.HealthDataConsent;
import com.manacommunity.api.healthguard.entity.HealthObservation;
import com.manacommunity.api.healthguard.repository.HealthDataConsentRepository;
import com.manacommunity.api.healthguard.repository.HealthObservationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HealthDataService {
    private static final String HEALTH_CONNECT = "HEALTH_CONNECT";
    private static final Set<String> SCOPES = Set.of("STEPS", "WORKOUTS");
    private static final Set<String> TYPES = Set.of("STEPS", "WORKOUT");

    private final HealthDataConsentRepository consentRepository;
    private final HealthObservationRepository observationRepository;

    public HealthDataService(HealthDataConsentRepository consentRepository,
                             HealthObservationRepository observationRepository) {
        this.consentRepository = consentRepository;
        this.observationRepository = observationRepository;
    }

    @Transactional
    public List<HealthDataConsentDto> saveConsents(Long userId, HealthDataConsentRequest request) {
        String provider = normalizeProvider(request.getProvider());
        String version = request.getConsentVersion().trim();
        Set<String> grantedScopes = request.getGrantedScopes().stream()
                .map(this::normalizeScope)
                .collect(java.util.stream.Collectors.toSet());
        if (!SCOPES.containsAll(grantedScopes)) {
            throw new IllegalArgumentException("Unsupported health data scope");
        }

        OffsetDateTime now = OffsetDateTime.now();
        consentRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, provider)
                .forEach(consent -> consent.setRevokedAt(now));
        for (String scope : grantedScopes) {
            HealthDataConsent consent = consentRepository
                    .findByUserIdAndProviderAndDataScopeAndConsentVersion(userId, provider, scope, version)
                    .orElseGet(() -> HealthDataConsent.builder()
                            .userId(userId).provider(provider).dataScope(scope).consentVersion(version).build());
            consent.setGrantedAt(now);
            consent.setRevokedAt(null);
            consentRepository.save(consent);
        }
        return getConsents(userId);
    }

    public List<HealthDataConsentDto> getConsents(Long userId) {
        return consentRepository.findByUserIdAndRevokedAtIsNullOrderByProviderAscDataScopeAsc(userId)
                .stream().map(c -> HealthDataConsentDto.builder()
                        .provider(c.getProvider()).scope(c.getDataScope())
                        .consentVersion(c.getConsentVersion()).grantedAt(c.getGrantedAt())
                        .revokedAt(c.getRevokedAt()).build())
                .toList();
    }

    @Transactional
    public void revokeConsent(Long userId, String providerName, String scopeName) {
        String provider = normalizeProvider(providerName);
        String scope = normalizeScope(scopeName);
        if (!SCOPES.contains(scope)) throw new IllegalArgumentException("Unsupported health data scope");
        consentRepository.findByUserIdAndRevokedAtIsNullOrderByProviderAscDataScopeAsc(userId).stream()
                .filter(c -> c.getProvider().equals(provider) && c.getDataScope().equals(scope))
                .forEach(c -> c.setRevokedAt(OffsetDateTime.now()));
    }

    @Transactional
    public int saveObservations(Long userId, HealthObservationBatchRequest request) {
        String provider = normalizeProvider(request.getProvider());
        if (!HEALTH_CONNECT.equals(provider)) throw new IllegalArgumentException("Unsupported health data provider");

        for (HealthObservationBatchRequest.Observation input : request.getObservations()) {
            String type = input.getType().trim().toUpperCase(Locale.ROOT);
            if (!TYPES.contains(type)) throw new IllegalArgumentException("Unsupported health observation type");
            String scope = type.equals("STEPS") ? "STEPS" : "WORKOUTS";
            if (!consentRepository.existsByUserIdAndProviderAndDataScopeAndRevokedAtIsNull(userId, provider, scope)) {
                throw new IllegalStateException("Active consent is required for " + scope);
            }
            if (input.getEndedAt().isBefore(input.getStartedAt())) {
                throw new IllegalArgumentException("Observation end time cannot precede its start time");
            }

            HealthObservation observation = observationRepository
                    .findByUserIdAndProviderAndSourceRecordId(userId, provider, input.getSourceRecordId())
                    .orElseGet(() -> HealthObservation.builder()
                            .userId(userId).provider(provider).sourceRecordId(input.getSourceRecordId()).build());
            observation.setSourceApp(input.getSourceApp());
            observation.setObservationType(type);
            observation.setNumericValue(input.getNumericValue());
            observation.setUnit(input.getUnit());
            observation.setStartedAt(input.getStartedAt());
            observation.setEndedAt(input.getEndedAt());
            observationRepository.save(observation);
        }
        return request.getObservations().size();
    }

    public List<HealthObservationDto> getObservations(Long userId, OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("A valid from/to range is required");
        }
        return observationRepository
                .findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(userId, from, to)
                .stream().map(o -> HealthObservationDto.builder()
                        .id(o.getId()).provider(o.getProvider()).sourceRecordId(o.getSourceRecordId())
                        .sourceApp(o.getSourceApp()).type(o.getObservationType()).numericValue(o.getNumericValue())
                        .unit(o.getUnit()).startedAt(o.getStartedAt()).endedAt(o.getEndedAt())
                        .lastReceivedAt(o.getUpdatedAt()).build())
                .toList();
    }

    @Transactional
    public long deleteObservations(Long userId, String scopeName) {
        String scope = normalizeScope(scopeName);
        if (!SCOPES.contains(scope)) throw new IllegalArgumentException("Unsupported health data scope");
        String type = scope.equals("STEPS") ? "STEPS" : "WORKOUT";
        return observationRepository.deleteByUserIdAndObservationType(userId, type);
    }

    private String normalizeProvider(String value) {
        String provider = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!HEALTH_CONNECT.equals(provider)) throw new IllegalArgumentException("Unsupported health data provider");
        return provider;
    }

    private String normalizeScope(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
