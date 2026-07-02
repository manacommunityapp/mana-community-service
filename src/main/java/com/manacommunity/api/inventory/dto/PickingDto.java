package com.manacommunity.api.inventory.dto;

import com.manacommunity.api.inventory.entity.PickingState;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickingDto {
    private Long id;
    private String name;
    private String origin;
    private PickingState state;
    private LocalDateTime scheduledDate;
    private Long locationId;
    private String locationName;
    private Long locationDestId;
    private String locationDestName;
    private Long pickingTypeId;
    private String pickingTypeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MoveDto> moves;
    private List<MoveLineDto> moveLines;
}
