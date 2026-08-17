package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.PoojaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoojaTypeRepository extends JpaRepository<PoojaType, Long> {

    @Query("SELECT p FROM PoojaType p WHERE (:communityId IS NULL OR p.communityId = :communityId OR p.communityId IS NULL) ORDER BY p.name ASC")
    List<PoojaType> findByCommunityOrGlobal(@Param("communityId") Long communityId);

    List<PoojaType> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<PoojaType> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
