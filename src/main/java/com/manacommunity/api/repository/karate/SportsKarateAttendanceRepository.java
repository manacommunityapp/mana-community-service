package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsKarateAttendanceRepository extends JpaRepository<SportsKarateAttendance, Long> {

    List<SportsKarateAttendance> findByClassSessionId(Long classId);

    List<SportsKarateAttendance> findByEnrollmentId(Long enrollmentId);

    Optional<SportsKarateAttendance> findByClassSessionIdAndEnrollmentId(Long classId, Long enrollmentId);

    boolean existsByClassSessionIdAndEnrollmentId(Long classId, Long enrollmentId);

    /** Count attended sessions (PRESENT or LATE) for an enrollment. */
    @Query("""
            SELECT COUNT(a) FROM SportsKarateAttendance a
            WHERE a.enrollment.id = :enrollmentId
              AND a.status IN ('PRESENT', 'LATE')
            """)
    long countAttendedByEnrollment(@Param("enrollmentId") Long enrollmentId);

    /** All attendance records for an enrollment in an ordered list for history view. */
    @Query("""
            SELECT a FROM SportsKarateAttendance a
            WHERE a.enrollment.id = :enrollmentId
            ORDER BY a.classSession.scheduledDate DESC
            """)
    List<SportsKarateAttendance> findByEnrollmentIdOrdered(@Param("enrollmentId") Long enrollmentId);
}
