package com.manacommunity.api.sports.model;

import com.manacommunity.api.sports.model.SportsEvent.MatchFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SportsEvent Format Reconciliation")
class SportsEventFormatReconciliationTest {

    @Test
    @DisplayName("preserves existing SportsEventFormat instances and their DB IDs when updating with same format")
    void preservesExistingEntitiesAndIdsOnUpdate() {
        SportsEvent event = new SportsEvent();
        SportsEventFormat existingFormat = SportsEventFormat.builder()
                .id(42L)
                .event(event)
                .format(MatchFormat.SINGLES)
                .build();
        event.getEventFormats().add(existingFormat);

        // Update event with format: [SINGLES]
        event.setFormat(List.of(MatchFormat.SINGLES));

        assertThat(event.getEventFormats()).hasSize(1);
        SportsEventFormat current = event.getEventFormats().get(0);
        // CRITICAL: Must be the EXACT same instance and ID to prevent Hibernate insert/delete constraint violations
        assertThat(current).isSameAs(existingFormat);
        assertThat(current.getId()).isEqualTo(42L);
        assertThat(current.getFormat()).isEqualTo(MatchFormat.SINGLES);
    }

    @Test
    @DisplayName("retains existing formats and appends only genuinely new formats")
    void retainsExistingAndAppendsNewFormat() {
        SportsEvent event = new SportsEvent();
        SportsEventFormat existingFormat = SportsEventFormat.builder()
                .id(101L)
                .event(event)
                .format(MatchFormat.SINGLES)
                .build();
        event.getEventFormats().add(existingFormat);

        // Update event with format: [SINGLES, DOUBLES]
        event.setFormat(List.of(MatchFormat.SINGLES, MatchFormat.DOUBLES));

        assertThat(event.getEventFormats()).hasSize(2);
        assertThat(event.getEventFormats().get(0)).isSameAs(existingFormat);
        assertThat(event.getEventFormats().get(0).getId()).isEqualTo(101L);

        SportsEventFormat added = event.getEventFormats().get(1);
        assertThat(added.getId()).isNull();
        assertThat(added.getFormat()).isEqualTo(MatchFormat.DOUBLES);
        assertThat(added.getEvent()).isSameAs(event);
    }

    @Test
    @DisplayName("removes only formats absent from target list, preserving remaining")
    void removesOnlyExcludedFormats() {
        SportsEvent event = new SportsEvent();
        SportsEventFormat singles = SportsEventFormat.builder()
                .id(10L)
                .event(event)
                .format(MatchFormat.SINGLES)
                .build();
        SportsEventFormat doubles = SportsEventFormat.builder()
                .id(20L)
                .event(event)
                .format(MatchFormat.DOUBLES)
                .build();
        event.getEventFormats().add(singles);
        event.getEventFormats().add(doubles);

        // Update to only DOUBLES
        event.setFormat(List.of(MatchFormat.DOUBLES));

        assertThat(event.getEventFormats()).hasSize(1);
        assertThat(event.getEventFormats().get(0)).isSameAs(doubles);
        assertThat(event.getEventFormats().get(0).getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("deduplicates incoming formats list preserving order")
    void deduplicatesIncomingFormats() {
        SportsEvent event = new SportsEvent();

        event.setFormat(List.of(MatchFormat.SINGLES, MatchFormat.SINGLES, MatchFormat.MIXED_DOUBLES));

        assertThat(event.getEventFormats()).hasSize(2);
        assertThat(event.getEventFormats().get(0).getFormat()).isEqualTo(MatchFormat.SINGLES);
        assertThat(event.getEventFormats().get(1).getFormat()).isEqualTo(MatchFormat.MIXED_DOUBLES);
    }

    @Test
    @DisplayName("clears formats when passed null or empty list")
    void clearsFormatsWhenEmptyOrNull() {
        SportsEvent event = new SportsEvent();
        event.getEventFormats().add(SportsEventFormat.builder().id(1L).format(MatchFormat.SINGLES).build());

        event.setFormat(new ArrayList<>());
        assertThat(event.getEventFormats()).isEmpty();

        event.getEventFormats().add(SportsEventFormat.builder().id(2L).format(MatchFormat.DOUBLES).build());
        event.setFormat(null);
        assertThat(event.getEventFormats()).isEmpty();
    }

    @Test
    @DisplayName("cleans up duplicates if event previously had duplicated formats")
    void cleansUpPreexistingDuplicates() {
        SportsEvent event = new SportsEvent();
        SportsEventFormat first = SportsEventFormat.builder().id(1L).format(MatchFormat.SINGLES).build();
        SportsEventFormat duplicate = SportsEventFormat.builder().id(2L).format(MatchFormat.SINGLES).build();
        event.getEventFormats().add(first);
        event.getEventFormats().add(duplicate);

        event.setFormat(List.of(MatchFormat.SINGLES));

        assertThat(event.getEventFormats()).hasSize(1);
        assertThat(event.getEventFormats().get(0)).isSameAs(first);
    }
}
