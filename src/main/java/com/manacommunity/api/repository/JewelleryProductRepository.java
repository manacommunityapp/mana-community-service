package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryProduct;
import com.manacommunity.api.model.JewelleryVendorProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JewelleryProductRepository extends JpaRepository<JewelleryProduct, Long> {

    /** Paginated unfiltered product list. */
    org.springframework.data.domain.Page<JewelleryProduct> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);


    List<JewelleryProduct> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);

    List<JewelleryProduct> findByMaterialAndStatusOrderByCreatedAtDesc(String material, String status);

    List<JewelleryProduct> findByCategoryAndMaterialAndStatusOrderByCreatedAtDesc(
            String category, String material, String status);

    List<JewelleryProduct> findByVendorAndStatusOrderByCreatedAtDesc(JewelleryVendorProfile vendor, String status);

    Optional<JewelleryProduct> findByIdAndStatus(Long id, String status);

    @Query("""
            SELECT p FROM JewelleryProduct p
            WHERE p.status = 'ACTIVE'
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.createdAt DESC
            """)
    List<JewelleryProduct> searchActive(@Param("q") String query, Pageable pageable);

    @Query("""
            SELECT p FROM JewelleryProduct p
            WHERE p.status = 'ACTIVE'
            ORDER BY p.createdAt DESC
            """)
    List<JewelleryProduct> findFeatured(Pageable pageable);
}
