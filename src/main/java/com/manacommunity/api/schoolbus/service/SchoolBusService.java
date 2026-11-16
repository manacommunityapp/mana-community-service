package com.manacommunity.api.schoolbus.service;

import com.manacommunity.api.schoolbus.dto.RegisterChildRequest;
import com.manacommunity.api.schoolbus.entity.BusTripLog;
import com.manacommunity.api.schoolbus.entity.RegisteredChild;
import com.manacommunity.api.schoolbus.repository.BusTripLogRepository;
import com.manacommunity.api.schoolbus.repository.RegisteredChildRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchoolBusService {

    private final RegisteredChildRepository childRepo;
    private final BusTripLogRepository tripRepo;

    public List<RegisteredChild> getMyChildren(AppUser user) {
        return childRepo.findByParentIdAndStatus(user.getId(), "ACTIVE");
    }

    public RegisteredChild registerChild(RegisterChildRequest req, AppUser user) {
        return childRepo.save(RegisteredChild.builder()
                .parent(user).community(user.getCommunity())
                .name(req.getName()).grade(req.getGrade())
                .schoolName(req.getSchoolName()).busId(req.getBusId())
                .stopName(req.getStopName()).status("ACTIVE").build());
    }

    public List<BusTripLog> getTrips(Long childId, int limit) {
        return tripRepo.findByChildIdOrderByTripDateDesc(childId, PageRequest.of(0, limit));
    }

    public Map<String, Object> getBusLocation(Long busId) {
        return Map.of(
                "busId", busId,
                "lat", 17.3850,
                "lng", 78.4867,
                "status", "EN_ROUTE_HOME",
                "etaMinutes", 12,
                "lastUpdated", System.currentTimeMillis()
        );
    }
}
