package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.PrescriptionFollowup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PrescriptionFollowupRepository extends JpaRepository<PrescriptionFollowup, Long> {
    List<PrescriptionFollowup> findByCommunityIdAndPatientUserIdOrderByFollowupDueDateAsc(Long communityId, Long patientUserId);
    List<PrescriptionFollowup> findByCommunityIdAndCompletedFalseOrderByFollowupDueDateAsc(Long communityId);

    @Query("SELECT f FROM PrescriptionFollowup f WHERE f.followupDueDate <= :tomorrow AND f.completed = false AND f.reminderSent = false")
    List<PrescriptionFollowup> findDueForReminder(@Param("tomorrow") LocalDate tomorrow);
}
