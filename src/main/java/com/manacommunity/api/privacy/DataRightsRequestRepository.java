package com.manacommunity.api.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataRightsRequestRepository extends JpaRepository<DataRightsRequest, Long> {

    List<DataRightsRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<DataRightsRequest> findByStatusOrderByCreatedAtAsc(String status);
}
