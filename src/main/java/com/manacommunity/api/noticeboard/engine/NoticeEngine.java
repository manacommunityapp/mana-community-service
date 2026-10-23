package com.manacommunity.api.noticeboard.engine;

import com.manacommunity.api.noticeboard.dto.NoticeStatsResponse;
import com.manacommunity.api.noticeboard.entity.Notice;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class NoticeEngine {

    public boolean isUserEligibleForNotice(Notice notice, AppUser user, String userBlock, boolean isOwner) {
        if (notice == null || user == null) {
            return false;
        }

        // Check expiration
        if (notice.getExpiresOn() != null && LocalDate.now().isAfter(notice.getExpiresOn())) {
            return false;
        }

        // Check scheduled status
        if (notice.getStatus() == Notice.NoticeStatus.SCHEDULED && notice.getScheduledPublishAt() != null) {
            if (LocalDateTime.now().isBefore(notice.getScheduledPublishAt())) {
                return false;
            }
        }

        // Target audience segmentation check
        Notice.TargetAudience audience = notice.getTargetAudience() != null ? notice.getTargetAudience() : Notice.TargetAudience.ALL;
        switch (audience) {
            case OWNERS_ONLY -> {
                if (!isOwner) return false;
            }
            case TENANTS_ONLY -> {
                if (isOwner) return false;
            }
            case BLOCK_SPECIFIC -> {
                if (notice.getTargetBlock() != null && !notice.getTargetBlock().isBlank()) {
                    if (userBlock == null || !userBlock.equalsIgnoreCase(notice.getTargetBlock().trim())) {
                        return false;
                    }
                }
            }
            case COMMITTEE_ONLY -> {
                String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
                if (!role.contains("ADMIN") && !role.contains("COMMITTEE")) {
                    return false;
                }
            }
            case ALL -> {}
        }

        return true;
    }

    public NoticeStatsResponse computeStats(Notice notice, long totalCommunityResidents, long readCount, long ackCount) {
        double readPct = totalCommunityResidents > 0 ? (readCount * 100.0 / totalCommunityResidents) : 0.0;
        double ackPct = totalCommunityResidents > 0 ? (ackCount * 100.0 / totalCommunityResidents) : 0.0;

        return NoticeStatsResponse.builder()
                .noticeId(notice.getId())
                .totalTargetUsers(totalCommunityResidents)
                .totalReads(readCount)
                .readPercentage(BigDecimal.valueOf(readPct).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .totalAcknowledgements(ackCount)
                .acknowledgementPercentage(BigDecimal.valueOf(ackPct).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .requiresAcknowledgement(notice.isRequiresAcknowledgement())
                .build();
    }
}
