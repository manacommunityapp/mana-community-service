package com.manacommunity.api.security.camera;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/security/cameras")
@RequiredArgsConstructor
public class SecurityCameraController {

    private final SecurityCameraRepository cameraRepo;

    @GetMapping
    public ResponseEntity<List<SecurityCameraResponse>> getCameras(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {

        Long communityId = principal.getCommunityId();
        List<SecurityCamera> cameras = (status != null && !status.isBlank())
                ? cameraRepo.findByCommunityIdAndStatusAndIsActiveTrue(communityId, status)
                : cameraRepo.findByCommunityIdAndIsActiveTrueOrderByNameAsc(communityId);

        List<SecurityCameraResponse> response = cameras.stream()
                .map(c -> SecurityCameraResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .location(c.getLocation())
                        .type(c.getType())
                        .status(c.getStatus())
                        .streamUrl(c.getStreamUrl())
                        .snapshotUrl(c.getSnapshotUrl())
                        .lastSnapshotAt(c.getLastSnapshotAt())
                        .installDate(c.getInstallDate())
                        .ipAddress(c.getIpAddress())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/snapshot")
    public ResponseEntity<SnapshotResponse> requestSnapshot(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        SecurityCamera camera = cameraRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Camera not found"));

        if (!camera.getCommunityId().equals(principal.getCommunityId())) {
            return ResponseEntity.status(403).build();
        }

        String snapshotUrl = "https://via.placeholder.com/1280x720.png?text=Camera+"
                + (camera.getName() != null ? camera.getName().replace(" ", "+") : id);
        camera.setSnapshotUrl(snapshotUrl);
        camera.setLastSnapshotAt(LocalDateTime.now());
        cameraRepo.save(camera);

        return ResponseEntity.ok(SnapshotResponse.builder()
                .cameraId(id)
                .snapshotUrl(snapshotUrl)
                .takenAt(camera.getLastSnapshotAt())
                .build());
    }
}
