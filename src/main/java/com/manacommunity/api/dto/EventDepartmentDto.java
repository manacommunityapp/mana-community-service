package com.manacommunity.api.dto;

import com.manacommunity.api.model.EventDepartment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDepartmentDto {

    private Long id;
    private Long communityId;
    private Long eventId;
    private String name;
    private String headName;
    private Integer totalTarget;
    private Integer presentCount;
    private String color;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;

    public static EventDepartmentDto from(EventDepartment dept) {
        return EventDepartmentDto.builder()
                .id(dept.getId())
                .communityId(dept.getCommunityId())
                .eventId(dept.getEventId())
                .name(dept.getName())
                .headName(dept.getHeadName())
                .totalTarget(dept.getTotalTarget())
                .presentCount(dept.getPresentCount())
                .color(dept.getColor())
                .description(dept.getDescription())
                .active(dept.isActive())
                .createdAt(dept.getCreatedAt())
                .build();
    }
}
