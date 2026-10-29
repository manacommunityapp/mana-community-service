package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.PartnerCandidateResponse;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Provides a community-scoped unified partner-search endpoint for the doubles
 * partner selector in the sports registration flow.
 *
 * <p>GET /api/sports/partner-search returns a ranked list of {@link PartnerCandidateResponse}
 * objects that unions:
 * <ol>
 *   <li>AppUser records (flat owners, tenants, staff) in the community.</li>
 *   <li>FamilyMember records registered in the community.</li>
 * </ol>
 *
 * <p>Each candidate includes a pre-computed {@code eligible} flag and an optional
 * {@code ineligibilityReason} derived from the target event's age/gender constraints,
 * so the frontend can disable ineligible entries without additional round-trips.
 *
 * <p>Privacy (DPDP): phone and email are never returned — only name, flat, gender, and age.
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsPartnerSearchController {

    private final AppUserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final SportsEventRepository eventRepository;
    private final LoggedInUserService loggedInUserService;

    /**
     * Search for eligible doubles partners within a community.
     *
     * @param principal  the authenticated caller
     * @param communityId the community to scope the search to
     * @param query       name / flat search string (min 1 char)
     * @param eventId     optional — if provided, eligibility is computed against this event's rules
     * @param matchType   optional — DOUBLES | MIXED_DOUBLES; affects gender gating
     * @param callerGender optional — the registering player's gender; used for MIXED_DOUBLES pairing
     */
    @GetMapping("/partner-search")
    public ResponseEntity<List<PartnerCandidateResponse>> searchPartners(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false, defaultValue = "DOUBLES") String matchType,
            @RequestParam(required = false) String callerGender) {

        // ── Resolve the target event (optional) ────────────────────────────
        SportsEvent event = (eventId != null)
                ? eventRepository.findById(eventId).orElse(null)
                : null;

        Integer minAge = event != null ? event.getMinAge() : null;
        Integer maxAge = event != null ? event.getMaxAge() : null;
        boolean allowHigherAge = event == null || Boolean.TRUE.equals(event.getAllowHigherAgeCategory());
        String eventGender = event != null ? event.getGender() : null;

        // ── Derive expected partner gender for MIXED_DOUBLES ───────────────
        boolean isMixed = "MIXED_DOUBLES".equalsIgnoreCase(matchType);
        String requiredGender = null;
        if (isMixed && callerGender != null) {
            String g = callerGender.trim().toUpperCase();
            if ("MALE".equals(g)) requiredGender = "FEMALE";
            else if ("FEMALE".equals(g)) requiredGender = "MALE";
        } else if (!isMixed && eventGender != null) {
            // For straight doubles in a gendered event, partner must match event gender
            requiredGender = eventGender.trim().toUpperCase();
        }

        List<PartnerCandidateResponse> results = new ArrayList<>();

        // ── 1. AppUser search ───────────────────────────────────────────────
        String trimmedQuery = query.trim();
        if (!trimmedQuery.isEmpty()) {
            List<AppUser> users = userRepository.searchCommunityMembers(communityId, trimmedQuery);
            for (AppUser u : users) {
                if (!Boolean.TRUE.equals(u.getIsActive())) continue;

                Integer age = computeUserAge(u);
                String gender = normalise(u.getGender());

                EligibilityResult elig = checkEligibility(age, gender, minAge, maxAge, allowHigherAge, requiredGender, isMixed);

                String flatLabel = buildFlatLabel(u.getBlock(), u.getFlatNo());
                String occupancyType = u.getOccupancyStatus() != null ? u.getOccupancyStatus() : "Resident";
                String occupancyLabel = flatLabel.isEmpty()
                        ? occupancyType
                        : occupancyType + " · " + flatLabel;

                results.add(PartnerCandidateResponse.builder()
                        .type("USER")
                        .id(u.getId())
                        .primaryUserId(null)
                        .name(u.getFullName())
                        .relation(null)
                        .occupancyLabel(occupancyLabel)
                        .flatNo(u.getFlatNo())
                        .block(u.getBlock())
                        .gender(gender)
                        .age(age)
                        .eligible(elig.eligible)
                        .ineligibilityReason(elig.reason)
                        .build());
            }
        }

        // ── 2. FamilyMember search ─────────────────────────────────────────
        if (!trimmedQuery.isEmpty()) {
            List<FamilyMember> familyMembers = familyMemberRepository.searchByNameInCommunity(communityId, trimmedQuery);
            for (FamilyMember fm : familyMembers) {
                Integer age = computeFamilyMemberAge(fm);
                String gender = normalise(fm.getGender());

                EligibilityResult elig = checkEligibility(age, gender, minAge, maxAge, allowHigherAge, requiredGender, isMixed);

                // Use primary user's flat info
                String flatLabel = "";
                String primaryOccupancy = "Family";
                if (fm.getUser() != null) {
                    flatLabel = buildFlatLabel(fm.getUser().getBlock(), fm.getUser().getFlatNo());
                    String ownerOccupancy = fm.getUser().getOccupancyStatus();
                    if (ownerOccupancy != null) primaryOccupancy = ownerOccupancy;
                }

                String relationLabel = fm.getRelation() != null ? fm.getRelation() : "Family";
                String occupancyLabel = flatLabel.isEmpty()
                        ? "Family (" + relationLabel + ")"
                        : "Family (" + relationLabel + ") · " + flatLabel;

                results.add(PartnerCandidateResponse.builder()
                        .type("FAMILY_MEMBER")
                        .id(fm.getId())
                        .primaryUserId(fm.getUser() != null ? fm.getUser().getId() : null)
                        .name(fm.getName())
                        .relation(fm.getRelation())
                        .occupancyLabel(occupancyLabel)
                        .flatNo(fm.getUser() != null ? fm.getUser().getFlatNo() : null)
                        .block(fm.getUser() != null ? fm.getUser().getBlock() : null)
                        .gender(gender)
                        .age(age)
                        .eligible(elig.eligible)
                        .ineligibilityReason(elig.reason)
                        .build());
            }
        }

        // ── Sort: eligible first, then alphabetically ──────────────────────
        results.sort(Comparator
                .comparing((PartnerCandidateResponse r) -> r.isEligible() ? 0 : 1)
                .thenComparing(r -> r.getName() != null ? r.getName().toLowerCase() : ""));

        return ResponseEntity.ok(results);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String normalise(String gender) {
        if (gender == null) return null;
        String g = gender.trim().toUpperCase();
        return switch (g) {
            case "M", "MEN", "MAN", "BOY", "BOYS", "MALE" -> "MALE";
            case "F", "WOMEN", "WOMAN", "GIRL", "GIRLS", "FEMALE", "LADIES" -> "FEMALE";
            default -> g;
        };
    }

    private static Integer computeUserAge(AppUser u) {
        if (u.getDateOfBirth() == null) return null;
        LocalDate dob = u.getDateOfBirth();
        LocalDate today = LocalDate.now();
        int age = today.getYear() - dob.getYear();
        if (today.getMonthValue() < dob.getMonthValue() ||
                (today.getMonthValue() == dob.getMonthValue() && today.getDayOfMonth() < dob.getDayOfMonth())) {
            age--;
        }
        return age >= 0 ? age : null;
    }

    private static Integer computeFamilyMemberAge(FamilyMember fm) {
        // Prefer dob string for precise calculation
        if (fm.getDob() != null && !fm.getDob().isBlank()) {
            LocalDate dob = parseDob(fm.getDob());
            if (dob != null) {
                LocalDate today = LocalDate.now();
                int age = today.getYear() - dob.getYear();
                if (today.getMonthValue() < dob.getMonthValue() ||
                        (today.getMonthValue() == dob.getMonthValue() && today.getDayOfMonth() < dob.getDayOfMonth())) {
                    age--;
                }
                return age >= 0 ? age : null;
            }
        }
        // Fallback to stored age integer
        return fm.getAge();
    }

    /** Attempts to parse a dob string in yyyy-MM-dd, dd-MM-yyyy, dd/MM/yyyy formats. */
    private static LocalDate parseDob(String dob) {
        if (dob == null) return null;
        String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "d-M-yyyy", "d/M/yyyy"};
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dob.trim(), DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static String buildFlatLabel(String block, String flatNo) {
        if (block != null && !block.isBlank() && flatNo != null && !flatNo.isBlank()) {
            return "Flat " + block + "-" + flatNo;
        } else if (flatNo != null && !flatNo.isBlank()) {
            return "Flat " + flatNo;
        } else if (block != null && !block.isBlank()) {
            return "Block " + block;
        }
        return "";
    }

    private static EligibilityResult checkEligibility(
            Integer candidateAge,
            String candidateGender,
            Integer minAge,
            Integer maxAge,
            boolean allowHigherAge,
            String requiredGender,
            boolean isMixed) {

        // ── Age check ──────────────────────────────────────────────────────
        if (candidateAge != null) {
            if (minAge != null && candidateAge < minAge) {
                return new EligibilityResult(false,
                        "Age " + candidateAge + " — min " + minAge);
            }
            if (!allowHigherAge && maxAge != null && candidateAge > maxAge) {
                return new EligibilityResult(false,
                        "Age " + candidateAge + " — max " + maxAge);
            }
        }

        // ── Gender check ──────────────────────────────────────────────────
        if (requiredGender != null && candidateGender != null
                && !requiredGender.equalsIgnoreCase(candidateGender)) {
            String label = isMixed ? "Mixed Doubles needs " + requiredGender.toLowerCase()
                    : "Gender must be " + requiredGender.toLowerCase();
            return new EligibilityResult(false, label);
        }

        return new EligibilityResult(true, null);
    }

    /** Lightweight value object for eligibility check results. */
    private record EligibilityResult(boolean eligible, String reason) {}
}
