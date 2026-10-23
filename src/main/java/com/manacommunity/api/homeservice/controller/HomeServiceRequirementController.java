package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceBidRequest;
import com.manacommunity.api.homeservice.dto.HomeServiceRequirementRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceRequirementEntity;
import com.manacommunity.api.homeservice.model.entity.RequirementWorkerResponseEntity;
import com.manacommunity.api.homeservice.service.HomeServiceRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController("homeServiceRequirementController")
@RequestMapping("/api/v1/home-services/requirements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceRequirementController {
    private final HomeServiceRequirementService requirementService;

    @PostMapping
    public ResponseEntity<HomeServiceRequirementEntity> createRequirement(@RequestBody HomeServiceRequirementRequest req) {
        return ResponseEntity.ok(requirementService.createRequirement(req));
    }

    @GetMapping
    public ResponseEntity<List<HomeServiceRequirementEntity>> getRequirements(@RequestParam(defaultValue = "comm-mana-1") String communityId) {
        return ResponseEntity.ok(requirementService.getCommunityRequirements(communityId));
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<RequirementWorkerResponseEntity> submitBid(@PathVariable String id, @RequestBody HomeServiceBidRequest req) {
        req.setRequestId(id);
        return ResponseEntity.ok(requirementService.submitBid(req));
    }

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<RequirementWorkerResponseEntity>> getBids(@PathVariable String id) {
        return ResponseEntity.ok(requirementService.getBidsForRequirement(id));
    }
}
