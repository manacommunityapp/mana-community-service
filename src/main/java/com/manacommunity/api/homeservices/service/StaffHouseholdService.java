package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.model.StaffAttendance;
import com.manacommunity.api.homeservices.model.StaffHouseholdLink;
import com.manacommunity.api.homeservices.repository.DomesticStaffRepository;
import com.manacommunity.api.homeservices.repository.StaffAttendanceRepository;
import com.manacommunity.api.homeservices.repository.StaffHouseholdLinkRepository;
import com.manacommunity.api.model.Community;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffHouseholdService {

    private final StaffHouseholdLinkRepository linkRepo;
    private final DomesticStaffRepository staffRepo;
    private final StaffAttendanceRepository attendanceRepo;
    private final EntityManager em;

    public StaffHouseholdLink linkToHousehold(Long communityId, Long staffId, Long residentUserId,
                                               String flatNumber, String schedule) {
        linkRepo.findByStaffIdAndResidentUserIdAndActiveTrue(staffId, residentUserId)
                .ifPresent(l -> { throw new IllegalStateException("Link already exists"); });
        Community community = em.getReference(Community.class, communityId);
        StaffHouseholdLink link = StaffHouseholdLink.builder()
                .community(community)
                .staffId(staffId)
                .residentUserId(residentUserId)
                .flatNumber(flatNumber)
                .schedule(schedule)
                .sinceDate(LocalDate.now())
                .build();
        return linkRepo.save(link);
    }

    public void unlink(Long linkId, Long residentUserId) {
        StaffHouseholdLink link = linkRepo.findById(linkId)
                .filter(l -> l.getResidentUserId().equals(residentUserId))
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));
        link.setActive(false);
    }

    @Transactional(readOnly = true)
    public List<DomesticStaff> getMyStaff(Long residentUserId) {
        return linkRepo.findByResidentUserIdAndActiveTrue(residentUserId).stream()
                .map(l -> staffRepo.findById(l.getStaffId()).orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StaffAttendance> getTodayPresentStaff(Long communityId) {
        return attendanceRepo.findByStaffCommunityIdAndDateOrderByStaffNameAsc(communityId, LocalDate.now())
                .stream()
                .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_IN)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StaffHouseholdLink> getCommunityLinks(Long communityId) {
        return linkRepo.findByCommunityIdAndActiveTrue(communityId);
    }
}
