package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InteriorChangeRequestRepository extends JpaRepository<InteriorChangeRequest,Long> {
    List<InteriorChangeRequest> findByServiceRequestIdOrderByCreatedAtDesc(Long requestId);
    Optional<InteriorChangeRequest> findByIdAndServiceRequestId(Long id,Long requestId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InteriorChangeRequest c where c.id=:id and c.serviceRequest.id=:requestId")
    Optional<InteriorChangeRequest> lockByIdAndServiceRequestId(@Param("id") Long id,@Param("requestId") Long requestId);
}
