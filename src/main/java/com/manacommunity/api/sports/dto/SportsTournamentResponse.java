package com.manacommunity.api.sports.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.manacommunity.api.dto.ContactDto;
import com.manacommunity.api.dto.SponsorDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportsTournamentResponse {
    private Long id;
    private String name;
    private Long communityId;
    private String communityName;
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
    private Long contactId;
    private String contactTitle;
    private List<ContactDto> contacts;
    private String otherContacts;
    private Boolean allowAdminChat;
    private String registrationStatus;
    private List<SponsorDto> sponsors;
    private List<SportsEventResponse> sportsEvents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("events")
    public List<SportsEventResponse> getEvents() {
        return sportsEvents;
    }

    @JsonProperty("events")
    public void setEvents(List<SportsEventResponse> events) {
        if (this.sportsEvents == null) {
            this.sportsEvents = events;
        }
    }

    @JsonProperty("childEvents")
    public List<SportsEventResponse> getChildEvents() {
        return sportsEvents;
    }

    @JsonProperty("childEvents")
    public void setChildEvents(List<SportsEventResponse> childEvents) {
        if (this.sportsEvents == null) {
            this.sportsEvents = childEvents;
        }
    }

    @JsonProperty("status")
    public String getStatus() {
        return registrationStatus;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        if (this.registrationStatus == null) {
            this.registrationStatus = status;
        }
    }
}
