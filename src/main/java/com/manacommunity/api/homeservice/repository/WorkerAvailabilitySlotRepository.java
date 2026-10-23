package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.WorkerAvailabilitySlotEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceDayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceWorkerAvailabilitySlotRepository")
public interface WorkerAvailabilitySlotRepository extends JpaRepository<WorkerAvailabilitySlotEntity, String> {
    List<WorkerAvailabilitySlotEntity> findByWorkerId(String workerId);
    List<WorkerAvailabilitySlotEntity> findByWorkerIdAndDayOfWeek(String workerId, HomeServiceDayOfWeek dayOfWeek);
}
