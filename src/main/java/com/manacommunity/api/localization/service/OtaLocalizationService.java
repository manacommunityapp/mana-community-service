package com.manacommunity.api.localization.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.manacommunity.api.localization.dto.LocalizationDtos.*;
import com.manacommunity.api.localization.entity.OtaTranslationBundleEntity;
import com.manacommunity.api.localization.entity.TtsAudioCacheEntity;
import com.manacommunity.api.localization.repository.OtaTranslationBundleRepository;
import com.manacommunity.api.localization.repository.TtsAudioCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OtaLocalizationService {

    private static final Set<String> RTL_LOCALES = Set.of("ur", "ar");

    private final OtaTranslationBundleRepository bundleRepository;
    private final TtsAudioCacheRepository ttsCacheRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OtaBundleDto publishBundle(PublishBundleRequest req) {
        String locale = normalizeLocale(req.locale());
        String namespace = req.namespace() != null ? req.namespace().trim().toLowerCase(Locale.ROOT) : "gate";
        Map<String, String> strings = req.strings() != null ? req.strings() : Map.of();
        Map<String, String> hints = req.ttsHints() != null ? req.ttsHints() : Map.of();
        String versionHash = sha256Hex(canonicalJson(Map.of("strings", strings, "ttsHints", hints)));

        bundleRepository.findTopByLocaleAndNamespaceAndActiveTrueOrderByPublishedAtDesc(locale, namespace)
                .ifPresent(prev -> {
                    if (!prev.getVersionHash().equals(versionHash)) {
                        prev.setActive(false);
                        bundleRepository.save(prev);
                    }
                });

        FontProfileDto fontProfile = fontProfileFor(locale);
        OtaTranslationBundleEntity entity = new OtaTranslationBundleEntity();
        entity.setBundleId(UUID.randomUUID());
        entity.setLocale(locale);
        entity.setNamespace(namespace);
        entity.setVersionHash(versionHash);
        entity.setDirection(RTL_LOCALES.contains(locale) ? "rtl" : "ltr");
        entity.setFontFamily(fontProfile.family());
        entity.setLineHeightMultiplier(fontProfile.lineHeightMultiplier());
        entity.setStringsJson(strings);
        entity.setTtsHintsJson(hints);
        entity.setAudioPackUrl(req.audioPackUrl() != null
                ? req.audioPackUrl()
                : "https://cdn.manacommunity.in/audio/" + locale + "/" + namespace + "/" + versionHash.substring(0, 12) + ".tar");
        entity.setActive(true);
        entity.setPublishedAt(Instant.now());
        bundleRepository.save(entity);

        return toBundleDto(entity);
    }

    @Transactional(readOnly = true)
    public OtaManifestResponseDto getManifest(String rawLocale) {
        String locale = normalizeLocale(rawLocale);
        List<OtaTranslationBundleEntity> active = bundleRepository.findByLocaleAndActiveTrue(locale);
        Map<String, NamespaceManifestEntryDto> namespaces = new LinkedHashMap<>();

        if (active.isEmpty()) {
            OtaBundleDto defaultGate = defaultGateBundle(locale);
            namespaces.put("gate", new NamespaceManifestEntryDto(
                    "gate",
                    defaultGate.versionHash(),
                    "/api/i18n/bundles/" + locale + "/gate",
                    defaultGate.audioPackUrl(),
                    defaultGate.strings().size()
            ));
        } else {
            for (OtaTranslationBundleEntity b : active) {
                namespaces.put(b.getNamespace(), new NamespaceManifestEntryDto(
                        b.getNamespace(),
                        b.getVersionHash(),
                        "/api/i18n/bundles/" + locale + "/" + b.getNamespace(),
                        b.getAudioPackUrl(),
                        b.getStringsJson() != null ? b.getStringsJson().size() : 0
                ));
            }
        }

        return new OtaManifestResponseDto(
                locale,
                RTL_LOCALES.contains(locale) ? "rtl" : "ltr",
                fontProfileFor(locale),
                namespaces
        );
    }

    @Transactional(readOnly = true)
    public OtaBundleDto getBundle(String rawLocale, String namespace) {
        String locale = normalizeLocale(rawLocale);
        String ns = namespace != null ? namespace.trim().toLowerCase(Locale.ROOT) : "gate";
        return bundleRepository.findTopByLocaleAndNamespaceAndActiveTrueOrderByPublishedAtDesc(locale, ns)
                .map(this::toBundleDto)
                .orElseGet(() -> defaultGateBundle(locale));
    }

    @Transactional
    public TtsAudioResponseDto synthesizeOrGetCached(TtsSynthesizeRequest req) {
        String locale = normalizeLocale(req.locale());
        String voiceId = (req.voiceId() != null && !req.voiceId().isBlank()) ? req.voiceId() : defaultVoiceFor(locale);
        String normalized = req.text() != null ? req.text().trim().replaceAll("\\s+", " ") : "";
        String cacheKey = sha256Hex(locale + ":" + voiceId + ":" + normalized);

        TtsAudioCacheEntity existing = ttsCacheRepository.findById(cacheKey).orElse(null);
        if (existing != null) {
            return new TtsAudioResponseDto(
                    existing.getCacheKey(),
                    existing.getLocale(),
                    existing.getVoiceId(),
                    existing.getNormalizedText(),
                    existing.getAudioUrl(),
                    existing.getCodec(),
                    existing.getDurationMs(),
                    existing.getSizeBytes(),
                    true
            );
        }

        int durationMs = Math.max(900, Math.min(8000, normalized.length() * 65));
        int sizeBytes = Math.max(2048, (durationMs / 1000) * 2500);
        String audioUrl = "https://cdn.manacommunity.in/tts/" + locale + "/" + cacheKey + ".opus";

        TtsAudioCacheEntity created = new TtsAudioCacheEntity();
        created.setCacheKey(cacheKey);
        created.setLocale(locale);
        created.setVoiceId(voiceId);
        created.setNormalizedText(normalized);
        created.setAudioUrl(audioUrl);
        created.setCodec("OPUS_16K");
        created.setDurationMs(durationMs);
        created.setSizeBytes(sizeBytes);
        ttsCacheRepository.save(created);

        return new TtsAudioResponseDto(
                cacheKey,
                locale,
                voiceId,
                normalized,
                audioUrl,
                "OPUS_16K",
                durationMs,
                sizeBytes,
                false
        );
    }

    public FontProfileDto fontProfileFor(String locale) {
        return switch (locale) {
            case "te" -> new FontProfileDto("NotoSansTelugu", "https://cdn.manacommunity.in/fonts/NotoSansTelugu.ttf", 1.5);
            case "kn" -> new FontProfileDto("NotoSansKannada", "https://cdn.manacommunity.in/fonts/NotoSansKannada.ttf", 1.5);
            case "ta" -> new FontProfileDto("NotoSansTamil", "https://cdn.manacommunity.in/fonts/NotoSansTamil.ttf", 1.48);
            case "hi", "mr" -> new FontProfileDto("NotoSansDevanagari", "https://cdn.manacommunity.in/fonts/NotoSansDevanagari.ttf", 1.45);
            case "ur" -> new FontProfileDto("NotoNastaliqUrdu", "https://cdn.manacommunity.in/fonts/NotoNastaliqUrdu.ttf", 1.65);
            default -> new FontProfileDto("DMSans", "https://cdn.manacommunity.in/fonts/DMSans.ttf", 1.25);
        };
    }

    private OtaBundleDto defaultGateBundle(String locale) {
        Map<String, String> strings = switch (locale) {
            case "te" -> Map.of(
                    "gate.approve", "అనుమతించు",
                    "gate.deny", "తిరస్కరించు",
                    "gate.holdToSpeak", "పేరు చెప్పడానికి నొక్కి పట్టుకోండి (2 సెకన్లు)",
                    "gate.audioPrompt", "సందర్శకుడిని అనుమతించడానికి ఆకుపచ్చ బటన్ నొక్కండి"
            );
            case "hi" -> Map.of(
                    "gate.approve", "अनुमति दें",
                    "gate.deny", "अस्वीकार करें",
                    "gate.holdToSpeak", "नाम बोलने के लिए दबाकर रखें (2 सेकंड)",
                    "gate.audioPrompt", "विज़िटर को अनुमति देने के लिए हरा बटन दबाएं"
            );
            case "kn" -> Map.of(
                    "gate.approve", "ಅನುಮತಿಸಿ",
                    "gate.deny", "ನಿರಾಕರಿಸಿ",
                    "gate.holdToSpeak", "ಹೆಸರು ಹೇಳಲು ಒತ್ತಿ ಹಿಡಿಯಿರಿ (2 ಸೆಕೆಂಡ್)",
                    "gate.audioPrompt", "ಸಂದರ್ಶಕರನ್ನು ಅನುಮತಿಸಲು ಹಸಿರು ಬಟನ್ ಒತ್ತಿ"
            );
            case "ta" -> Map.of(
                    "gate.approve", "அனுமதி",
                    "gate.deny", "நிராகரி",
                    "gate.holdToSpeak", "பெயர் சொல்ல அழுத்திப் பிடிக்கவும் (2 வினாடிகள்)",
                    "gate.audioPrompt", "பார்வையாளரை அனுமதிக்க பச்சை பொத்தானை அழுத்தவும்"
            );
            case "mr" -> Map.of(
                    "gate.approve", "परवानगी द्या",
                    "gate.deny", "नाकारा",
                    "gate.holdToSpeak", "नाव सांगण्यासाठी दाबून धरा (2 सेकंद)",
                    "gate.audioPrompt", "अभ्यागताला परवानगी देण्यासाठी हिरवे बटण दाबा"
            );
            case "ur" -> Map.of(
                    "gate.approve", "منظور کریں",
                    "gate.deny", "مسترد کریں",
                    "gate.holdToSpeak", "نام بولنے کے لیے دبا کر رکھیں",
                    "gate.audioPrompt", "ملاقاتی کو اندر آنے دینے کے لیے سبز بٹن دبائیں"
            );
            default -> Map.of(
                    "gate.approve", "Approve",
                    "gate.deny", "Deny",
                    "gate.holdToSpeak", "Hold to record visitor name (2s)",
                    "gate.audioPrompt", "Tap green button to approve visitor entry"
            );
        };
        Map<String, String> hints = Map.of("gate.audioPrompt", strings.get("gate.audioPrompt"));
        String hash = sha256Hex(canonicalJson(strings));
        return new OtaBundleDto(
                UUID.nameUUIDFromBytes((locale + ":gate:" + hash).getBytes(StandardCharsets.UTF_8)).toString(),
                locale,
                "gate",
                hash,
                RTL_LOCALES.contains(locale) ? "rtl" : "ltr",
                fontProfileFor(locale),
                strings,
                hints,
                "https://cdn.manacommunity.in/audio/" + locale + "/gate/" + hash.substring(0, 12) + ".tar",
                Instant.now().toString()
        );
    }

    private String defaultVoiceFor(String locale) {
        return switch (locale) {
            case "te" -> "te-IN-ShrutiNeural";
            case "kn" -> "kn-IN-SapnaNeural";
            case "ta" -> "ta-IN-PallaviNeural";
            case "hi" -> "hi-IN-SwaraNeural";
            case "mr" -> "mr-IN-AarohiNeural";
            case "ur" -> "ur-IN-GulNeural";
            default -> "en-IN-NeerjaNeural";
        };
    }

    private String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) return "en";
        String base = raw.trim().toLowerCase(Locale.ROOT).split("[-_]")[0];
        return Set.of("en", "te", "kn", "ta", "hi", "mr", "ur").contains(base) ? base : "en";
    }

    private OtaBundleDto toBundleDto(OtaTranslationBundleEntity e) {
        return new OtaBundleDto(
                e.getBundleId().toString(),
                e.getLocale(),
                e.getNamespace(),
                e.getVersionHash(),
                e.getDirection(),
                new FontProfileDto(e.getFontFamily(), fontProfileFor(e.getLocale()).fontUrl(), e.getLineHeightMultiplier()),
                e.getStringsJson(),
                e.getTtsHintsJson(),
                e.getAudioPackUrl(),
                e.getPublishedAt().toString()
        );
    }

    public String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String canonicalJson(Object value) {
        try {
            ObjectMapper sorted = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return sorted.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Canonical JSON serialization failed", ex);
        }
    }
}
