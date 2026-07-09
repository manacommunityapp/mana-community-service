package com.manacommunity.api.jobs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobResponse {

    private Long id;
    private String title;
    private String company;
    private String description;
    private String location;
    private String jobType;
    private String salary;
    private boolean referral;
    private String contactEmail;
    private String status;
    private Long postedById;
    private String postedByName;
    private Long communityId;
    private int applicationCount;
    private boolean hasApplied;
    private String createdAt;
}
