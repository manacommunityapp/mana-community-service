package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.dto.BiometricDtos.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api/v1/access/turnstiles")
@RequiredArgsConstructor
public class BiometricAccessAliasController {

    private final BiometricTurnstileService turnstileService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<BiometricTurnstileDto>> listTurnstiles(
            @RequestParam(required = false) Long societyId,
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = societyId != null ? societyId : communityId;
        if (cid == null) {
            AppUser user = loggedInUserService.resolve(principal);
            cid = user.getCommunity().getId();
        }
        return ResponseEntity.ok(turnstileService.getTurnstilesForCommunity(cid));
    }

    @PostMapping("/verify-face")
    public ResponseEntity<VerifyFaceResult> verifyFace(@Valid @RequestBody VerifyFaceRequest request) {
        return ResponseEntity.ok(turnstileService.verifyFace(request));
    }

    @PostMapping("/{id}/relay-unlock")
    public ResponseEntity<?> relayUnlock(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        List<BiometricTurnstileDto> turnstiles = turnstileService.getTurnstilesForCommunity(communityId);
        BiometricTurnstileDto target = turnstiles.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("turnstileId", id);
        if (target != null) {
            result.put("turnstileName", target.getTurnstileName());
            result.put("relayUnlockMs", target.getRelayUnlockMs());
        }
        result.put("status", "unlocked");
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<BiometricAccessLogDto>> getAccessLogs(
            @RequestParam(required = false) Long societyId,
            @RequestParam(required = false) Long communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = societyId != null ? societyId : communityId;
        if (cid == null) {
            AppUser user = loggedInUserService.resolve(principal);
            cid = user.getCommunity().getId();
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(turnstileService.getAccessLogs(cid, pageable));
    }
}
