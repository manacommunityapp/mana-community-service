package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.dto.HomeServiceBookingRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceBookingEntity;
import com.manacommunity.api.homeservice.model.entity.WorkerFlatAssignmentEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceBookingStatus;
import com.manacommunity.api.homeservice.repository.HomeServiceBookingRepository;
import com.manacommunity.api.homeservice.repository.WorkerFlatAssignmentRepository;
import com.manacommunity.api.homeservice.exception.SlotConflictException;
import com.manacommunity.api.homeservice.exception.HomeServiceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("homeServiceBookingService")
@RequiredArgsConstructor
public class HomeServiceBookingService {
    private final HomeServiceBookingRepository bookingRepository;
    private final WorkerFlatAssignmentRepository assignmentRepository;

    @Transactional
    public HomeServiceBookingEntity createBooking(HomeServiceBookingRequest request) {
        List<HomeServiceBookingEntity> conflicts = bookingRepository.findConflictingBookings(
                request.getWorkerId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            throw new SlotConflictException(
                    "Worker already has an active booking between " + request.getStartTime() + " and " + request.getEndTime()
            );
        }

        HomeServiceBookingEntity booking = HomeServiceBookingEntity.builder()
                .id(UUID.randomUUID().toString())
                .communityId(request.getCommunityId())
                .residentUserId(request.getResidentUserId())
                .flatNumber(request.getFlatNumber())
                .tower(request.getTower())
                .workerId(request.getWorkerId())
                .categoryId(request.getCategoryId())
                .bookingType(request.getBookingType())
                .pricingModel(request.getPricingModel())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .recurringDays(request.getRecurringDays() != null ? String.join(",", request.getRecurringDays()) : null)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(HomeServiceBookingStatus.REQUESTED)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return bookingRepository.save(booking);
    }

    @Transactional
    public HomeServiceBookingEntity updateBookingStatus(String bookingId, HomeServiceBookingStatus newStatus, String cancellationReason) {
        HomeServiceBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new HomeServiceNotFoundException("Booking not found: " + bookingId));

        booking.setStatus(newStatus);
        if (cancellationReason != null) {
            booking.setCancellationReason(cancellationReason);
        }
        booking.setUpdatedAt(LocalDateTime.now());

        if (newStatus == HomeServiceBookingStatus.CONFIRMED) {
            WorkerFlatAssignmentEntity assignment = WorkerFlatAssignmentEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .workerId(booking.getWorkerId())
                    .communityId(booking.getCommunityId())
                    .flatNumber(booking.getFlatNumber())
                    .tower(booking.getTower())
                    .residentUserId(booking.getResidentUserId())
                    .serviceCategoryId(booking.getCategoryId())
                    .scheduleSummary(booking.getStartTime() + " - " + booking.getEndTime())
                    .startDate(booking.getStartDate())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            assignmentRepository.save(assignment);
        }

        return bookingRepository.save(booking);
    }

    public List<HomeServiceBookingEntity> getBookingsByResident(String residentUserId) {
        return bookingRepository.findByResidentUserIdOrderByCreatedAtDesc(residentUserId);
    }

    public List<HomeServiceBookingEntity> getBookingsByWorker(String workerId) {
        return bookingRepository.findByWorkerIdOrderByCreatedAtDesc(workerId);
    }
}
