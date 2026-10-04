package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.dto.BiometricDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/access/turnstiles")
@RequiredArgsConstructor
public class BiometricTurnstileController {

    private final BiometricTurnstileService turnstileService;

    @PostMapping("/verify-face")
    public ResponseEntity<VerifyFaceResult> verifyFace(@Valid @RequestBody VerifyFaceRequest request) {
        return ResponseEntity.ok(turnstileService.verifyFace(request));
    }

    @PostMapping("/enroll")
    public ResponseEntity<Void> enrollUser(@Valid @RequestBody EnrollBiometricRequest request) {
        turnstileService.enrollUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<List<BiometricTurnstileDto>> getTurnstiles(@PathVariable Long communityId) {
        return ResponseEntity.ok(turnstileService.getTurnstilesForCommunity(communityId));
    }

    @GetMapping("/logs/{communityId}")
    public ResponseEntity<Page<BiometricAccessLogDto>> getAccessLogs(
            @PathVariable Long communityId,
            Pageable pageable) {
        return ResponseEntity.ok(turnstileService.getAccessLogs(communityId, pageable));
    }

    @PutMapping("/{turnstileId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long turnstileId,
            @RequestParam BiometricEnums.TurnstileStatus status) {
        turnstileService.setTurnstileStatus(turnstileId, status);
        return ResponseEntity.ok().build();
    }
}
