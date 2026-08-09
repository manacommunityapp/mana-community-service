package com.manacommunity.api.repository;

import com.manacommunity.api.model.EventDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventDepartmentRepository extends JpaRepository<EventDepartment, Long> {

    List<EventDepartment> findByActiveTrueOrderByNameAsc();

    List<EventDepartment> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);

    List<EventDepartment> findByEventIdAndActiveTrueOrderByNameAsc(Long eventId);
}
