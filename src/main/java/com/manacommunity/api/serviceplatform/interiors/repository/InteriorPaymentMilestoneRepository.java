package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorPaymentMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InteriorPaymentMilestoneRepository extends JpaRepository<InteriorPaymentMilestone,Long> {
    List<InteriorPaymentMilestone> findByServiceRequestIdOrderByDueDateAscIdAsc(Long requestId);
    Optional<InteriorPaymentMilestone> findByIdAndServiceRequestId(Long id,Long requestId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from InteriorPaymentMilestone m where m.id=:id and m.serviceRequest.id=:requestId")
    Optional<InteriorPaymentMilestone> lockByIdAndServiceRequestId(@Param("id") Long id,@Param("requestId") Long requestId);
}
