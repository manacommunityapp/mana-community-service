package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsServicePackageRepository extends JpaRepository<HsServicePackage, Long> {
    List<HsServicePackage> findByCommunityIdAndIsActiveTrue(Long communityId);
    List<HsServicePackage> findByCommunityIdAndCategoryAndIsActiveTrue(Long communityId, String category);
}
