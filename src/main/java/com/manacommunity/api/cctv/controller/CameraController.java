package com.manacommunity.api.cctv.controller;

import com.manacommunity.api.cctv.entity.Camera;
import com.manacommunity.api.cctv.service.CameraService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cctv/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Camera>> getCameras(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cameraService.getCameras(loggedInUserService.resolve(principal)));
    }

    @PostMapping("/{id}/snapshot")
    public ResponseEntity<Map<String, Object>> captureSnapshot(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cameraService.captureSnapshot(id, loggedInUserService.resolve(principal)));
    }
}
