package com.manacommunity.api.communityops.pass;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/digital-pass")
@RequiredArgsConstructor
public class DigitalPassController {

    private final DigitalPassService service;

    @PostMapping
    public ResponseEntity<DigitalPass> generate(@RequestBody DigitalPassRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.generate(principal.getCommunityId(), principal.getUserId(), req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<DigitalPass>> myPasses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getMyActivePasses(principal.getUserId()));
    }

    @GetMapping("/flat/{flatNumber}")
    public ResponseEntity<List<DigitalPass>> forFlat(@PathVariable String flatNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getActivePassesForFlat(principal.getCommunityId(), flatNumber));
    }

    @PostMapping("/scan")
    public ResponseEntity<DigitalPass> scan(@RequestBody ScanRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.scan(req.token(), principal.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.revoke(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    record ScanRequest(String token) {}
}
