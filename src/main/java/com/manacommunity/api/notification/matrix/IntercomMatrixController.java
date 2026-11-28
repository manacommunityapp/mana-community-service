package com.manacommunity.api.notification.matrix;

import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService;
import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService.BlackholeDiagnosticReport;
import com.manacommunity.api.notification.matrix.IntercomMatrixDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/matrix")
@RequiredArgsConstructor
public class IntercomMatrixController {

    private final IntercomMatrixService matrixService;
    private final PushNotificationDiagnosticsService diagnosticsService;

    @PostMapping("/oem-wakeup")
    public ResponseEntity<OemWakeupEvaluationResult> evaluateOemWakeup(
            @RequestBody OemWakeupEvaluationRequest request) {
        return ResponseEntity.ok(matrixService.evaluateOemWakeup(request));
    }

    @PostMapping("/cascade-probe")
    public ResponseEntity<ChannelMatrixProbeResult> executeCascadeProbe(
            @RequestBody ChannelMatrixProbeRequest request) {
        return ResponseEntity.ok(matrixService.executeChannelCascadeProbe(request));
    }

    @PostMapping("/family-race")
    public ResponseEntity<FamilyResolutionRaceResult> executeFamilyRace(
            @RequestBody FamilyResolutionRaceRequest request) {
        return ResponseEntity.ok(matrixService.executeConcurrentFamilyRace(request));
    }

    @GetMapping("/health")
    public ResponseEntity<BlackholeDiagnosticReport> getPipelineHealth() {
        return ResponseEntity.ok(diagnosticsService.evaluatePipelineHealth());
    }
}
