package com.manacommunity.api.noticeboard.statutory.repository;

import com.manacommunity.api.noticeboard.statutory.entity.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, Long> {
    List<ReadReceipt> findByNoticeId(Long noticeId);
    List<ReadReceipt> findByNoticeIdAndBeaconStatus(Long noticeId, ReadReceipt.BeaconStatus status);
    List<ReadReceipt> findByFlatNumberAndTowerName(String flat, String tower);
    Optional<ReadReceipt> findByBeaconTrackingId(String beaconTrackingId);

    @Query("SELECT r.beaconStatus, COUNT(r) FROM ReadReceipt r WHERE r.noticeId = :noticeId GROUP BY r.beaconStatus")
    List<Object[]> countByStatusForNotice(Long noticeId);

    @Query("SELECT r.towerName, r.beaconStatus, COUNT(r) FROM ReadReceipt r WHERE r.noticeId = :noticeId GROUP BY r.towerName, r.beaconStatus")
    List<Object[]> towerWiseDeliveryForNotice(Long noticeId);

    @Query("SELECT r FROM ReadReceipt r WHERE r.noticeId = :noticeId AND r.beaconStatus IN ('BOUNCED', 'FAILED')")
    List<ReadReceipt> findFailedForNotice(Long noticeId);
}
