package com.manacommunity.api.communityops.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffLeaveRequestRepository extends JpaRepository<StaffLeaveRequest, Long> {
    List<StaffLeaveRequest> findByCommunityIdAndStatus(Long communityId, LeaveStatus status);
    List<StaffLeaveRequest> findByStaffUserId(Long staffUserId);
    List<StaffLeaveRequest> findByCommunityId(Long communityId);
}
