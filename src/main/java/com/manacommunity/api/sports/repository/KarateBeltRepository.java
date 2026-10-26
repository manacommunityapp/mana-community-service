package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.KarateBelt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KarateBeltRepository extends JpaRepository<KarateBelt, Long> {
    List<KarateBelt> findAllByOrderBySortOrderAsc();
}
