package com.manacommunity.api.search;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Long> {

    Optional<SearchIndex> findByCommunityIdAndModuleAndEntityId(
            Long communityId, SearchModule module, String entityId);

    void deleteByCommunityIdAndModuleAndEntityId(
            Long communityId, SearchModule module, String entityId);

    /**
     * Full-text search with weighted ranking and optional module filter.
     * Returns columns in order: id, community_id, module, entity_id, title, subtitle,
     * description, tags, image_url, deep_link_type, deep_link_id, visibility,
     * owner_user_id, metadata, is_active, indexed_at, updated_at, rank
     */
    @Query(value = """
            SELECT si.id, si.community_id, si.module, si.entity_id,
                   si.title, si.subtitle, si.description, si.tags, si.image_url,
                   si.deep_link_type, si.deep_link_id, si.visibility,
                   si.owner_user_id, si.metadata, si.is_active,
                   si.indexed_at, si.updated_at,
                   ts_rank(si.search_vector, plainto_tsquery('english', :query)) AS rank
            FROM search_index si
            WHERE si.community_id = :communityId
              AND si.is_active = true
              AND si.search_vector @@ plainto_tsquery('english', :query)
              AND (:modules IS NULL OR si.module = ANY(string_to_array(:modules, ',')))
              AND si.visibility IN ('PUBLIC', 'COMMUNITY')
            ORDER BY rank DESC, si.updated_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchFts(
            @Param("communityId") Long communityId,
            @Param("query") String query,
            @Param("modules") String modules,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT si.module, COUNT(*) AS cnt
            FROM search_index si
            WHERE si.community_id = :communityId
              AND si.is_active = true
              AND si.search_vector @@ plainto_tsquery('english', :query)
              AND si.visibility IN ('PUBLIC', 'COMMUNITY')
            GROUP BY si.module
            """, nativeQuery = true)
    List<Object[]> countFacets(
            @Param("communityId") Long communityId,
            @Param("query") String query
    );
}
