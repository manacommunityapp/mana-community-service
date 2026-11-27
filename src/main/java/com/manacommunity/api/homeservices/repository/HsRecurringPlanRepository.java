package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsRecurringPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface HsRecurringPlanRepository extends JpaRepository<HsRecurringPlan, Long> {
    List<HsRecurringPlan> findByUserId(Long userId);
    List<HsRecurringPlan> findByStatusAndNextServiceDateLessThanEqual(String status, LocalDate date);
}
