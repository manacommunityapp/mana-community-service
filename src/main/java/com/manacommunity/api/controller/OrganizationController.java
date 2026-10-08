package com.manacommunity.api.controller;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Organization;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.OrganizationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final CommunityRepository communityRepository;

    @Data
    public static class CreateOrganizationRequest {
        private String name;
        private String code;
        private String businessRegistrationNo;
        private String contactEmail;
        private String contactPhone;
        private String tier;
    }

    @GetMapping
    public ResponseEntity<List<Organization>> listOrganizations() {
        return ResponseEntity.ok(organizationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable Long id) {
        return organizationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createOrganization(@RequestBody CreateOrganizationRequest req) {
        if (req.getCode() != null && organizationRepository.existsByCodeIgnoreCase(req.getCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Organization code already exists: " + req.getCode());
        }

        Organization org = Organization.builder()
                .name(req.getName())
                .code(req.getCode() != null ? req.getCode().toUpperCase() : null)
                .businessRegistrationNo(req.getBusinessRegistrationNo())
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .tier(req.getTier() != null ? req.getTier() : "STANDARD")
                .active(true)
                .build();

        Organization saved = organizationRepository.save(org);
        log.info("Created organization [id={}, code={}]", saved.getId(), saved.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}/communities")
    public ResponseEntity<List<Community>> getOrganizationCommunities(@PathVariable Long id) {
        if (!organizationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(communityRepository.findByOrganizationId(id));
    }
}
