package com.manacommunity.api.dto.admin;

import lombok.Data;

@Data
public class UpdateCommunitySettingsRequest {
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private Integer maxMembers;
    private Features features;

    @Data
    public static class Features {
        private Boolean marketplace;
        private Boolean sports;
        private Boolean auction;
        private Boolean jobs;
        private Boolean polls;
    }
}
