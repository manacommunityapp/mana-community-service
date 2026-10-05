package com.manacommunity.api.calendar.dto;

import com.manacommunity.api.calendar.model.CalendarDomain;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CalendarDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarEventItemDto {
        private String id;
        private CalendarDomain domain;
        private String title;
        private String description;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean isAllDay;
        private NotificationPriority priority;
        private String badge;
        private String color;
        private String icon;
        private String organizerName;
        private String targetRoute;
        private String actionLabel;
        private boolean isMyItem;
        private String googleCalendarUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCalendarEventRequest {
        @NotNull(message = "Domain is required")
        private CalendarDomain domain;

        @NotBlank(message = "Title is required")
        private String title;

        private String description;
        private String location;

        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;

        private boolean isAllDay;
        private NotificationPriority priority;
        private String badge;
        private String targetRoute;
        private String actionLabel;
        private String organizerName;
        private String targetTower;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarMonthSummaryDto {
        private String yearMonth;
        private Map<String, Integer> dateCounts;
        private Map<String, List<String>> dateDomains;
        private int totalEvents;
    }
}
