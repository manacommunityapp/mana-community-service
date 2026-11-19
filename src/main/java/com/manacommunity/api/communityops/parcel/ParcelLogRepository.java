package com.manacommunity.api.communityops.parcel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParcelLogRepository extends JpaRepository<ParcelLog, Long> {
    List<ParcelLog> findByCommunityIdAndStatus(Long communityId, ParcelStatus status);
    List<ParcelLog> findByCommunityIdAndFlatNumberAndStatus(Long communityId, String flatNumber, ParcelStatus status);
    List<ParcelLog> findByResidentUserIdAndStatus(Long residentUserId, ParcelStatus status);
    List<ParcelLog> findByResidentUserIdAndStatusIn(Long residentUserId, List<ParcelStatus> statuses);
    long countByCommunityIdAndStatus(Long communityId, ParcelStatus status);
}
