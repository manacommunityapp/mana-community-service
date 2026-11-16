package com.manacommunity.api.mobile;

import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resident directory endpoint for the mobile app.
 * GET /api/community/directory/residents
 * Returns ResidentEntry[] with flat info and resident type.
 */
@RestController
@RequestMapping("/api/community/directory/residents")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentDirectoryController {

    private final CommunityFlatRepository flatRepo;
    private final FlatMembershipRepository membershipRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getResidents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String block) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            return ResponseEntity.ok(List.of());
        }
        Long communityId = user.getCommunity().getId();

        List<CommunityFlat> flats = flatRepo.findByCommunityId(communityId);
        if (block != null && !block.isBlank()) {
            flats = flats.stream()
                    .filter(f -> block.equalsIgnoreCase(f.getTowerBlock()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (CommunityFlat flat : flats) {
            List<FlatMembership> memberships = membershipRepo.findByFlatIdWithUser(flat.getId());
            for (FlatMembership m : memberships) {
                AppUser resident = m.getUser();
                if (resident == null) continue;
                if (search != null && !search.isBlank()) {
                    String q = search.toLowerCase();
                    boolean nameMatch  = resident.getFullName() != null && resident.getFullName().toLowerCase().contains(q);
                    boolean flatMatch  = flat.getFlatNumber().toLowerCase().contains(q);
                    if (!nameMatch && !flatMatch) continue;
                }
                results.add(toEntry(resident, flat, m));
            }
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping("/flat/{flatNumber}")
    public ResponseEntity<Map<String, Object>> getByFlat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String flatNumber) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) return ResponseEntity.notFound().build();
        Long communityId = user.getCommunity().getId();

        return flatRepo.findByCommunityId(communityId).stream()
                .filter(f -> flatNumber.equalsIgnoreCase(f.getFlatNumber())
                        || flatNumber.equalsIgnoreCase(f.getTowerBlock() + "-" + f.getFlatNumber()))
                .findFirst()
                .flatMap(flat -> membershipRepo.findByFlatIdWithUser(flat.getId()).stream()
                        .filter(m -> Boolean.TRUE.equals(m.getIsPrimaryResident()))
                        .findFirst()
                        .map(m -> toEntry(m.getUser(), flat, m)))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<String>> getBlocks(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) return ResponseEntity.ok(List.of());
        Long communityId = user.getCommunity().getId();
        List<String> blocks = flatRepo.findByCommunityId(communityId).stream()
                .map(CommunityFlat::getTowerBlock)
                .filter(Objects::nonNull)
                .distinct().sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(blocks);
    }

    private Map<String, Object> toEntry(AppUser resident, CommunityFlat flat, FlatMembership membership) {
        boolean isVerified = "VERIFIED".equals(flat.getVerificationStatus());
        String residentType = membership.getResidentType() != null ? membership.getResidentType() : "OWNER";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           resident.getId());
        m.put("name",         resident.getFullName() != null ? resident.getFullName() : "");
        m.put("flatNumber",   flat.getFlatNumber());
        m.put("block",        flat.getTowerBlock());
        m.put("residentType", residentType);
        m.put("isVerified",   isVerified);
        m.put("phone",        resident.getPhone());
        return m;
    }
}
