package com.manacommunity.api.access.biometric;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiometricAccessLogRepository extends JpaRepository<BiometricAccessLog, Long> {
    Page<BiometricAccessLog> findByCommunityIdOrderByTimestampDesc(Long communityId, Pageable pageable);
    Page<BiometricAccessLog> findByTurnstileIdOrderByTimestampDesc(Long turnstileId, Pageable pageable);
}
