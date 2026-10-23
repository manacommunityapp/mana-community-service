package com.manacommunity.api.homeservices.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StaffJobPostResponse {
    private Long id;
    private String title;
    private String role;
    private String description;
    private String salaryRange;
    private String shiftPreference;
    private List<String> requirements;
    private String status;
    private Integer applicantCount;
    private String postedByName;
    private LocalDateTime createdAt;
}
