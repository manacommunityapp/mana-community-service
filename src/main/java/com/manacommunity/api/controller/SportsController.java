package com.manacommunity.api.controller;

import com.manacommunity.api.dto.PlayerCategoryRequest;
import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.dto.SportsEventRequest;
import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.PlayerCategoryRepository;
import com.manacommunity.api.repository.SportMetaRepository;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.SportsEventService;
import com.manacommunity.api.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsEventCsvImportService;
import org.springframework.web.multipart.MultipartFile;
import static com.manacommunity.api.constants.PermissionConstants.*;

/**
 * BUG FIXES applied:
 *
 * 1. SportsController injected SportMetaRepository directly — this is
 *    acceptable for a simple read endpoint. Kept but added proper import.
 *
 * 2. categoryRepo was used but NEVER declared as a field — added
 *    PlayerCategoryRepository categoryRepo field.
 *
 * 3. All model/DTO types were used without imports — added all imports.
 *
 * 4. UserPrincipal was not imported — changed to app-level class.
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsController {

    private final SportsEventService eventService;
    private final SportMetaRepository sportMetaRepo;
    private final PlayerCategoryRepository categoryRepo; // BUG FIX: was missing
    private final LoggedInUserService loggedInUserService;
    private final TournamentService tournamentService;
    private final PermissionCheckService permissionCheckService;
    private final SportsEventCsvImportService csvImportService;

    @GetMapping("/meta")
    public ResponseEntity<List<SportsMeta>> getAllSports(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(sportMetaRepo.findByActiveTrue());
    }

    @PostMapping("/meta")
    public ResponseEntity<SportsMeta> createSport(
            @RequestBody SportsMeta sport,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        if (sport.getActive() == null) {
            sport.setActive(true);
        }
        if (principal != null) {
            AppUser loggedInUser = loggedInUserService.resolve(principal);
            if (loggedInUser != null) {
                if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
                    if (loggedInUser.getCommunity() != null) {
                        sport.setCommunityId(loggedInUser.getCommunity().getId());
                    }
                }
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(sportMetaRepo.save(sport));
    }

    @PutMapping("/meta/{id}")
    public ResponseEntity<SportsMeta> updateSport(
            @PathVariable Long id, 
            @RequestBody SportsMeta req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return sportMetaRepo.findById(id).map(sport -> {
            if (sport.getCommunityId() == null) {
                if (principal == null) {
                    throw new UnauthorizedActionException("You do not have permission to modify this sport.");
                }
                AppUser loggedInUser = loggedInUserService.resolve(principal);
                if (loggedInUser == null || !"SUPER_ADMIN".equals(loggedInUser.getRole())) {
                    throw new UnauthorizedActionException("You do not have permission to modify this sport.");
                }
            } else {
                if (principal == null) {
                    throw new UnauthorizedActionException("You do not have permission to modify this sport.");
                }
                AppUser loggedInUser = loggedInUserService.resolve(principal);
                if (loggedInUser == null) {
                    throw new UnauthorizedActionException("You do not have permission to modify this sport.");
                }
                if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
                    Long userCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
                    if (userCommunityId == null || !userCommunityId.equals(sport.getCommunityId())) {
                        throw new UnauthorizedActionException("You do not have permission to modify this sport.");
                    }
                }
            }

            sport.setName(req.getName());
            sport.setIcon(req.getIcon());
            sport.setIconUrl(req.getIconUrl());
            sport.setFormats(req.getFormats());
            if (req.getActive() != null) {
                sport.setActive(req.getActive());
            }
            return ResponseEntity.ok(sportMetaRepo.save(sport));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/meta/{id}")
    public ResponseEntity<Void> deleteSport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN);
        return sportMetaRepo.findById(id).map(sport -> {
            if (sport.getCommunityId() == null) {
                if (principal == null) {
                    throw new UnauthorizedActionException("You do not have permission to delete this sport.");
                }
                AppUser loggedInUser = loggedInUserService.resolve(principal);
                if (loggedInUser == null || !"SUPER_ADMIN".equals(loggedInUser.getRole())) {
                    throw new UnauthorizedActionException("You do not have permission to delete this sport.");
                }
            } else {
                if (principal == null) {
                    throw new UnauthorizedActionException("You do not have permission to delete this sport.");
                }
                AppUser loggedInUser = loggedInUserService.resolve(principal);
                if (loggedInUser == null) {
                    throw new UnauthorizedActionException("You do not have permission to delete this sport.");
                }
                if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
                    Long userCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
                    if (userCommunityId == null || !userCommunityId.equals(sport.getCommunityId())) {
                        throw new UnauthorizedActionException("You do not have permission to delete this sport.");
                    }
                }
            }

            sport.setActive(false); // soft-delete by deactivating
            sportMetaRepo.save(sport);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<PlayerCategory>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        if ("SUPER_ADMIN".equals(loggedInUser.getRole())) {
            return ResponseEntity.ok(categoryRepo.findAll());
        }
        // For all other roles: show DEFAULT categories + their community's categories
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        if (communityId != null) {
            return ResponseEntity.ok(categoryRepo.findDefaultAndCommunityCategories(communityId));
        }
        return ResponseEntity.ok(categoryRepo.findAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<PlayerCategory> createCategory(
            @Valid @RequestBody PlayerCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);

        AppUser loggedInUser = loggedInUserService.resolve(principal);
        String userRole = loggedInUser.getRole(); // e.g., "SUPER_ADMIN", "ADMIN", "SPORTS_ADMIN", "VENDOR"
        String typeValue;
        switch (userRole) {
            case "SUPER_ADMIN":
                typeValue = "DEFAULT";
                break;
            case "ADMIN", "SPORTS_ADMIN":
                typeValue = "USER";
                break;
            case "VENDOR":
                typeValue = "VENDOR";
                break;
            default:
                throw new IllegalArgumentException("Unknown user role: " + userRole);
        }
        req.setType(typeValue);

        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createCategory(req));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<PlayerCategory> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody com.manacommunity.api.dto.PlayerCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.updateCategory(id, req));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        eventService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/events")
    public ResponseEntity<SportsEvent> createSportsEvent(
            @Valid @RequestBody SportsEventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            req.setCommunityId(loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null);
        }

        SportsEvent created = eventService.createEvent(req, loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/events/{id}/status")
    public ResponseEntity<SportsEvent> updateStatus(
            @PathVariable Long id, @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.updateStatus(id, status));
    }

    @PutMapping("/tournaments/{id}")
    public ResponseEntity<SportsEvent> updateTournament(
            @PathVariable Long id, @Valid @RequestBody TournamentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        Tournament tournament = tournamentService.saveTournamentRecord(req, req.getAllowAdminChat());

        // Find and return the updated SportsEvent for backward-compatibility with UI
        SportsEvent updated = eventService.getEventById(id);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<SportsEvent> updateSportsEvent(
            @PathVariable Long id, @Valid @RequestBody SportsEventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            req.setCommunityId(loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null);
        }

        return ResponseEntity.ok(eventService.updateEvent(id, req));
    }


    @DeleteMapping({"/events/{id}", "/tournaments/{id}"})
    public ResponseEntity<Void> deleteTournament(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/events/{id}", "/tournaments/{id}"})
    public ResponseEntity<SportsEvent> getTournamentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * Public lookup of an event by its shareable UUID — backs the registration
     * link {@code /sports/register/{uuid}} so the form can render event details
     * (name, dates, age limits, whether admin approval is required) without
     * exposing the sequential numeric id. Permitted unauthenticated in
     * {@code SecurityConfig}; the registration POST still enforces verification.
     */
    @GetMapping("/events/by-uuid/{uuid}")
    public ResponseEntity<SportsEvent> getEventByUuid(@PathVariable java.util.UUID uuid) {
        return ResponseEntity.ok(eventService.getEventByUuid(uuid));
    }

    @GetMapping({"/events/{eventId}/confirmed-count", "/tournaments/{eventId}/confirmed-count"})
    public ResponseEntity<Long> getConfirmedCount(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        return ResponseEntity.ok(eventService.getConfirmedRegistrationCount(eventId));
    }

    @PutMapping({"/events/{id}/committee", "/tournaments/{id}/committee"})
    public ResponseEntity<SportsEvent> updateCommittee(
            @PathVariable Long id,
            @RequestBody List<Long> committeeIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        SportsEvent event = eventService.getEventById(id);
        String idsString = committeeIds == null ? "" : 
            committeeIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        event.setDisputeCommitteeIds(idsString);
        return ResponseEntity.ok(eventService.saveEvent(event));
    }

    @GetMapping({"/event-list-map", "/tournament-list-map"})
    public ResponseEntity<List<Map<String, Object>>> getTournamentMap(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        return ResponseEntity.ok(eventService.getEventMap(targetCommunityId));
    }

    @GetMapping({"/events/open", "/tournaments/open"})
    public ResponseEntity<List<SportsEvent>> getOpenTournaments(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        return ResponseEntity.ok(eventService.getOpenEvents(targetCommunityId));
    }

    @GetMapping({"/events/open-all", "/tournaments/open-all"})
    public ResponseEntity<List<SportsEvent>> getAllOpenTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = "SUPER_ADMIN".equals(loggedInUser.getRole());
        if (!isSuperAdmin) {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            return ResponseEntity.ok(eventService.getOpenEvents(communityId));
        }
        return ResponseEntity.ok(eventService.getAllOpenEvents());
    }

    @GetMapping({"/events/closed", "/tournaments/closed"})
    public ResponseEntity<List<SportsEvent>> getClosedTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = "SUPER_ADMIN".equals(loggedInUser.getRole());
        if (!isSuperAdmin) {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            return ResponseEntity.ok(eventService.getClosedEvents(communityId));
        }
        return ResponseEntity.ok(eventService.getClosedEvents());
    }

    @GetMapping({"/events/mine", "/tournaments/mine"})
    public ResponseEntity<List<SportsEvent>> getMyTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getMyEvents(loggedInUser.getId()));
    }

    @GetMapping({"/events/all", "/tournaments/all"})
    public ResponseEntity<List<SportsEvent>> getAllTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = "SUPER_ADMIN".equals(loggedInUser.getRole());
        if (!isSuperAdmin) {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            return ResponseEntity.ok(eventService.getCommunityEvents(communityId));
        }
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping({"/events/community", "/tournaments/community"})
    public ResponseEntity<List<SportsEvent>> getCommunityTournaments(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (targetCommunityId == null || !targetCommunityId.equals(communityId)) {
                throw new UnauthorizedActionException("You can only access your own community's tournaments.");
            }
        }
        return ResponseEntity.ok(eventService.getCommunityEvents(targetCommunityId));
    }

    @PostMapping("/register")
    public ResponseEntity<SportsEventRegistration> register(
            @Valid @RequestBody RegistrationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.registerUser(req, loggedInUser.getId()));
    }

    @DeleteMapping("/register/{registrationId}")
    public ResponseEntity<Void> withdraw(@PathVariable Long registrationId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        eventService.withdraw(registrationId, loggedInUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/events/{eventId}/registrations/import", "/tournaments/{eventId}/registrations/import"})
    public ResponseEntity<SportsEventCsvImportService.ImportResult> importRegistrations(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN, CREATE_EDIT_EVENT_REGISTRATIONS);
        SportsEventCsvImportService.ImportResult result = csvImportService.importRegistrations(eventId, file);
        return ResponseEntity.ok(result);
    }

    @GetMapping({"/events/{eventId}/registrations", "/tournaments/{eventId}/registrations"})
    public ResponseEntity<List<SportsEventRegistration>> getTournamentRegistrations(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getEventRegistrations(eventId));
    }

    @GetMapping("/registrations/mine")
    public ResponseEntity<List<SportsEventRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getUserRegistrations(loggedInUser.getId()));
    }

    @PutMapping("/registrations/{id}/confirm")
    public ResponseEntity<SportsEventRegistration> confirmRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.confirmRegistration(id));
    }

    @PutMapping("/registrations/{id}/reject")
    public ResponseEntity<SportsEventRegistration> rejectRegistration(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.rejectRegistration(id, reason));
    }

    @PutMapping("/registrations/{id}/nominate")
    public ResponseEntity<SportsEventRegistration> nominateCaptain(
            @PathVariable Long id, 
            @RequestParam boolean nominate,
            @RequestParam(required = false) String teamName,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.ok(eventService.nominateCaptain(id, nominate, teamName));
    }

    @PutMapping("/registrations/{id}/confirm-captain")
    public ResponseEntity<SportsEventRegistration> confirmCaptain(
            @PathVariable Long id, 
            @RequestParam boolean confirm,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.ok(eventService.confirmCaptain(id, confirm));
    }
}
