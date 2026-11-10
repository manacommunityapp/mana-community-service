package com.manacommunity.api.security.camera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SecurityCameraRepository extends JpaRepository<SecurityCamera, Long> {
    List<SecurityCamera> findByCommunityIdAndIsActiveTrueOrderByNameAsc(Long communityId);
    List<SecurityCamera> findByCommunityIdAndStatusAndIsActiveTrue(Long communityId, String status);
}
