package com.manacommunity.api.localization.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.localization.dto.LocalizationDtos.*;
import com.manacommunity.api.localization.entity.OtaTranslationBundleEntity;
import com.manacommunity.api.localization.entity.TtsAudioCacheEntity;
import com.manacommunity.api.localization.repository.OtaTranslationBundleRepository;
import com.manacommunity.api.localization.repository.TtsAudioCacheRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OtaLocalizationServiceTest {

    @Test
    void servesTeluguAndRtlUrduFontProfilesAndCachesTtsByHash() {
        OtaTranslationBundleRepository bundleRepo = Mockito.mock(OtaTranslationBundleRepository.class);
        TtsAudioCacheRepository ttsRepo = Mockito.mock(TtsAudioCacheRepository.class);
        Map<String, TtsAudioCacheEntity> ttsStore = new ConcurrentHashMap<>();

        when(bundleRepo.findByLocaleAndActiveTrue(any())).thenReturn(java.util.List.of());
        when(bundleRepo.findTopByLocaleAndNamespaceAndActiveTrueOrderByPublishedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(ttsRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(ttsStore.get(inv.getArgument(0))));
        when(ttsRepo.save(any())).thenAnswer(inv -> {
            TtsAudioCacheEntity e = inv.getArgument(0);
            ttsStore.put(e.getCacheKey(), e);
            return e;
        });

        OtaLocalizationService service = new OtaLocalizationService(bundleRepo, ttsRepo, new ObjectMapper());

        OtaManifestResponseDto te = service.getManifest("te-IN");
        assertEquals("te", te.locale());
        assertEquals("ltr", te.direction());
        assertEquals("NotoSansTelugu", te.fontProfile().family());
        assertEquals(1.5, te.fontProfile().lineHeightMultiplier());

        OtaManifestResponseDto ur = service.getManifest("ur");
        assertEquals("rtl", ur.direction());
        assertEquals("NotoNastaliqUrdu", ur.fontProfile().family());

        TtsAudioResponseDto first = service.synthesizeOrGetCached(new TtsSynthesizeRequest("te", null, "  అనుమతించు  "));
        assertFalse(first.cacheHit());
        TtsAudioResponseDto second = service.synthesizeOrGetCached(new TtsSynthesizeRequest("te", null, "అనుమతించు"));
        assertTrue(second.cacheHit());
        assertEquals(first.cacheKey(), second.cacheKey());
    }
}
