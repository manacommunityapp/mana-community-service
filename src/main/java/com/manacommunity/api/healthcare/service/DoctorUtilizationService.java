package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.DoctorSlotUtilization;
import com.manacommunity.api.healthcare.repository.DoctorSlotUtilizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorUtilizationService {

    private final DoctorSlotUtilizationRepository repo;

    @Transactional
    public DoctorSlotUtilization upsert(Long communityId, Long doctorId, LocalDate slotDate,
                                         int totalSlots, int bookedSlots, int attendedSlots,
                                         int noShowSlots, int cancelledSlots) {
        DoctorSlotUtilization u = repo
                .findByCommunityIdAndDoctorIdAndSlotDate(communityId, doctorId, slotDate)
                .orElseGet(() -> {
                    DoctorSlotUtilization n = new DoctorSlotUtilization();
                    n.setCommunityId(communityId);
                    n.setDoctorId(doctorId);
                    n.setSlotDate(slotDate);
                    return n;
                });
        u.setTotalSlots(totalSlots);
        u.setBookedSlots(bookedSlots);
        u.setAttendedSlots(attendedSlots);
        u.setNoShowSlots(noShowSlots);
        u.setCancelledSlots(cancelledSlots);
        u.setCalculatedAt(Instant.now());
        return repo.save(u);
    }

    public List<DoctorSlotUtilization> getForDoctor(Long communityId, Long doctorId, LocalDate from, LocalDate to) {
        return repo.findByCommunityIdAndDoctorIdAndSlotDateBetweenOrderBySlotDateAsc(communityId, doctorId, from, to);
    }

    public List<DoctorSlotUtilization> getForDate(Long communityId, LocalDate date) {
        return repo.findByCommunityIdAndSlotDateOrderByDoctorIdAsc(communityId, date);
    }
}
