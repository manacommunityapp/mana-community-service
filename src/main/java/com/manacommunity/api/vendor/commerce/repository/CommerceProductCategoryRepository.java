package com.manacommunity.api.vendor.commerce.repository;

import com.manacommunity.api.vendor.commerce.model.CommerceProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceProductCategoryRepository extends JpaRepository<CommerceProductCategory, Long> {

    List<CommerceProductCategory> findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(Long communityId);

    @Query("SELECT c FROM CommerceProductCategory c WHERE c.community.id = :communityId AND c.parent IS NULL AND c.active = TRUE ORDER BY c.displayOrder")
    List<CommerceProductCategory> findRootCategoriesByCommunity(Long communityId);

    @Query("SELECT c FROM CommerceProductCategory c WHERE c.parent.id = :parentId AND c.active = TRUE ORDER BY c.displayOrder")
    List<CommerceProductCategory> findChildCategories(Long parentId);

    Optional<CommerceProductCategory> findBySlugAndCommunityId(String slug, Long communityId);

    boolean existsBySlugAndCommunityId(String slug, Long communityId);
}
