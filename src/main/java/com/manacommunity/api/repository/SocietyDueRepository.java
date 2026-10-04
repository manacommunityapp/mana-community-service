package com.manacommunity.api.repository;

import com.manacommunity.api.model.SocietyDue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SocietyDueRepository extends JpaRepository<SocietyDue, Long> {

    List<SocietyDue> findByCommunityIdOrderByDueDateDesc(Long communityId);

    List<SocietyDue> findByCommunityIdAndStatusOrderByDueDateDesc(Long communityId, SocietyDue.DueStatus status);

    List<SocietyDue> findByCommunityIdAndFlatNumberOrderByDueDateDesc(Long communityId, String flatNumber);

    Optional<SocietyDue> findByIdAndCommunityId(Long id, Long communityId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM SocietyDue d WHERE d.communityId = :cid AND d.status = :status")
    BigDecimal sumByCommunityIdAndStatus(@Param("cid") Long communityId, @Param("status") SocietyDue.DueStatus status);

    long countByCommunityIdAndStatus(Long communityId, SocietyDue.DueStatus status);
}
