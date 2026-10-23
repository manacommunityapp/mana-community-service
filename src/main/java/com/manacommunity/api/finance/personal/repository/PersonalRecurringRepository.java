package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalRecurring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonalRecurringRepository extends JpaRepository<PersonalRecurring, String> {
    List<PersonalRecurring> findByUserIdAndIsActiveTrueOrderByNextDueDateAsc(Long userId);
    List<PersonalRecurring> findByUserIdOrderByNextDueDateAsc(Long userId);
    List<PersonalRecurring> findByIsActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
    List<PersonalRecurring> findByUserIdAndIsActiveTrueAndNextDueDateLessThanEqual(Long userId, LocalDate date);
}
