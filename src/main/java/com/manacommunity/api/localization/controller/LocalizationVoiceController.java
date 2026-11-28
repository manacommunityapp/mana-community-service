package com.manacommunity.api.localization.controller;

import com.manacommunity.api.localization.dto.LocalizationDtos.*;
import com.manacommunity.api.localization.service.OtaLocalizationService;
import com.manacommunity.api.localization.service.VoiceGateEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LocalizationVoiceController {

    private final OtaLocalizationService localizationService;
    private final VoiceGateEntryService voiceGateEntryService;

    @GetMapping("/i18n/manifest")
    public ResponseEntity<OtaManifestResponseDto> getOtaManifest(@RequestParam(defaultValue = "en") String locale) {
        return ResponseEntity.ok(localizationService.getManifest(locale));
    }

    @GetMapping("/i18n/bundles/{locale}/{namespace}")
    public ResponseEntity<OtaBundleDto> getOtaBundle(
            @PathVariable String locale,
            @PathVariable String namespace) {
        return ResponseEntity.ok(localizationService.getBundle(locale, namespace));
    }

    @PostMapping("/i18n/bundles")
    public ResponseEntity<OtaBundleDto> publishBundle(@RequestBody PublishBundleRequest request) {
        return ResponseEntity.ok(localizationService.publishBundle(request));
    }

    @PostMapping("/tts/synthesize")
    public ResponseEntity<TtsAudioResponseDto> synthesizeTts(@RequestBody TtsSynthesizeRequest request) {
        return ResponseEntity.ok(localizationService.synthesizeOrGetCached(request));
    }

    @PostMapping("/gate/voice-entry")
    public ResponseEntity<VoiceGateEntryDto> createVoiceEntry(@RequestBody VoiceGateEntryRequest request) {
        return ResponseEntity.ok(voiceGateEntryService.createVoiceEntry(request));
    }

    @PostMapping("/gate/voice-entry/{entryId}/asr")
    public ResponseEntity<VoiceGateEntryDto> applyAsrTranscript(
            @PathVariable String entryId,
            @RequestBody VoiceAsrUpdateRequest request) {
        return ResponseEntity.ok(voiceGateEntryService.applyAsrTranscript(entryId, request));
    }

    @GetMapping("/gate/voice-entries")
    public ResponseEntity<List<VoiceGateEntryDto>> listVoiceEntries(@RequestParam long societyId) {
        return ResponseEntity.ok(voiceGateEntryService.listRecent(societyId));
    }
}
