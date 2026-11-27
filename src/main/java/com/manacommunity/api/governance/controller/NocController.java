package com.manacommunity.api.governance.controller;

import com.manacommunity.api.governance.entity.NocRequest;
import com.manacommunity.api.governance.repository.NocRequestRepository;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance/noc")
@RequiredArgsConstructor
public class NocController {

    private final NocRequestRepository nocRepo;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NocRequest>>> getMyRequests(@RequestParam Long residentId) {
        return ok(nocRepo.findByResidentIdOrderByCreatedAtDesc(residentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NocRequest>> submitRequest(@RequestBody NocRequest request) {
        request.setStatus("PENDING");
        request.setNocNumber(null);
        return ok(nocRepo.save(request));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<NocRequest>>> getPending(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        return ok(nocRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, "PENDING"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NocRequest>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        Long communityId = principal.getCommunityId();
        List<NocRequest> list = status != null
                ? nocRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status)
                : nocRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ok(list);
    }

    @PutMapping("/{id}/review")
    @Transactional
    public ResponseEntity<ApiResponse<NocRequest>> review(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return nocRepo.findById(id).map(noc -> {
            String decision = body.get("decision").toString(); // APPROVED or REJECTED
            noc.setStatus(decision.equals("APPROVED") ? "APPROVED" : "REJECTED");
            noc.setReviewedBy(Long.valueOf(body.get("reviewerId").toString()));
            noc.setReviewerNotes(body.containsKey("notes") ? body.get("notes").toString() : null);
            noc.setDecidedAt(OffsetDateTime.now());
            if ("APPROVED".equals(decision)) {
                long seq = nocRepo.countByCommunityIdAndStatus(noc.getCommunityId(), "APPROVED") + 1;
                noc.setNocNumber(String.format("NOC/%d/%04d", Year.now().getValue(), seq));
                noc.setValidUntil(LocalDate.now().plusMonths(6));
            }
            return ok(nocRepo.save(noc));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOC request not found")));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
