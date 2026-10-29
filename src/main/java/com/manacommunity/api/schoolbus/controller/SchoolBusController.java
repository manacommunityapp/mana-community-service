package com.manacommunity.api.schoolbus.controller;

import com.manacommunity.api.schoolbus.dto.RegisterChildRequest;
import com.manacommunity.api.schoolbus.entity.BusTripLog;
import com.manacommunity.api.schoolbus.entity.RegisteredChild;
import com.manacommunity.api.schoolbus.service.SchoolBusService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/school-bus")
@RequiredArgsConstructor
public class SchoolBusController {

    private final SchoolBusService schoolBusService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/my-children")
    public ResponseEntity<List<RegisteredChild>> getMyChildren(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(schoolBusService.getMyChildren(loggedInUserService.resolve(principal)));
    }

    @PostMapping("/register-child")
    public ResponseEntity<RegisteredChild> registerChild(@RequestBody RegisterChildRequest req,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolBusService.registerChild(req, user));
    }

    @GetMapping("/children/{childId}/trips")
    public ResponseEntity<List<BusTripLog>> getTrips(@PathVariable Long childId,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(schoolBusService.getTrips(childId, limit));
    }

    @GetMapping("/buses/{busId}/location")
    public ResponseEntity<Map<String, Object>> getBusLocation(@PathVariable Long busId) {
        return ResponseEntity.ok(schoolBusService.getBusLocation(busId));
    }
}
