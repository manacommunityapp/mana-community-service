package com.manacommunity.api.mobile.schoolbus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegisteredChildRepository extends JpaRepository<RegisteredChild, Long> {
    List<RegisteredChild> findByUserIdAndActiveTrue(Long userId);
    List<RegisteredChild> findByBusIdAndActiveTrue(Long busId);
}
