package com.manacommunity.api.mobile;

import com.manacommunity.api.user.dto.FamilyMemberRequest;
import com.manacommunity.api.user.dto.FamilyMemberResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.FamilyMemberService;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mobile-facing family members API at /api/family/members.
 * Maps FamilyMemberResponse to the shape the mobile app expects:
 * isMinor, isEmergencyContact, addedAt.
 */
@RestController
@RequestMapping("/api/family/members")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MobileFamilyController {

    private final FamilyMemberService familyMemberService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyFamily(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<Map<String, Object>> members = familyMemberService
                .getFamilyMembersResponse(user, communityId)
                .stream().map(this::toMobileResponse).collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MobileFamilyRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal != null ? principal.getCommunityId() : null;
        FamilyMemberRequest javaReq = toJavaRequest(req);
        FamilyMemberResponse created = familyMemberService.addFamilyMember(javaReq, user, communityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMobileResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MobileFamilyRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal != null ? principal.getCommunityId() : null;
        FamilyMemberRequest javaReq = toJavaRequest(req);
        FamilyMemberResponse updated = familyMemberService.updateFamilyMember(id, javaReq, user, communityId);
        return ResponseEntity.ok(toMobileResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal != null ? principal.getCommunityId() : null;
        familyMemberService.deleteFamilyMember(id, user, communityId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMobileResponse(FamilyMemberResponse r) {
        boolean isMinor = r.getAge() != null && r.getAge() < 18;
        boolean isEmergencyContact = Boolean.TRUE.equals(r.getEmergencyContact());
        String addedAt = r.getCreatedAt() != null ? r.getCreatedAt().toString() : java.time.LocalDateTime.now().toString();
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id",                r.getId());
        map.put("name",              r.getName());
        map.put("relation",          r.getRelation() != null ? r.getRelation() : "OTHER");
        map.put("phone",             r.getPhone());
        map.put("dateOfBirth",       r.getDob());
        map.put("isMinor",           isMinor);
        map.put("isEmergencyContact",isEmergencyContact);
        map.put("addedAt",           addedAt);
        return map;
    }

    private FamilyMemberRequest toJavaRequest(MobileFamilyRequest req) {
        FamilyMemberRequest r = new FamilyMemberRequest();
        r.setName(req.name());
        r.setRelation(req.relation());
        r.setPhone(req.phone());
        r.setDob(req.dateOfBirth());
        if (req.isMinor() != null && req.isMinor()) {
            r.setAge(10); // placeholder minor age; full dob handling would be ideal
        }
        r.setEmergencyContact(req.isEmergencyContact());
        return r;
    }

    public record MobileFamilyRequest(
            String name,
            String relation,
            String phone,
            String dateOfBirth,
            Boolean isMinor,
            Boolean isEmergencyContact
    ) {}
}
