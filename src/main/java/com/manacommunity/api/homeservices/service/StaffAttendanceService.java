package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.dto.AttendanceSummaryResponse;
import com.manacommunity.api.homeservices.dto.StaffAttendanceRequest;
import com.manacommunity.api.homeservices.dto.StaffAttendanceResponse;
import com.manacommunity.api.user.model.AppUser;

import java.time.LocalDate;
import java.util.List;

public interface StaffAttendanceService {

    List<StaffAttendanceResponse> getAttendance(Long communityId, LocalDate date);

    List<AttendanceSummaryResponse> getAttendanceSummary(Long communityId, LocalDate from, LocalDate to);

    StaffAttendanceResponse markAttendance(StaffAttendanceRequest request, AppUser markedBy);
}
