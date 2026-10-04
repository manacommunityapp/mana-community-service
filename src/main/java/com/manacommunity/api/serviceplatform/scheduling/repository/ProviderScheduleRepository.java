package com.manacommunity.api.serviceplatform.scheduling.repository;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, Long> { List<ProviderSchedule> findByProviderIdAndIsActiveTrue(Long providerId); List<ProviderSchedule> findByProviderIdAndDayOfWeekAndIsActiveTrue(Long providerId, Integer dayOfWeek); }
