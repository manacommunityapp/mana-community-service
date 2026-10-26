package com.manacommunity.api.cctv.service;

import com.manacommunity.api.cctv.entity.Camera;
import com.manacommunity.api.cctv.repository.CameraRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CameraService {

    private final CameraRepository cameraRepository;

    public List<Camera> getCameras(AppUser user) {
        return cameraRepository.findByCommunityId(user.getCommunity().getId());
    }

    public Map<String, Object> captureSnapshot(Long cameraId, AppUser user) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new RuntimeException("Camera not found"));
        Map<String, Object> result = new HashMap<>();
        result.put("cameraId", cameraId);
        result.put("cameraName", camera.getName());
        result.put("snapshotUrl", camera.getSnapshotUrl());
        result.put("capturedAt", Instant.now().toString());
        return result;
    }
}
