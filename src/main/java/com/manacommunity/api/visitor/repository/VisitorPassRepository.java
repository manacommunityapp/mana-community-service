package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.VisitorPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitorPassRepository extends JpaRepository<VisitorPass, Long> {

    Optional<VisitorPass> findByPassCode(String passCode);

    Optional<VisitorPass> findByOtp(String otp);

    Optional<VisitorPass> findFirstByVisitorPhoneAndStatusInOrderByCreatedAtDesc(
            String phone, List<VisitorPass.PassStatus> statuses);

    List<VisitorPass> findByResidentIdAndStatusOrderByCreatedAtDesc(
            Long residentId, VisitorPass.PassStatus status);

    List<VisitorPass> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            Long communityId, VisitorPass.PassStatus status);

    List<VisitorPass> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    List<VisitorPass> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT v FROM VisitorPass v WHERE v.community.id = :communityId " +
           "AND v.status IN :statuses ORDER BY v.createdAt DESC")
    List<VisitorPass> findByCommunityAndStatuses(
            @Param("communityId") Long communityId,
            @Param("statuses") List<VisitorPass.PassStatus> statuses);

    @Query("SELECT v FROM VisitorPass v WHERE v.community.id = :communityId " +
           "AND v.createdAt >= :since ORDER BY v.createdAt DESC")
    List<VisitorPass> findRecentByCommunity(
            @Param("communityId") Long communityId,
            @Param("since") LocalDateTime since);
}
