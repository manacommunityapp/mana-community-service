package com.manacommunity.api.localization.dto;

import java.util.List;
import java.util.Map;

public final class LocalizationDtos {

    private LocalizationDtos() {}

    public record FontProfileDto(
            String family,
            String fontUrl,
            double lineHeightMultiplier
    ) {}

    public record NamespaceManifestEntryDto(
            String namespace,
            String versionHash,
            String bundleUrl,
            String audioPackUrl,
            int stringCount
    ) {}

    public record OtaManifestResponseDto(
            String locale,
            String direction,
            FontProfileDto fontProfile,
            Map<String, NamespaceManifestEntryDto> namespaces
    ) {}

    public record PublishBundleRequest(
            String locale,
            String namespace,
            Map<String, String> strings,
            Map<String, String> ttsHints,
            String audioPackUrl
    ) {}

    public record OtaBundleDto(
            String bundleId,
            String locale,
            String namespace,
            String versionHash,
            String direction,
            FontProfileDto fontProfile,
            Map<String, String> strings,
            Map<String, String> ttsHints,
            String audioPackUrl,
            String publishedAt
    ) {}

    public record TtsSynthesizeRequest(
            String locale,
            String voiceId,
            String text
    ) {}

    public record TtsAudioResponseDto(
            String cacheKey,
            String locale,
            String voiceId,
            String normalizedText,
            String audioUrl,
            String codec,
            int durationMs,
            int sizeBytes,
            boolean cacheHit
    ) {}

    public record VoiceGateEntryRequest(
            long societyId,
            String guardUserId,
            String flatNumber,
            String guardLocale,
            String residentLocale,
            String audioCodec,
            int durationMs,
            int audioSizeBytes,
            String spokenUtteranceHint
    ) {}

    public record VoiceGateEntryDto(
            String entryId,
            long societyId,
            String guardUserId,
            String flatNumber,
            String guardLocale,
            String residentLocale,
            String audioUrl,
            String audioCodec,
            int durationMs,
            int audioSizeBytes,
            String transcriptRaw,
            String transcriptTranslated,
            String detectedVisitorName,
            String detectedOrgName,
            Double entityConfidence,
            String status,
            String expiresAt,
            String createdAt
    ) {}

    public record VoiceAsrUpdateRequest(
            String transcriptRaw,
            String transcriptTranslated,
            String detectedVisitorName,
            String detectedOrgName,
            double entityConfidence
    ) {}
}
