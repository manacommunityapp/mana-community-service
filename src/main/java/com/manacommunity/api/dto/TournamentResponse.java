package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TournamentResponse {
    private Long id;
    private String name;
    private Integer maxParticipants;
    private String description;
    private LocalDate eventDateStart;
    private LocalDate eventDateEnd;
    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;
    private String startTime;
    private String dueTime;
    private String bannerImage;
    private String contactName;
    private String contactNumber;
    private String contactEmail;
    private String otherContacts;
    private Boolean allowAdminChat;
    private String registrationStatus;
    private List<SponsorDto> sponsors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
