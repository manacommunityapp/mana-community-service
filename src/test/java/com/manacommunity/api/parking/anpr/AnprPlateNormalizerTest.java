package com.manacommunity.api.parking.anpr;

import com.manacommunity.api.parking.anpr.service.AnprPlateNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ANPR Plate Normalizer Unit Tests")
class AnprPlateNormalizerTest {

    private final AnprPlateNormalizer normalizer = new AnprPlateNormalizer();

    @Test
    @DisplayName("Should normalise uppercase and strip spaces")
    void shouldNormaliseUppercaseAndStripSpaces() {
        assertThat(normalizer.normalize("mh 12 ab 1234")).isEqualTo("MH12AB1234");
    }

    @Test
    @DisplayName("Should strip hyphens")
    void shouldStripHyphens() {
        assertThat(normalizer.normalize("MH-12-AB-1234")).isEqualTo("MH12AB1234");
    }

    @Test
    @DisplayName("Should match plates with different spacing")
    void shouldMatchWithDifferentSpacing() {
        assertThat(normalizer.matches("MH 12 AB 1234", "MH12AB1234")).isTrue();
    }

    @Test
    @DisplayName("Should return false for different plates")
    void shouldReturnFalseForDifferentPlates() {
        assertThat(normalizer.matches("MH12AB1234", "DL01CX5678")).isFalse();
    }
}
