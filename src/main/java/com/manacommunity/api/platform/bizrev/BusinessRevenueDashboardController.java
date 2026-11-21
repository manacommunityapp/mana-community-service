package com.manacommunity.api.platform.bizrev;

import com.manacommunity.api.platform.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/revenue")
@RequiredArgsConstructor
public class BusinessRevenueDashboardController {

    private final BusinessRevenueDashboardService service;

    @GetMapping("/snapshot")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<BusinessRevenueDashboardSnapshot>> getSnapshot() {
        return ResponseEntity.ok(ApiResponse.ok(service.getSnapshot()));
    }

    @PostMapping("/snapshot/refresh")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<BusinessRevenueDashboardSnapshot>> refresh() {
        return ResponseEntity.ok(ApiResponse.ok(service.refreshSnapshot()));
    }
}
