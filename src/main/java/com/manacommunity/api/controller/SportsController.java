package com.manacommunity.api.controller;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.dto.PlayerCategoryRequest;
import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.dto.SponsorDto;
import com.manacommunity.api.dto.SportsEventRequest;
import com.manacommunity.api.dto.SportsEventResponse;
import com.manacommunity.api.dto.SportsRegistrationResponse;
import com.manacommunity.api.dto.SportsMetaRequest;
import com.manacommunity.api.dto.SportsMetaResponse;
import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.PlayerCategoryRepository;
import com.manacommunity.api.repository.SportMetaRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.SportsEventService;
import com.manacommunity.api.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsEventCsvImportService;
import org.springframework.web.multipart.MultipartFile;
import static com.manacommunity.api.constants.PermissionConstants.*;

@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsController {

    private final SportsEventService eventService;
    private final SportMetaRepository sportMetaRepo;
    private final PlayerCategoryRepository categoryRepo;
    private final LoggedInUserService loggedInUserService;
    private final TournamentService tournamentService;
    private final PermissionCheckService permissionCheckService;
    private final SportsEventCsvImportService csvImportService;

    @GetMapping("/meta")
    public ResponseEntity<List<SportsMetaResponse>> getAllSports(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(sportMetaRepo.findByActiveTrue().stream()
                .map(this::toSportsMetaResponse).toList());
    }

    @PostMapping("/meta")
    public ResponseEntity<SportsMetaResponse> createSport(
            @Valid @RequestBody SportsMetaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        SportsMeta sport = SportsMeta.builder()
                .name(request.name())
                .icon(request.icon())
                .iconUrl(request.iconUrl())
                .formats(request.formats() != null ? request.formats() : List.of())
                .active(request.active() != null ? request.active() : true)
                .build();
        if (principal != null) {
            AppUser loggedInUser = loggedInUserService.resolve(principal);
            if (loggedInUser != null && !ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
                if (loggedInUser.getCommunity() != null) {
                    sport.setCommunityId(loggedInUser.getCommunity().getId());
                }
            }
        }
        SportsMeta saved = sportMetaRepo.save(sport);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSportsMetaResponse(saved));
    }

    @PutMapping("/meta/{id}")
    public ResponseEntity<SportsMetaResponse> updateSport(
            @PathVariable Long id,
            @Valid @RequestBody SportsMetaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        SportsMeta sport = sportMetaRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Sport", id));

        AppUser loggedInUser = loggedInUserService.resolve(principal);
        if (sport.getCommunityId() == null) {
            if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
                throw new UnauthorizedActionException("You do not have permission to modify this sport.");
            }
        } else if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            Long userCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (userCommunityId == null || !userCommunityId.equals(sport.getCommunityId())) {
                throw new UnauthorizedActionException("You do not have permission to modify this sport.");
            }
        }

        sport.setName(request.name());
        sport.setIcon(request.icon());
        sport.setIconUrl(request.iconUrl());
        if (request.formats() != null) {
            sport.setFormats(request.formats());
        }
        if (request.active() != null) {
            sport.setActive(request.active());
        }
        return ResponseEntity.ok(toSportsMetaResponse(sportMetaRepo.save(sport)));
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
                if (loggedInUser == null || !ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
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
                if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
                    Long userCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
                    if (userCommunityId == null || !userCommunityId.equals(sport.getCommunityId())) {
                        throw new UnauthorizedActionException("You do not have permission to delete this sport.");
                    }
                }
            }

            sport.setActive(false);
            sportMetaRepo.save(sport);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<PlayerCategory>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        if (ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            return ResponseEntity.ok(categoryRepo.findAll());
        }
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
        String userRole = loggedInUser.getRole();
        String typeValue;
        switch (userRole) {
            case ROLE_SUPER_ADMIN:
                typeValue = "DEFAULT";
                break;
            case ROLE_ADMIN, ROLE_SPORTS_ADMIN:
                typeValue = "USER";
                break;
            case ROLE_VENDOR:
                typeValue = "VENDOR";
                break;
            default:
                throw new com.manacommunity.api.exception.InvalidInputException("Unknown user role: " + userRole);
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
    public ResponseEntity<SportsEventResponse> createSportsEvent(
            @Valid @RequestBody SportsEventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            req.setCommunityId(loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null);
        }

        SportsEvent created = eventService.createEvent(req, loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toEventResponse(created));
    }

    @PutMapping("/events/{id}/status")
    public ResponseEntity<SportsEventResponse> updateStatus(
            @PathVariable Long id, @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(toEventResponse(eventService.updateStatus(id, status)));
    }

    @PutMapping("/tournaments/{id}")
    public ResponseEntity<SportsEventResponse> updateTournament(
            @PathVariable Long id, @Valid @RequestBody TournamentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        Tournament tournament = tournamentService.saveTournamentRecord(req, req.getAllowAdminChat());

        SportsEvent updated = eventService.getEventById(id);
        return ResponseEntity.ok(toEventResponse(updated));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<SportsEventResponse> updateSportsEvent(
            @PathVariable Long id, @Valid @RequestBody SportsEventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            req.setCommunityId(loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null);
        }

        return ResponseEntity.ok(toEventResponse(eventService.updateEvent(id, req)));
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
    public ResponseEntity<SportsEventResponse> getTournamentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        return ResponseEntity.ok(toEventResponse(eventService.getEventById(id)));
    }

    @GetMapping("/events/by-uuid/{uuid}")
    public ResponseEntity<SportsEventResponse> getEventByUuid(@PathVariable java.util.UUID uuid) {
        return ResponseEntity.ok(toEventResponse(eventService.getEventByUuid(uuid)));
    }

    @GetMapping({"/events/{eventId}/confirmed-count", "/tournaments/{eventId}/confirmed-count"})
    public ResponseEntity<Long> getConfirmedCount(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        return ResponseEntity.ok(eventService.getConfirmedRegistrationCount(eventId));
    }

    @PutMapping({"/events/{id}/committee", "/tournaments/{id}/committee"})
    public ResponseEntity<SportsEventResponse> updateCommittee(
            @PathVariable Long id,
            @RequestBody List<Long> committeeIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        SportsEvent event = eventService.getEventById(id);
        String idsString = committeeIds == null ? "" :
            committeeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        event.setDisputeCommitteeIds(idsString);
        return ResponseEntity.ok(toEventResponse(eventService.saveEvent(event)));
    }

    @GetMapping({"/event-list-map", "/tournament-list-map"})
    public ResponseEntity<List<Map<String, Object>>> getTournamentMap(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        return ResponseEntity.ok(eventService.getEventMap(targetCommunityId));
    }

    @GetMapping({"/events/open", "/tournaments/open"})
    public ResponseEntity<List<SportsEventResponse>> getOpenTournaments(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(eventService.getOpenEvents(targetCommunityId).stream()
                .map(this::toEventResponse).toList());
    }

    @GetMapping({"/events/open-all", "/tournaments/open-all"})
    public ResponseEntity<List<SportsEventResponse>> getAllOpenTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(loggedInUser.getRole());
        if (!isSuperAdmin) {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(eventService.getOpenEvents(communityId).stream()
                    .map(this::toEventResponse).toList());
        }
        return ResponseEntity.ok(eventService.getAllOpenEvents().stream()
                .map(this::toEventResponse).toList());
    }

    @GetMapping({"/events/closed", "/tournaments/closed"})
    public ResponseEntity<List<SportsEventResponse>> getClosedTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(loggedInUser.getRole());
        if (!isSuperAdmin) {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(eventService.getClosedEvents(communityId).stream()
                    .map(this::toEventResponse).toList());
        }
        return ResponseEntity.ok(eventService.getClosedEvents().stream()
                .map(this::toEventResponse).toList());
    }

    @GetMapping({"/events/mine", "/tournaments/mine"})
    public ResponseEntity<List<SportsEventResponse>> getMyTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getMyEvents(loggedInUser.getId()).stream()
                .map(this::toEventResponse).toList());
    }

    @GetMapping({"/events/all", "/tournaments/all"})
    public ResponseEntity<PagedResponse<SportsEventResponse>> getAllTournaments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(loggedInUser.getRole());
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("eventDateStart").descending());

        Page<SportsEvent> eventPage;
        if (!isSuperAdmin) {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(PagedResponse.empty());
            }
            eventPage = eventService.getCommunityEvents(communityId, pageable);
        } else {
            eventPage = eventService.getAllEvents(pageable);
        }
        return ResponseEntity.ok(PagedResponse.from(eventPage, this::toEventResponse));
    }

    @GetMapping({"/events/community", "/tournaments/community"})
    public ResponseEntity<List<SportsEventResponse>> getCommunityTournaments(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (targetCommunityId == null || !targetCommunityId.equals(communityId)) {
                throw new UnauthorizedActionException("You can only access your own community's tournaments.");
            }
        }
        return ResponseEntity.ok(eventService.getCommunityEvents(targetCommunityId).stream()
                .map(this::toEventResponse).toList());
    }

    @PostMapping("/register")
    public ResponseEntity<SportsRegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toRegistrationResponse(eventService.registerUser(req, loggedInUser.getId())));
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
    public ResponseEntity<List<SportsRegistrationResponse>> getTournamentRegistrations(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getEventRegistrations(eventId).stream()
                .map(this::toRegistrationResponse).toList());
    }

    @GetMapping("/registrations/mine")
    public ResponseEntity<List<SportsRegistrationResponse>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getUserRegistrations(loggedInUser.getId()).stream()
                .map(this::toRegistrationResponse).toList());
    }

    @PutMapping("/registrations/{id}/confirm")
    public ResponseEntity<SportsRegistrationResponse> confirmRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(toRegistrationResponse(eventService.confirmRegistration(id)));
    }

    @PutMapping("/registrations/{id}/reject")
    public ResponseEntity<SportsRegistrationResponse> rejectRegistration(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_EVENT_REGISTRATIONS);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(toRegistrationResponse(eventService.rejectRegistration(id, reason)));
    }

    @PutMapping("/registrations/{id}/nominate")
    public ResponseEntity<SportsRegistrationResponse> nominateCaptain(
            @PathVariable Long id,
            @RequestParam boolean nominate,
            @RequestParam(required = false) String teamName,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.ok(toRegistrationResponse(eventService.nominateCaptain(id, nominate, teamName)));
    }

    @PutMapping("/registrations/{id}/confirm-captain")
    public ResponseEntity<SportsRegistrationResponse> confirmCaptain(
            @PathVariable Long id,
            @RequestParam boolean confirm,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.ok(toRegistrationResponse(eventService.confirmCaptain(id, confirm)));
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private SportsEventResponse toEventResponse(SportsEvent e) {
        SportsEventResponse.SportRef sportRef = null;
        if (e.getSport() != null) {
            sportRef = SportsEventResponse.SportRef.builder()
                    .id(e.getSport().getId())
                    .name(e.getSport().getName())
                    .icon(e.getSport().getIcon())
                    .iconUrl(e.getSport().getIconUrl())
                    .build();
        }

        SportsEventResponse.CommunityRef communityRef = null;
        if (e.getCommunity() != null) {
            communityRef = SportsEventResponse.CommunityRef.builder()
                    .id(e.getCommunity().getId())
                    .name(e.getCommunity().getName())
                    .build();
        }

        SportsEventResponse.VenueRef venueRef = null;
        if (e.getVenue() != null) {
            venueRef = SportsEventResponse.VenueRef.builder()
                    .id(e.getVenue().getId())
                    .name(e.getVenue().getName())
                    .address(e.getVenue().getAddress())
                    .city(e.getVenue().getCity())
                    .area(e.getVenue().getArea())
                    .build();
        }

        SportsEventResponse.CreatedByRef createdByRef = null;
        if (e.getCreatedBy() != null) {
            createdByRef = SportsEventResponse.CreatedByRef.builder()
                    .id(e.getCreatedBy().getId())
                    .name(e.getCreatedBy().getFullName())
                    .email(e.getCreatedBy().getEmail())
                    .build();
        }

        List<SportsEventResponse.CategoryRef> categoryRefs = e.getCategories() != null
                ? e.getCategories().stream().map(c -> SportsEventResponse.CategoryRef.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .categoryType(c.getCategory_type())
                        .description(c.getDescription())
                        .minAge(c.getMinAge())
                        .maxAge(c.getMaxAge())
                        .gender(c.getGender())
                        .type(c.getType())
                        .build()).toList()
                : List.of();

        List<SponsorDto> sponsorDtos = e.getSponsors() != null
                ? e.getSponsors().stream().map(s -> {
                    SponsorDto dto = new SponsorDto();
                    dto.setCategory(s.getCategory());
                    dto.setName(s.getName());
                    dto.setUrl(s.getUrl());
                    return dto;
                }).toList()
                : List.of();

        SportsEventResponse.TournamentRef tournamentRef = null;
        if (e.getTournament() != null) {
            tournamentRef = SportsEventResponse.TournamentRef.builder()
                    .id(e.getTournament().getId())
                    .name(e.getTournament().getName())
                    .registrationStatus(e.getTournament().getRegistrationStatus() != null
                            ? e.getTournament().getRegistrationStatus().name() : null)
                    .build();
        }

        return SportsEventResponse.builder()
                .id(e.getId())
                .uuid(e.getUuid())
                .name(e.getName())
                .icon(e.getIcon())
                .minAge(e.getMinAge())
                .maxAge(e.getMaxAge())
                .minPlayers(e.getMinPlayers())
                .maxPlayers(e.getMaxPlayers())
                .gender(e.getGender())
                .playersBorn(e.getPlayersBorn())
                .active(e.getActive())
                .adminApprovalRequired(e.getAdminApprovalRequired())
                .sport(sportRef)
                .community(communityRef)
                .venue(venueRef)
                .createdBy(createdByRef)
                .eventDateStart(e.getEventDateStart())
                .eventDateEnd(e.getEventDateEnd())
                .registrationDateStart(e.getRegistrationDateStart())
                .registrationDateEnd(e.getRegistrationDateEnd())
                .maxParticipants(e.getMaxParticipants())
                .startTime(e.getStartTime())
                .dueTime(e.getDueTime())
                .auctionStatus(e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null)
                .format(e.getFormat())
                .tournamentType(e.getTournamentType() != null ? e.getTournamentType().name() : null)
                .categories(categoryRefs)
                .sponsors(sponsorDtos)
                .contactName(e.getContactName())
                .contactNumber(e.getContactNumber())
                .contactEmail(e.getContactEmail())
                .otherContacts(e.getOtherContacts())
                .auctionEnabled(e.getAuctionEnabled())
                .bannerImage(e.getBannerImage())
                .tournamentLevel(e.getTournamentLevel())
                .description(e.getDescription())
                .disputeCommitteeIds(e.getDisputeCommitteeIds())
                .tournament(tournamentRef)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private SportsRegistrationResponse toRegistrationResponse(SportsEventRegistration r) {
        SportsRegistrationResponse.EventRef eventRef = null;
        if (r.getEvent() != null) {
            SportsRegistrationResponse.SportRef sportRef = null;
            if (r.getEvent().getSport() != null) {
                sportRef = SportsRegistrationResponse.SportRef.builder()
                        .id(r.getEvent().getSport().getId())
                        .name(r.getEvent().getSport().getName())
                        .build();
            }
            eventRef = SportsRegistrationResponse.EventRef.builder()
                    .id(r.getEvent().getId())
                    .name(r.getEvent().getName())
                    .eventDateStart(r.getEvent().getEventDateStart() != null ? r.getEvent().getEventDateStart().toString() : null)
                    .eventDateEnd(r.getEvent().getEventDateEnd() != null ? r.getEvent().getEventDateEnd().toString() : null)
                    .sport(sportRef)
                    .build();
        }

        SportsRegistrationResponse.UserRef userRef = null;
        if (r.getUser() != null) {
            userRef = SportsRegistrationResponse.UserRef.builder()
                    .id(r.getUser().getId())
                    .fullName(r.getUser().getFullName())
                    .name(r.getUser().getFullName())
                    .email(r.getUser().getEmail())
                    .phone(r.getUser().getPhone())
                    .flatNo(r.getUser().getFlatNo())
                    .build();
        }

        SportsRegistrationResponse.CategoryRef categoryRef = null;
        if (r.getCategory() != null) {
            categoryRef = SportsRegistrationResponse.CategoryRef.builder()
                    .id(r.getCategory().getId())
                    .name(r.getCategory().getName())
                    .categoryType(r.getCategory().getCategory_type())
                    .minAge(r.getCategory().getMinAge())
                    .maxAge(r.getCategory().getMaxAge())
                    .gender(r.getCategory().getGender())
                    .build();
        }

        SportsRegistrationResponse.UserRef partnerRef = null;
        if (r.getPartner() != null) {
            partnerRef = SportsRegistrationResponse.UserRef.builder()
                    .id(r.getPartner().getId())
                    .fullName(r.getPartner().getFullName())
                    .name(r.getPartner().getFullName())
                    .email(r.getPartner().getEmail())
                    .build();
        }

        return SportsRegistrationResponse.builder()
                .id(r.getId())
                .event(eventRef)
                .user(userRef)
                .category(categoryRef)
                .matchType(r.getMatchType() != null ? r.getMatchType().name() : null)
                .partner(partnerRef)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .playerName(r.getPlayerName())
                .email(r.getEmail())
                .relation(r.getRelation())
                .flatNumber(r.getFlatNumber())
                .age(r.getAge())
                .role(r.getRole())
                .captainNomination(r.getCaptainNomination())
                .captainConfirmation(r.getCaptainConfirmation())
                .proposedTeamName(r.getProposedTeamName())
                .registeredAt(r.getRegisteredAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private SportsMetaResponse toSportsMetaResponse(SportsMeta sport) {
        return new SportsMetaResponse(
                sport.getId(),
                sport.getName(),
                sport.getIcon(),
                sport.getIconUrl(),
                sport.getCommunityId(),
                sport.getFormats(),
                sport.getActive()
        );
    }
}
