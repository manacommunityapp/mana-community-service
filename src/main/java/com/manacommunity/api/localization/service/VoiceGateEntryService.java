package com.manacommunity.api.localization.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.localization.dto.LocalizationDtos.*;
import com.manacommunity.api.localization.entity.VoiceGateEntryEntity;
import com.manacommunity.api.localization.repository.VoiceGateEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-latency Voice-First Gate Entry pipeline:
 * 1) Accepts ~5 KB 2-second Opus audio clip and immediately returns DISPATCHED_AUDIO for lock-screen playback.
 * 2) Extracts brand/person entities from code-switched Indic speech asynchronously (or via hint/ASR webhook).
 * 3) Enforces 7-day DPDP audio retention expiry.
 */
@Service
@RequiredArgsConstructor
public class VoiceGateEntryService {

    private static final List<String> KNOWN_BRANDS = List.of(
            "Urban Company", "Swiggy", "Zomato", "Blinkit", "Zepto", "Amazon", "Flipkart", "BlueDart", "BigBasket"
    );
    private static final Pattern NAME_AFTER_CONNECTOR = Pattern.compile(
            "(?:from|nundi|se|inda|irundhu|pasun)\\s+([A-Za-z]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final VoiceGateEntryRepository entryRepository;

    @Transactional
    public VoiceGateEntryDto createVoiceEntry(VoiceGateEntryRequest req) {
        UUID entryId = UUID.randomUUID();
        Instant now = Instant.now();

        VoiceGateEntryEntity entity = new VoiceGateEntryEntity();
        entity.setEntryId(entryId);
        entity.setSocietyId(req.societyId());
        entity.setGuardUserId(req.guardUserId() != null ? req.guardUserId() : "guard_01");
        entity.setFlatNumber(req.flatNumber() != null ? req.flatNumber() : "A-402");
        entity.setGuardLocale(req.guardLocale() != null ? req.guardLocale() : "te");
        entity.setResidentLocale(req.residentLocale() != null ? req.residentLocale() : "en");
        entity.setAudioCodec(req.audioCodec() != null ? req.audioCodec() : "OPUS_16K");
        entity.setDurationMs(req.durationMs() > 0 ? req.durationMs() : 2000);
        entity.setAudioSizeBytes(req.audioSizeBytes() > 0 ? req.audioSizeBytes() : 5120);
        entity.setAudioUrl("https://cdn.manacommunity.in/gate-voice/" + req.societyId() + "/" + entryId + ".opus");
        entity.setStatus("DISPATCHED_AUDIO");
        entity.setExpiresAt(now.plus(7, ChronoUnit.DAYS));

        if (req.spokenUtteranceHint() != null && !req.spokenUtteranceHint().isBlank()) {
            applyLexiconExtraction(entity, req.spokenUtteranceHint());
        }

        entryRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public VoiceGateEntryDto applyAsrTranscript(String entryId, VoiceAsrUpdateRequest req) {
        VoiceGateEntryEntity entity = entryRepository.findById(UUID.fromString(entryId))
                .orElseThrow(() -> new ResourceNotFoundException("VoiceGateEntry", "entryId", entryId));

        entity.setTranscriptRaw(req.transcriptRaw());
        entity.setTranscriptTranslated(req.transcriptTranslated());
        entity.setDetectedVisitorName(req.detectedVisitorName());
        entity.setDetectedOrgName(req.detectedOrgName());
        entity.setEntityConfidence(req.entityConfidence());
        entity.setStatus("TRANSCRIBED");
        entity.setTranscribedAt(Instant.now());
        entryRepository.save(entity);

        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<VoiceGateEntryDto> listRecent(long societyId) {
        return entryRepository.findTop30BySocietyIdOrderByCreatedAtDesc(societyId).stream()
                .map(this::toDto)
                .toList();
    }

    public void applyLexiconExtraction(VoiceGateEntryEntity entity, String rawUtterance) {
        entity.setTranscriptRaw(rawUtterance.trim());
        String matchedOrg = KNOWN_BRANDS.stream()
                .filter(b -> rawUtterance.toLowerCase(Locale.ROOT).contains(b.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);

        String detectedName = null;
        Matcher m = NAME_AFTER_CONNECTOR.matcher(rawUtterance);
        if (m.find()) {
            String candidate = m.group(1);
            if (matchedOrg == null || !matchedOrg.toLowerCase(Locale.ROOT).startsWith(candidate.toLowerCase(Locale.ROOT))) {
                detectedName = capitalize(candidate);
            }
        }
        if (detectedName == null) {
            for (String token : rawUtterance.trim().split("\\s+")) {
                String clean = token.replaceAll("[^A-Za-z]", "");
                if (clean.length() >= 3
                        && !SetOfConnectors.contains(clean.toLowerCase(Locale.ROOT))
                        && (matchedOrg == null || !matchedOrg.toLowerCase(Locale.ROOT).contains(clean.toLowerCase(Locale.ROOT)))) {
                    detectedName = capitalize(clean);
                    break;
                }
            }
        }

        entity.setDetectedOrgName(matchedOrg);
        entity.setDetectedVisitorName(detectedName);
        entity.setEntityConfidence(matchedOrg != null ? 0.94 : 0.80);
        entity.setTranscriptTranslated(
                (detectedName != null ? detectedName : "Visitor")
                        + (matchedOrg != null ? " · " + matchedOrg : "")
        );
        entity.setStatus("TRANSCRIBED");
        entity.setTranscribedAt(Instant.now());
    }

    private static final java.util.Set<String> SetOfConnectors = java.util.Set.of(
            "from", "nundi", "se", "inda", "irundhu", "pasun", "came", "for", "flat"
    );

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private VoiceGateEntryDto toDto(VoiceGateEntryEntity e) {
        return new VoiceGateEntryDto(
                e.getEntryId().toString(),
                e.getSocietyId(),
                e.getGuardUserId(),
                e.getFlatNumber(),
                e.getGuardLocale(),
                e.getResidentLocale(),
                e.getAudioUrl(),
                e.getAudioCodec(),
                e.getDurationMs(),
                e.getAudioSizeBytes(),
                e.getTranscriptRaw(),
                e.getTranscriptTranslated(),
                e.getDetectedVisitorName(),
                e.getDetectedOrgName(),
                e.getEntityConfidence(),
                e.getStatus(),
                e.getExpiresAt().toString(),
                e.getCreatedAt().toString()
        );
    }
}
