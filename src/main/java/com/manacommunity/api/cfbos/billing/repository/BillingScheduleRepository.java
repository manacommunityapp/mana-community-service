package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.BillingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, Long> { List<BillingSchedule> findByIsActiveTrue(); }
