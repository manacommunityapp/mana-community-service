package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.MealRegistrationResponse;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.service.EventBookingRegistrationService;
import com.manacommunity.api.events.service.EventMealService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/registrations")
public class EventBookingRegistrationController {

    private final EventBookingRegistrationService service;
    private final EventMealService mealService;
    private final LoggedInUserService loggedInUserService;
    private final AppUserRepository userRepository;
    private final com.manacommunity.api.events.repository.EventBookingRegistrationRepository bookingRepository;

    public EventBookingRegistrationController(EventBookingRegistrationService service,
                                              EventMealService mealService,
                                              LoggedInUserService loggedInUserService,
                                              AppUserRepository userRepository,
                                              com.manacommunity.api.events.repository.EventBookingRegistrationRepository bookingRepository) {
        this.service = service;
        this.mealService = mealService;
        this.loggedInUserService = loggedInUserService;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping
    public ResponseEntity<?> createRegistration(
            @RequestBody EventBookingRegistration registration,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "adminOverride", required = false, defaultValue = "false") boolean adminOverride,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId) {
        String actId = registration.getActivityId();
        if (actId != null && actId.startsWith("pooja-")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if ("Pooja".equalsIgnoreCase(registration.getCategory())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // Cultural registrations are handled by CulturalRegistrationController
        if (actId != null && (actId.startsWith("cultural-") || actId.startsWith("cult-"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Cultural registrations must use POST /api/events/cultural/registrations");
        }

        AppUser caller = loggedInUserService.resolve(principal);
        boolean isAdmin = caller != null && (
                caller.hasRole("ADMIN") || caller.hasRole("SUPER_ADMIN") ||
                caller.hasRole("COMMUNITY_ADMIN") || caller.hasRole("EVENT_ADMIN") ||
                caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_SUPER_ADMIN") ||
                caller.hasRole("ROLE_COMMUNITY_ADMIN") || caller.hasRole("ROLE_EVENT_ADMIN"));

        Long effectiveTargetId = targetUserId;
        if (effectiveTargetId == null && registration.getUser() != null && registration.getUser().getId() != null) {
            effectiveTargetId = registration.getUser().getId();
        }

        AppUser effectiveUser = caller;
        if (isAdmin && effectiveTargetId != null && caller != null && !effectiveTargetId.equals(caller.getId())) {
            effectiveUser = userRepository.findById(effectiveTargetId).orElse(caller);
        }

        // Meal activities → save to event_meal_registrations AND event_booking_registrations
        boolean isMeal = (actId != null && (actId.startsWith("meal-") || actId.startsWith("food-")))
                || "LUNCH_DINNER".equalsIgnoreCase(registration.getActivityType())
                || "Meal".equalsIgnoreCase(registration.getCategory());
        if (isMeal) {
            Long lunchDinnerId = parseMealId(actId, registration);
            if (lunchDinnerId == null) return ResponseEntity.badRequest().body("Missing lunchDinnerId");
            int headCount = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                    ? registration.getDevoteeCount() : 1;
            MealRegistrationResponse result = mealService.registerSingleMeal(
                    lunchDinnerId, headCount, null, effectiveUser);

            if (registration.getCategory() == null || registration.getCategory().isBlank()) {
                registration.setCategory("Food");
            }
            if (registration.getActivityType() == null || registration.getActivityType().isBlank()) {
                registration.setActivityType("LUNCH_DINNER");
            }
            if (registration.getActivityId() == null || registration.getActivityId().isBlank()) {
                registration.setActivityId("meal-" + lunchDinnerId);
            }
            if (registration.getPassType() == null || registration.getPassType().isBlank()) {
                registration.setPassType("Meal Registration Pass");
            }

            EventBookingRegistration created = null;
            if (effectiveUser != null && effectiveUser.getId() != null) {
                List<EventBookingRegistration> existing = bookingRepository.findByUserIdOrderByCreatedAtDesc(effectiveUser.getId());
                EventBookingRegistration match = existing.stream()
                        .filter(r -> ("meal-" + lunchDinnerId).equalsIgnoreCase(r.getActivityId()) || ("food-" + lunchDinnerId).equalsIgnoreCase(r.getActivityId()))
                        .findFirst().orElse(null);
                if (match != null) {
                    match.setDevoteeCount(headCount);
                    if (registration.getParticipantName() != null && !registration.getParticipantName().isBlank()) {
                        match.setParticipantName(registration.getParticipantName());
                    }
                    match.setStatus("CONFIRMED");
                    match.setUpdatedAt(java.time.LocalDateTime.now());
                    created = bookingRepository.save(match);
                } else {
                    created = service.createRegistration(registration, effectiveUser, communityId, true);
                }
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(created != null ? created : result);
        }

        EventBookingRegistration created = service.createRegistration(registration, effectiveUser, communityId, adminOverride && isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Update only the headCount for an existing meal slot registration. */
    @PatchMapping("/meal/{lunchDinnerId}/headcount")
    public ResponseEntity<MealRegistrationResponse> updateMealHeadCount(
            @PathVariable Long lunchDinnerId,
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Object countObj = body.get("headCount");
        Integer headCount = countObj instanceof Number ? ((Number) countObj).intValue() : null;
        if (headCount == null || headCount < 1) return ResponseEntity.badRequest().build();
        AppUser caller = loggedInUserService.resolve(principal);
        boolean isAdmin = caller != null && (
                caller.hasRole("ADMIN") || caller.hasRole("SUPER_ADMIN") ||
                caller.hasRole("COMMUNITY_ADMIN") || caller.hasRole("EVENT_ADMIN") ||
                caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_SUPER_ADMIN") ||
                caller.hasRole("ROLE_COMMUNITY_ADMIN") || caller.hasRole("ROLE_EVENT_ADMIN"));

        Long effectiveTargetId = targetUserId;
        if (effectiveTargetId == null && body.get("userId") != null) {
            try { effectiveTargetId = Long.parseLong(body.get("userId").toString()); } catch (Exception ignored) {}
        }
        AppUser effectiveUser = caller;
        if (isAdmin && effectiveTargetId != null && caller != null && !effectiveTargetId.equals(caller.getId())) {
            effectiveUser = userRepository.findById(effectiveTargetId).orElse(caller);
        }
        return ResponseEntity.ok(mealService.updateMealHeadCount(lunchDinnerId, headCount, effectiveUser));
    }

    private Long parseMealId(String actId, EventBookingRegistration reg) {
        if (actId != null) {
            if (actId.startsWith("meal-")) {
                try { return Long.parseLong(actId.replace("meal-", "")); } catch (NumberFormatException ignored) {}
            }
            if (actId.startsWith("food-")) {
                try { return Long.parseLong(actId.replace("food-", "")); } catch (NumberFormatException ignored) {}
            }
        }
        // Fallback: lunchDinnerId / mealId sent as transient fields in the payload
        if (reg.getLunchDinnerId() != null) return reg.getLunchDinnerId();
        return null;
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventBookingRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "status", required = false) String status) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getMyRegistrations(user, communityId, status));
    }

    @GetMapping
    public ResponseEntity<List<EventBookingRegistration>> getAllRegistrations(
            @RequestHeader(value = "X-Community-Id", required = false) Long communityId,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(service.getRegistrationsByCommunity(communityId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventBookingRegistration> getRegistrationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getRegistrationById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventBookingRegistration> updateRegistration(
            @PathVariable Long id,
            @RequestBody EventBookingRegistration patch,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        EventBookingRegistration updated = service.updateRegistration(id, patch, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        service.cancelRegistration(id, reason, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrDeleteRegistration(
            @PathVariable Long id,
            @RequestParam(value = "permanent", required = false, defaultValue = "false") boolean permanent,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (permanent) {
            service.deleteRegistration(id, user);
        } else {
            service.cancelRegistration(id, reason, user);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        service.deleteRegistration(id, user);
        return ResponseEntity.noContent().build();
    }
}
