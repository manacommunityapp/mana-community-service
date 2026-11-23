package com.manacommunity.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Safe public payload for pre-login mobile & web welcome screens.
 * Never exposes invite codes, flat numbers, emails, phone numbers, or any PII.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPublicOverviewResponse {

    // Live DB aggregate counts
    private long totalResidents;
    private long totalSocieties;
    private long totalCities;

    // DB-stored public copy & security labels
    private String topBadgeText;
    private String heroCapsuleText;
    private String heroTitle;
    private String heroSubtitle;
    private String securityLabel;
    private String certificationNote;

    // Live active communities (sanitized public summaries)
    private List<PublicSocietySummary> featuredSocieties;

    // DB-stored showcase carousel items
    private List<ShowcaseItemDto> showcaseItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicSocietySummary {
        private Long id;
        private String name;
        private String type;
        private String city;
        private String area;
        private long residentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShowcaseItemDto {
        private String itemKey;
        private String emoji;
        private String accentColor;
        private String title;
        private String tagline;
        private String description;
        private List<String> tags;
        private int sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePublicConfigRequest {
        private String topBadgeText;
        private String heroCapsuleText;
        private String heroTitle;
        private String heroSubtitle;
        private String securityLabel;
        private String certificationNote;
    }
}
