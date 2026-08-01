package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.WaitlistRequest;
import com.manacommunity.api.booking.dto.WaitlistResponse;
import com.manacommunity.api.booking.entity.BookingWaitlist;
import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.enums.WaitlistStatus;
import com.manacommunity.api.booking.repository.BookingWaitlistRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final BookingWaitlistRepository waitlistRepo;
    private final ResourceRepository resourceRepo;

    @Transactional
    public WaitlistResponse joinWaitlist(WaitlistRequest req, AppUser user, Community community) {
        Resource resource = resourceRepo.findById(req.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + req.getResourceId()));

        if (!resource.isAllowWaitlist()) {
            throw new IllegalStateException("Waitlist is not enabled for this resource");
        }

        LocalDate date = LocalDate.parse(req.getRequestedDate());
        LocalTime startTime = LocalTime.parse(req.getRequestedStartTime());
        LocalTime endTime = LocalTime.parse(req.getRequestedEndTime());

        long currentCount = waitlistRepo.countByResourceIdAndRequestedDateAndStatus(
                resource.getId(), date, WaitlistStatus.WAITING);

        BookingWaitlist waitlist = BookingWaitlist.builder()
                .resource(resource)
                .user(user)
                .requestedDate(date)
                .requestedStartTime(startTime)
                .requestedEndTime(endTime)
                .status(WaitlistStatus.WAITING)
                .position((int) currentCount + 1)
                .expiresAt(LocalDateTime.now().plusDays(resource.getAdvanceBookingDays() != null ? resource.getAdvanceBookingDays() : 7))
                .community(community)
                .build();

        return toResponse(waitlistRepo.save(waitlist));
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getMyWaitlist(Long userId) {
        return waitlistRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, WaitlistStatus.WAITING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void cancelWaitlist(Long id, Long userId) {
        BookingWaitlist waitlist = waitlistRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found: " + id));
        if (!waitlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own waitlist entries");
        }
        waitlist.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepo.save(waitlist);
    }

    @Transactional
    public void notifyNextInWaitlist(Long resourceId, LocalDate date) {
        List<BookingWaitlist> waiting = waitlistRepo.findByResourceIdAndRequestedDateAndStatusOrderByPositionAsc(
                resourceId, date, WaitlistStatus.WAITING);
        if (!waiting.isEmpty()) {
            BookingWaitlist next = waiting.get(0);
            next.setStatus(WaitlistStatus.NOTIFIED);
            next.setNotifiedAt(LocalDateTime.now());
            next.setExpiresAt(LocalDateTime.now().plusHours(2));
            waitlistRepo.save(next);
        }
    }

    private WaitlistResponse toResponse(BookingWaitlist w) {
        return WaitlistResponse.builder()
                .id(w.getId())
                .resourceId(w.getResource().getId())
                .resourceName(w.getResource().getName())
                .userId(w.getUser().getId())
                .userName(w.getUser().getFullName())
                .requestedDate(w.getRequestedDate().toString())
                .requestedStartTime(w.getRequestedStartTime().toString())
                .requestedEndTime(w.getRequestedEndTime().toString())
                .status(w.getStatus())
                .position(w.getPosition())
                .notifiedAt(w.getNotifiedAt() != null ? w.getNotifiedAt().toString() : null)
                .expiresAt(w.getExpiresAt() != null ? w.getExpiresAt().toString() : null)
                .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toString() : null)
                .build();
    }
}
