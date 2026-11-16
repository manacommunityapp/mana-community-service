package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryPhotoJob;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JewelleryPhotoJobRepository extends JpaRepository<JewelleryPhotoJob, Long> {

    Optional<JewelleryPhotoJob> findByIdAndUser(Long id, AppUser user);

    List<JewelleryPhotoJob> findByUserAndUserSavedTrueOrderByCreatedAtDesc(AppUser user);

    /** Claim oldest queued jobs for background processing */
    List<JewelleryPhotoJob> findTop10ByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT j FROM JewelleryPhotoJob j WHERE j.expiresAt < :now AND j.status NOT IN ('DELETION_PENDING', 'EXPIRED')")
    List<JewelleryPhotoJob> findExpiredJobs(LocalDateTime now);

    @Modifying
    @Query("UPDATE JewelleryPhotoJob j SET j.status = 'EXPIRED' WHERE j.expiresAt < :now AND j.status NOT IN ('DELETION_PENDING', 'EXPIRED', 'FAILED')")
    int markExpiredJobs(LocalDateTime now);
}
