package com.manacommunity.api.devices.controller;

import com.manacommunity.api.devices.dto.SmartDeviceDtos.*;
import com.manacommunity.api.devices.service.SmartDeviceService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class SmartDeviceController {

    private final SmartDeviceService deviceService;
    private final LoggedInUserService loggedInUserService;

    // ── Registry ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> listDevices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDevices(communityId, type)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDevice(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertFmOrAdmin(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(deviceService.registerDevice(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody RegisterDeviceRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertFmOrAdmin(principal);
        return ResponseEntity.ok(ApiResponse.ok(deviceService.updateDevice(id, req)));
    }

    // ── Control ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/control")
    public ResponseEntity<ApiResponse<DeviceControlResult>> controlDevice(
            @PathVariable Long id,
            @Valid @RequestBody ControlDeviceRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String role = user.getRole() != null ? user.getRole() : "RESIDENT";
        DeviceControlResult result = deviceService.controlDevice(
                id, req.action(), user.getId(), role, req.requestSource());
        if ("DENIED".equals(result.result())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(result.errorMessage()));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── Logs ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/log")
    public ResponseEntity<ApiResponse<Page<DeviceControlLogResponse>>> getLog(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getControlLog(id, pageable)));
    }

    @GetMapping("/my-actions")
    public ResponseEntity<ApiResponse<Page<DeviceControlLogResponse>>> myActions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getMyActions(user.getId(), pageable)));
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<ApiResponse<Map<String, String>>> heartbeat(@PathVariable Long id) {
        deviceService.updateHeartbeat(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "ok")));
    }

    // ── Visitor OTP ───────────────────────────────────────────────────────

    @PostMapping("/{id}/visitor-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> issueOtp(
            @PathVariable Long id,
            @Valid @RequestBody IssueOtpRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        int minutes = req.validMinutes() > 0 ? req.validMinutes() : 60;
        OtpResponse otp = deviceService.issueVisitorOtp(id, user.getId(), req.visitorName(), minutes);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(otp));
    }

    @PostMapping("/{id}/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(
            @PathVariable Long id,
            @Valid @RequestBody VerifyOtpRequest req) {
        boolean valid = deviceService.verifyAndUseOtp(id, req.otp());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("valid", valid)));
    }

    @DeleteMapping("/otps/{otpId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> revokeOtp(
            @PathVariable Long otpId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        deviceService.revokeOtp(otpId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "revoked")));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private void assertFmOrAdmin(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String role = user.getRole();
        if (!"FM".equals(role) && !"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            throw new SecurityException("Only FM or ADMIN may manage devices");
        }
    }
}
