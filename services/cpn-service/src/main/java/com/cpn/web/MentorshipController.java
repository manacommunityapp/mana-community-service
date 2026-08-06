package com.cpn.web;

import com.cpn.application.mentorship.MentorshipService;
import com.cpn.domain.mentorship.model.MentorBooking;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/mentorship")
@RequiredArgsConstructor
public class MentorshipController {

    private final MentorshipService mentorshipService;

    @PostMapping("/book")
    public ResponseEntity<MentorBooking> bookSession(@RequestBody MentorBooking booking) {
        return ResponseEntity.ok(mentorshipService.bookSession(booking));
    }

    @GetMapping("/mentee/{menteeId}")
    public ResponseEntity<List<MentorBooking>> getBookings(@PathVariable UUID menteeId) {
        return ResponseEntity.ok(mentorshipService.getMenteeBookings(menteeId));
    }
}
