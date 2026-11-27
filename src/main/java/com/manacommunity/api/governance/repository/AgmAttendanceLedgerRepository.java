package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.AgmAttendanceLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgmAttendanceLedgerRepository extends JpaRepository<AgmAttendanceLedger, Long> {
    Optional<AgmAttendanceLedger> findByMeetingIdAndFlatId(Long meetingId, Long flatId);
    List<AgmAttendanceLedger> findByMeetingIdAndStatus(Long meetingId, String status);
    long countByMeetingIdAndStatus(Long meetingId, String status);
    long countByMeetingIdAndProxyHolderUserIdAndStatusIn(Long meetingId, Long proxyHolderUserId, List<String> statuses);
}
