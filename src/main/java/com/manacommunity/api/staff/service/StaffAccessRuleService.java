package com.manacommunity.api.staff.service;

import com.manacommunity.api.staff.entity.StaffAccessRule;
import com.manacommunity.api.staff.repository.StaffAccessRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffAccessRuleService {

    private final StaffAccessRuleRepository repo;

    @Transactional
    public StaffAccessRule setRule(Long communityId, Long staffId, String dayOfWeek, LocalTime from, LocalTime until) {
        return repo.findByStaffIdAndDayOfWeek(staffId, dayOfWeek)
                .map(existing -> {
                    existing.setAllowedFrom(from);
                    existing.setAllowedUntil(until);
                    existing.setIsActive(true);
                    return repo.save(existing);
                })
                .orElseGet(() -> repo.save(StaffAccessRule.builder()
                        .communityId(communityId)
                        .staffId(staffId)
                        .dayOfWeek(dayOfWeek)
                        .allowedFrom(from)
                        .allowedUntil(until)
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<StaffAccessRule> getRules(Long staffId) {
        return repo.findByStaffIdAndIsActiveTrueOrderByDayOfWeek(staffId);
    }

    @Transactional
    public void deleteRule(Long id) {
        repo.findById(id).ifPresent(r -> {
            r.setIsActive(false);
            repo.save(r);
        });
    }

    @Transactional(readOnly = true)
    public boolean isAccessAllowed(Long staffId, String dayOfWeek, LocalTime currentTime) {
        return repo.findByStaffIdAndDayOfWeek(staffId, dayOfWeek)
                .map(r -> r.getIsActive()
                        && !currentTime.isBefore(r.getAllowedFrom())
                        && !currentTime.isAfter(r.getAllowedUntil()))
                .orElse(true); // no rule = always allowed
    }
}
