package com.manacommunity.api.dto.admin;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunitySettings;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommunitySettingsResponse {
    private Long   id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String inviteCode;
    private int    maxMembers;
    private Features features;

    @Data
    @Builder
    public static class Features {
        private boolean marketplace;
        private boolean sports;
        private boolean auction;
        private boolean jobs;
        private boolean polls;
    }

    public static CommunitySettingsResponse from(Community c, CommunitySettings s) {
        // s may be null — fall back to defaults
        boolean hasSettings = s != null;
        return CommunitySettingsResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(hasSettings ? s.getDescription() : null)
                .address(hasSettings ? s.getAddress() : c.getArea())
                .city(c.getCity())
                .state(c.getState())
                .inviteCode(c.getInviteCode())
                .maxMembers(hasSettings && s.getMaxMembers() != null ? s.getMaxMembers() : 500)
                .features(Features.builder()
                        .marketplace(hasSettings ? Boolean.TRUE.equals(s.getFeatureMarketplace()) : true)
                        .sports(hasSettings     ? Boolean.TRUE.equals(s.getFeatureSports())      : true)
                        .auction(hasSettings    ? Boolean.TRUE.equals(s.getFeatureAuction())     : true)
                        .jobs(hasSettings       ? Boolean.TRUE.equals(s.getFeatureJobs())        : true)
                        .polls(hasSettings      ? Boolean.TRUE.equals(s.getFeaturePolls())       : true)
                        .build())
                .build();
    }
}
