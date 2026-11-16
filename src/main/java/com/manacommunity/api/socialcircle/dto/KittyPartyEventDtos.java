package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class KittyPartyEventDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateEventRequest {
        @NotBlank(message = "Event name is required")
        private String eventName;

        private String description;

        @Builder.Default
        private String eventType = "KITTY_MONTHLY";

        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;

        @Builder.Default
        private String timezone = "Asia/Kolkata";

        @Builder.Default
        private String venueType = "HOST_HOME";

        private String venueAddress;

        private Long hostUserId;

        private Integer maxAttendees;

        @Builder.Default
        private String recurrenceRule = "NONE";
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEventRequest {
        private String eventName;
        private String description;
        private String eventType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String timezone;
        private String venueType;
        private String venueAddress;
        private Long hostUserId;
        private Integer maxAttendees;
        private String recurrenceRule;
        private String status;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventResponse {
        private Long id;
        private Long circleId;
        private String circleName;
        private String eventName;
        private String description;
        private String eventType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String timezone;
        private String venueType;
        private String venueAddress;
        private Long hostUserId;
        private String hostName;
        private Integer maxAttendees;
        private String recurrenceRule;
        private String status;
        private long goingCount;
        private long maybeCount;
        private long notGoingCount;
        private String currentUserRsvp;
        private String gatePassCode;
        private LocalDateTime gatePassGeneratedAt;
        private Long createdById;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RsvpRequest {
        @NotBlank(message = "Response is required (GOING, NOT_GOING, MAYBE, WAITLISTED)")
        private String response;

        @Builder.Default
        private Integer guestCount = 0;

        private String notes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RsvpResponse {
        private Long id;
        private Long eventId;
        private Long userId;
        private String userName;
        private String response;
        private Integer guestCount;
        private String notes;
        private String gatePassCode;
        private LocalDateTime gatePassGeneratedAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkAttendanceRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Attendance status is required (PRESENT, ABSENT, LATE)")
        private String attendanceStatus;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceResponse {
        private Long id;
        private Long eventId;
        private Long userId;
        private String userName;
        private String attendanceStatus;
        private Long markedById;
        private String markedByName;
        private LocalDateTime markedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSummaryDto {
        private long totalEvents;
        private long upcomingEvents;
        private long pastEvents;
        private List<EventResponse> events;
    }
}
