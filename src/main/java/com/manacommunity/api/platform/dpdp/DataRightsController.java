package com.manacommunity.api.platform.dpdp;

import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-rights")
@RequiredArgsConstructor
public class DataRightsController {

    private final DataRightsRequestRepository repo;

    // Resident submits a data rights request
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<DataRightsRequest>> submitRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        // body.get("requestType") = ERASURE | CORRECTION | PORTABILITY | RESTRICTION
        // body.get("reason") = optional free text
        DataRightsRequest req = new DataRightsRequest();
        req.setResidentId(principal.getId());
        req.setCommunityId(principal.getCommunityId());
        req.setRequestType(body.get("requestType"));
        req.setReason(body.get("reason"));
        req.setStatus("PENDING");
        req.setRequestedAt(Instant.now());
        req.setCreatedAt(Instant.now());
        return ResponseEntity.ok(ApiResponse.ok(repo.save(req)));
    }

    // Resident views their own requests
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<DataRightsRequest>>> myRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(repo.findByResidentId(principal.getId())));
    }

    // Admin reviews requests for their community
    @GetMapping("/community/{communityId}")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Manage Privacy')")
    public ResponseEntity<ApiResponse<List<DataRightsRequest>>> communityRequests(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(ApiResponse.ok(repo.findByCommunityIdAndStatus(communityId, status)));
    }

    // Admin resolves a request
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Manage Privacy')")
    public ResponseEntity<ApiResponse<DataRightsRequest>> resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        return repo.findById(id).map(req -> {
            req.setStatus(body.getOrDefault("status", "COMPLETED"));
            req.setResolutionNotes(body.get("notes"));
            req.setResolvedAt(Instant.now());
            req.setResolvedBy(principal.getId());
            return ResponseEntity.<ApiResponse<DataRightsRequest>>ok(ApiResponse.ok(repo.save(req)));
        }).orElse(ResponseEntity.notFound().<ApiResponse<DataRightsRequest>>build());
    }
}
