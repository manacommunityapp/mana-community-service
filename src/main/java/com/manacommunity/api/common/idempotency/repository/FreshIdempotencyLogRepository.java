package com.manacommunity.api.common.idempotency.repository;

import com.manacommunity.api.common.idempotency.entity.FreshIdempotencyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface FreshIdempotencyLogRepository extends JpaRepository<FreshIdempotencyLog, String> {

    Optional<FreshIdempotencyLog> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("DELETE FROM FreshIdempotencyLog l WHERE l.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") Instant now);
}
