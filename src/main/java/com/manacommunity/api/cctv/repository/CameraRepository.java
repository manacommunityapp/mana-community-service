package com.manacommunity.api.cctv.repository;

import com.manacommunity.api.cctv.entity.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CameraRepository extends JpaRepository<Camera, Long> {
    List<Camera> findByCommunityId(Long communityId);
}
