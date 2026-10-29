package com.manacommunity.api.mobile.schoolbus;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mobile-facing School Bus tracking API.
 */
@RestController
@RequestMapping("/api/school-bus")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SchoolBusController {

    private final SchoolBusRepository busRepo;
    private final RegisteredChildRepository childRepo;
    private final BusTripLogRepository tripRepo;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/school-bus/my-children */
    @GetMapping("/my-children")
    public ResponseEntity<List<Map<String, Object>>> getMyChildren(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<Map<String, Object>> children = childRepo.findByUserIdAndActiveTrue(user.getId())
                .stream().map(this::toChildResponse).collect(Collectors.toList());
        return ResponseEntity.ok(children);
    }

    /** GET /api/school-bus/buses */
    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> getBuses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String schoolName) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = resolveCommunityId(user);
        List<SchoolBus> buses = schoolName != null && !schoolName.isBlank()
                ? busRepo.findByCommunityIdAndSchoolNameContainingIgnoreCase(communityId, schoolName)
                : busRepo.findByCommunityIdOrderBySchoolName(communityId);
        return ResponseEntity.ok(buses.stream().map(this::toBusResponse).collect(Collectors.toList()));
    }

    /** GET /api/school-bus/buses/{busId} */
    @GetMapping("/buses/{busId}")
    public ResponseEntity<Map<String, Object>> getBus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long busId) {
        AppUser user = loggedInUserService.resolve(principal);
        SchoolBus bus = busRepo.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolBus", busId));
        return ResponseEntity.ok(toBusResponse(bus));
    }

    /** GET /api/school-bus/buses/{busId}/location */
    @GetMapping("/buses/{busId}/location")
    public ResponseEntity<Map<String, Object>> getBusLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long busId) {
        SchoolBus bus = busRepo.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolBus", busId));
        if (bus.getCurrentLat() == null || bus.getCurrentLng() == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("lat",       bus.getCurrentLat());
        location.put("lng",       bus.getCurrentLng());
        location.put("updatedAt", bus.getLocationUpdatedAt() != null ? bus.getLocationUpdatedAt().toString() : null);
        return ResponseEntity.ok(location);
    }

    /** POST /api/school-bus/register-child */
    @PostMapping("/register-child")
    public ResponseEntity<Map<String, Object>> registerChild(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Community community = user.getCommunity();
        if (community == null) return ResponseEntity.badRequest().build();

        SchoolBus bus = null;
        Object busIdObj = body.get("busId");
        if (busIdObj != null) {
            Long busId = Long.parseLong(busIdObj.toString());
            bus = busRepo.findById(busId).orElse(null);
        }

        RegisteredChild child = RegisteredChild.builder()
                .community(community)
                .user(user)
                .bus(bus)
                .name(String.valueOf(body.getOrDefault("name", "")))
                .grade(String.valueOf(body.getOrDefault("grade", "")))
                .schoolName(String.valueOf(body.getOrDefault("schoolName", "")))
                .stopName(body.get("stopName") != null ? String.valueOf(body.get("stopName")) : null)
                .active(true)
                .build();

        child = childRepo.save(child);
        return ResponseEntity.status(HttpStatus.CREATED).body(toChildResponse(child));
    }

    /** GET /api/school-bus/children/{childId}/trips */
    @GetMapping("/children/{childId}/trips")
    public ResponseEntity<List<Map<String, Object>>> getTripHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long childId) {
        AppUser user = loggedInUserService.resolve(principal);
        RegisteredChild child = childRepo.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("RegisteredChild", childId));
        if (!child.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You do not own this child registration.");
        }
        List<Map<String, Object>> trips = tripRepo.findByChildIdOrderByTripDateDescCreatedAtDesc(childId)
                .stream().map(t -> toTripResponse(t)).collect(Collectors.toList());
        return ResponseEntity.ok(trips);
    }

    /** GET /api/school-bus/children/{childId}/trips/today */
    @GetMapping("/children/{childId}/trips/today")
    public ResponseEntity<Map<String, Object>> getTodayTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long childId) {
        AppUser user = loggedInUserService.resolve(principal);
        RegisteredChild child = childRepo.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("RegisteredChild", childId));
        if (!child.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You do not own this child registration.");
        }
        return tripRepo.findFirstByChildIdAndTripDateOrderByCreatedAtDesc(childId, LocalDate.now())
                .map(t -> ResponseEntity.ok(toTripResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Long resolveCommunityId(AppUser user) {
        if (user.getCommunity() == null) {
            throw new UnauthorizedActionException("You are not associated with any community.");
        }
        return user.getCommunity().getId();
    }

    private Map<String, Object> toChildResponse(RegisteredChild c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         c.getId());
        m.put("name",       c.getName());
        m.put("grade",      c.getGrade());
        m.put("schoolName", c.getSchoolName());
        m.put("stopName",   c.getStopName());
        m.put("photo",      c.getPhotoUrl());
        m.put("busId",      c.getBus() != null ? c.getBus().getId() : null);
        m.put("busNumber",  c.getBus() != null ? c.getBus().getBusNumber() : null);
        return m;
    }

    private Map<String, Object> toBusResponse(SchoolBus b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             b.getId());
        m.put("busNumber",      b.getBusNumber());
        m.put("driverName",     b.getDriverName());
        m.put("driverPhone",    b.getDriverPhone());
        m.put("attendantName",  b.getAttendantName());
        m.put("attendantPhone", b.getAttendantPhone());
        m.put("schoolName",     b.getSchoolName());
        m.put("routeName",      b.getRouteName());
        m.put("status",         b.getStatus());
        m.put("stops",          List.of());
        return m;
    }

    private Map<String, Object> toTripResponse(BusTripLog t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            t.getId());
        m.put("busId",         t.getBus().getId());
        m.put("direction",     t.getDirection());
        m.put("date",          t.getTripDate().toString());
        m.put("departureTime", t.getDepartureTime());
        m.put("arrivalTime",   t.getArrivalTime());
        m.put("childPickedUp", t.isChildPickedUp());
        m.put("pickedUpAt",    t.getPickedUpAt());
        m.put("droppedOffAt",  t.getDroppedOffAt());
        m.put("notes",         t.getNotes());
        return m;
    }
}
