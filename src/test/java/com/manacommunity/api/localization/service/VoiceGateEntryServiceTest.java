package com.manacommunity.api.localization.service;

import com.manacommunity.api.localization.dto.LocalizationDtos.*;
import com.manacommunity.api.localization.repository.VoiceGateEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class VoiceGateEntryServiceTest {

    @Test
    void extractsEntitiesFromCodeSwitchedTeluguEnglishSpeechAndSetsOpusAudio() {
        VoiceGateEntryRepository repo = Mockito.mock(VoiceGateEntryRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VoiceGateEntryService service = new VoiceGateEntryService(repo);

        // Non-blocking audio-only entry
        VoiceGateEntryDto audioOnly = service.createVoiceEntry(new VoiceGateEntryRequest(
                1L, "guard_hyd_01", "A-402", "te", "en", "OPUS_16K", 2000, 5120, null
        ));
        assertEquals("DISPATCHED_AUDIO", audioOnly.status());
        assertTrue(audioOnly.audioUrl().endsWith(".opus"));

        // Code-switched utterance: "Urban Company nundi Ramesh"
        VoiceGateEntryDto extracted = service.createVoiceEntry(new VoiceGateEntryRequest(
                1L, "guard_hyd_01", "A-402", "te", "en", "OPUS_16K", 2000, 5120,
                "Urban Company nundi Ramesh"
        ));
        assertEquals("TRANSCRIBED", extracted.status());
        assertEquals("Urban Company", extracted.detectedOrgName());
        assertEquals("Ramesh", extracted.detectedVisitorName());
        assertEquals("Ramesh · Urban Company", extracted.transcriptTranslated());
    }
}
