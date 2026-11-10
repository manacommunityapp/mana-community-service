package com.manacommunity.api.schoolbus.repository;

import com.manacommunity.api.schoolbus.entity.RegisteredChild;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegisteredChildRepository extends JpaRepository<RegisteredChild, Long> {
    List<RegisteredChild> findByParentIdAndStatus(Long parentId, String status);
}
