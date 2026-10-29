package com.manacommunity.api.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KarateAttendanceResponse {
    private Long batchId;
    private String batchName;
    private long totalEnrolled;
    private List<EnrolleeStats> enrollees;

    @Data
    @AllArgsConstructor
    public static class EnrolleeStats {
        private Long userId;
        private String userName;
        private int classesAttended;
        private int totalClasses;
        private double attendancePct;
        private boolean lowAttendanceAlert;  // true when < 75%
    }
}
