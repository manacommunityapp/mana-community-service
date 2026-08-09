package com.manacommunity.api.media.repository;

import com.manacommunity.api.media.entity.MediaUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaUploadSessionRepository extends JpaRepository<MediaUploadSession, Long> {

    Optional<MediaUploadSession> findBySessionIdAndStatus(UUID sessionId, String status);

    @Modifying
    @Query("UPDATE MediaUploadSession s SET s.status = 'EXPIRED' WHERE s.expiresAt < :now AND s.status = 'PENDING'")
    int expireSessions(@Param("now") OffsetDateTime now);
}
