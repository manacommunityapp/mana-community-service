package com.manacommunity.api.serviceplatform.scheduling.repository;
import com.manacommunity.api.serviceplatform.scheduling.entity.ProviderTimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface ProviderTimeOffRepository extends JpaRepository<ProviderTimeOff, Long> { List<ProviderTimeOff> findByProviderIdAndIsApprovedTrue(Long providerId); }
