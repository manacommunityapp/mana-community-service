package com.manacommunity.api.communityops.controlroom;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsControlRoomController {

    private final OpsControlRoomService controlRoomService;
    private final RwaReportService reportService;

    @GetMapping("/control-room/summary")
    public ResponseEntity<OpsControlRoomSummary> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(controlRoomService.getSummary(principal.getCommunityId()));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<RwaMonthlyReport> monthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reportService.generateMonthlyReport(principal.getCommunityId(), year, month));
    }
}
