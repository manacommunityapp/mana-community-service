package com.manacommunity.api.access.biometric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricUserEnrollmentRepository extends JpaRepository<BiometricUserEnrollment, Long> {
    Optional<BiometricUserEnrollment> findByUserId(Long userId);
    Optional<BiometricUserEnrollment> findByFaceEmbeddingHash(String faceEmbeddingHash);
    List<BiometricUserEnrollment> findByCommunityId(Long communityId);
}
