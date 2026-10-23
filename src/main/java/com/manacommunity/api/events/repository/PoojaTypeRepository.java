package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoojaTypeRepository extends JpaRepository<EventPoojaType, Long> {

    @Query("SELECT p FROM EventPoojaType p WHERE (:communityId IS NULL OR p.communityId = :communityId OR p.communityId IS NULL) ORDER BY p.name ASC")
    List<EventPoojaType> findByCommunityOrGlobal(@Param("communityId") Long communityId);

    List<EventPoojaType> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<EventPoojaType> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    Optional<EventPoojaType> findFirstByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
