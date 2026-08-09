package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventMemberFlowResponse;
import com.manacommunity.api.events.entity.ActivityRegistration;
import com.manacommunity.api.events.repository.ActivityRegistrationRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.MealRegistrationRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventMemberFlowService {

    private static final Set<ActivityRegistration.ActivityRegStatus> ACTIVE_ACTIVITY_STATUSES = Set.of(
            ActivityRegistration.ActivityRegStatus.PENDING,
            ActivityRegistration.ActivityRegStatus.CONFIRMED,
            ActivityRegistration.ActivityRegStatus.WAITLISTED
    );

    private final EventRegistrationRepository eventRegistrationRepo;
    private final ActivityRegistrationRepository activityRegistrationRepo;
    private final MealRegistrationRepository mealRegistrationRepo;

    @Transactional(readOnly = true)
    public EventMemberFlowResponse getSummary(AppUser user) {
        List<ActivityRegistration> activityRegistrations =
                activityRegistrationRepo.findByUserIdOrderByRegisteredAtDesc(user.getId());
        List<ActivityRegistration> activeActivities = activityRegistrations.stream()
                .filter(reg -> reg.getStatus() != null && ACTIVE_ACTIVITY_STATUSES.contains(reg.getStatus()))
                .toList();

        int familyMemberCount = activeActivities.stream()
                .map(ActivityRegistration::getHeadCount)
                .filter(count -> count != null && count > 1)
                .mapToInt(count -> count - 1)
                .max()
                .orElse(0);

        int eventRegistrations = Math.toIntExact(eventRegistrationRepo.countByUserId(user.getId()));
        int mealRegistrations = Math.toIntExact(mealRegistrationRepo.countByUserId(user.getId()));

        return EventMemberFlowResponse.builder()
                .residentName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .flatNumber(user.getFlatNo())
                .block(user.getBlock())
                .tower(user.getTower())
                .residentType(user.getResidentType())
                .occupancyStatus(user.getOccupancyStatus())
                .flatProfileComplete(hasText(user.getFlatNo()) && hasText(user.getFullName())
                        && hasText(user.getEmail()) && hasText(user.getPhone()))
                .familyMemberCount(familyMemberCount)
                .eventRegistrationCount(eventRegistrations)
                .activityRegistrationCount(activeActivities.size())
                .mealRegistrationCount(mealRegistrations)
                .features(List.of(
                        feature("details", "Event Details", true, "Available"),
                        feature("timings", "Event Timings", true, "Available"),
                        feature("programmes", "Event Programmes", true, activeActivities.isEmpty()
                                ? "Ready to explore" : activeActivities.size() + " activity registrations"),
                        feature("donation", "Event Donation", true, "Available"),
                        feature("pooja", "Pooja Registration", true, activeActivities.isEmpty()
                                ? "Ready to register" : "Registration started"),
                        feature("meals", "Lunch / Dinner", true, mealRegistrations == 0
                                ? "Not selected" : mealRegistrations + " meal selections"),
                        feature("auctions", "Auctions", true, "Available")
                ))
                .build();
    }

    private EventMemberFlowResponse.FeatureStatus feature(String key, String label, boolean available, String status) {
        return EventMemberFlowResponse.FeatureStatus.builder()
                .key(key)
                .label(label)
                .available(available)
                .status(status)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
