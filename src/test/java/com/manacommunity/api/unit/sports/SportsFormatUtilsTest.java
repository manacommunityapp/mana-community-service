package com.manacommunity.api.unit.sports;

import com.manacommunity.api.sports.util.SportsFormatUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SportsFormatUtilsTest {

    @Test
    void formatBlockAndFlat_combinesBlockAndFlat() {
        assertThat(SportsFormatUtils.formatBlockAndFlat("Block B", "1008")).isEqualTo("Block B, Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat("B", "1008")).isEqualTo("Block B, Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat("Tower 2", "401")).isEqualTo("Tower 2, Flat 401");
        assertThat(SportsFormatUtils.formatBlockAndFlat("Wing A", "102")).isEqualTo("Wing A, Flat 102");
    }

    @Test
    void formatBlockAndFlat_preservesAlreadyFormattedAddress() {
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, "Block B, Flat 1008")).isEqualTo("Block B, Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat("Block B", "Block B, Flat 1008")).isEqualTo("Block B, Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, "Villa 42")).isEqualTo("Villa 42");
    }

    @Test
    void formatBlockAndFlat_normalizesHyphenatedStrings() {
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, "B-1008")).isEqualTo("Block B, Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, "Tower 1-302")).isEqualTo("Tower 1, Flat 302");
    }

    @Test
    void formatBlockAndFlat_handlesFlatOnlyOrBlockOnly() {
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, "1008")).isEqualTo("Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, "Flat 1008")).isEqualTo("Flat 1008");
        assertThat(SportsFormatUtils.formatBlockAndFlat("Block B", null)).isEqualTo("Block B");
        assertThat(SportsFormatUtils.formatBlockAndFlat(null, null)).isNull();
        assertThat(SportsFormatUtils.formatBlockAndFlat("", "")).isNull();
    }
}
