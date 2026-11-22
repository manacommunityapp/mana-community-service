package com.manacommunity.api.platform.dpdp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataRightsRequestRepository extends JpaRepository<DataRightsRequest, Long> {

    List<DataRightsRequest> findByResidentId(Long residentId);

    List<DataRightsRequest> findByCommunityIdAndStatus(Long communityId, String status);
}
