package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.enums.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    List<Resource> findByCommunityIdAndStatusAndDeletedFalseOrderByNameAsc(Long communityId, ResourceStatus status);

    List<Resource> findByCommunityIdAndDeletedFalseOrderByNameAsc(Long communityId);

    List<Resource> findByCategoryIdAndDeletedFalseOrderByNameAsc(Long categoryId);

    List<Resource> findByCommunityIdAndCategoryIdAndStatusAndDeletedFalse(Long communityId, Long categoryId, ResourceStatus status);

    long countByCommunityIdAndDeletedFalse(Long communityId);
}
