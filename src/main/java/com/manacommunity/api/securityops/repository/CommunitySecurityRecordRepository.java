package com.manacommunity.api.securityops.repository;

import com.manacommunity.api.securityops.entity.CommunitySecurityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunitySecurityRecordRepository extends JpaRepository<CommunitySecurityRecord, Long> {
    List<CommunitySecurityRecord> findByCommunityIdAndRecordTypeOrderByCreatedAtDesc(Long communityId, String recordType);
    List<CommunitySecurityRecord> findByCommunityIdAndRecordTypeAndResidentIdOrderByCreatedAtDesc(Long communityId, String recordType, Long residentId);
}
