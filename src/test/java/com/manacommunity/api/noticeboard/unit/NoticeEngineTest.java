package com.manacommunity.api.noticeboard.unit;

import com.manacommunity.api.noticeboard.dto.NoticeStatsResponse;
import com.manacommunity.api.noticeboard.engine.NoticeEngine;
import com.manacommunity.api.noticeboard.entity.Notice;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notice Engine Unit Tests")
class NoticeEngineTest {

    private NoticeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new NoticeEngine();
    }

    @Test
    @DisplayName("Should filter notice by audience tenancy")
    void testAudienceFiltering() {
        Notice ownerNotice = Notice.builder()
                .targetAudience(Notice.TargetAudience.OWNERS_ONLY)
                .status(Notice.NoticeStatus.PUBLISHED)
                .build();

        AppUser user = AppUser.builder().id(1L).build();

        assertTrue(engine.isUserEligibleForNotice(ownerNotice, user, "A", true));  // user is owner -> true
        assertFalse(engine.isUserEligibleForNotice(ownerNotice, user, "A", false)); // user is tenant -> false
    }

    @Test
    @DisplayName("Should filter notice by target block")
    void testBlockFiltering() {
        Notice blockANotice = Notice.builder()
                .targetAudience(Notice.TargetAudience.BLOCK_SPECIFIC)
                .targetBlock("Tower A")
                .status(Notice.NoticeStatus.PUBLISHED)
                .build();

        AppUser user = AppUser.builder().id(1L).build();

        assertTrue(engine.isUserEligibleForNotice(blockANotice, user, "Tower A", false));
        assertFalse(engine.isUserEligibleForNotice(blockANotice, user, "Tower B", false));
    }

    @Test
    @DisplayName("Should exclude expired or future scheduled notices")
    void testExpirationAndSchedule() {
        Notice expiredNotice = Notice.builder()
                .expiresOn(LocalDate.now().minusDays(1))
                .status(Notice.NoticeStatus.PUBLISHED)
                .build();

        Notice futureNotice = Notice.builder()
                .status(Notice.NoticeStatus.SCHEDULED)
                .scheduledPublishAt(LocalDateTime.now().plusHours(2))
                .build();

        AppUser user = AppUser.builder().id(1L).build();

        assertFalse(engine.isUserEligibleForNotice(expiredNotice, user, null, true));
        assertFalse(engine.isUserEligibleForNotice(futureNotice, user, null, true));
    }

    @Test
    @DisplayName("Should compute notice readership and acknowledgement rates")
    void testComputeStats() {
        Notice notice = Notice.builder().id(1L).requiresAcknowledgement(true).build();
        NoticeStatsResponse stats = engine.computeStats(notice, 100, 75, 50);

        assertNotNull(stats);
        assertEquals(100, stats.getTotalTargetUsers());
        assertEquals(75, stats.getTotalReads());
        assertEquals(75.0, stats.getReadPercentage());
        assertEquals(50, stats.getTotalAcknowledgements());
        assertEquals(50.0, stats.getAcknowledgementPercentage());
    }
}
