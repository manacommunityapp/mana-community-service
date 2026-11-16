package com.manacommunity.api.mw.dto;

import com.manacommunity.api.mw.entity.MwPoolListing;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class MwPoolListingPublicResponse {

    private Long id;
    private String title;
    private String description;
    private String requiredSkills;
    private LocalDate shiftDate;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private String location;
    private BigDecimal payRate;
    private String payType;
    private Integer maxWorkers;
    private Integer acceptedCount;
    private String status;

    public static MwPoolListingPublicResponse from(MwPoolListing e) {
        return MwPoolListingPublicResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .requiredSkills(e.getRequiredSkills())
                .shiftDate(e.getShiftDate())
                .shiftStart(e.getShiftStart())
                .shiftEnd(e.getShiftEnd())
                .location(e.getLocation())
                .payRate(e.getPayRate())
                .payType(e.getPayType())
                .maxWorkers(e.getMaxWorkers())
                .acceptedCount(e.getAcceptedCount())
                .status(e.getStatus())
                .build();
    }
}
