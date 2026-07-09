package com.manacommunity.api.polling.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PollResponse {

    private Long id;
    private String question;
    private String description;
    private String closesOn;
    private boolean allowMultiple;
    private boolean anonymous;
    private Long createdById;
    private String createdByName;
    private Long communityId;
    private String createdAt;
    private boolean closed;
    private boolean hasVoted;
    private int totalVotes;
    private List<OptionDto> options;

    @Data
    @Builder
    public static class OptionDto {
        private Long id;
        private String text;
        private int sortOrder;
        private long voteCount;
        private boolean selected;
    }
}
