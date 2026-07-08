package com.manacommunity.api.visitor.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitorPassService {

    private final VisitorPassRepository repo;

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getCommunityPasses(Long communityId) {
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getActivePasses(Long communityId) {
        return repo.findByCommunityAndStatuses(communityId,
                        List.of(VisitorPass.PassStatus.APPROVED, VisitorPass.PassStatus.CHECKED_IN))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getMyPasses(Long residentId) {
        return repo.findByResidentIdOrderByCreatedAtDesc(residentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VisitorPassResponse getByPassCode(String passCode) {
        return toResponse(repo.findByPassCode(passCode)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + passCode)));
    }

    @Transactional(readOnly = true)
    public VisitorPassResponse getById(Long id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getTodaysPasses(Long communityId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return repo.findRecentByCommunity(communityId, startOfDay)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VisitorPassResponse create(VisitorPassRequest req, AppUser resident, Community community) {
        VisitorPass pass = VisitorPass.builder()
                .passCode(generatePassCode())
                .visitorName(req.getVisitorName())
                .visitorPhone(req.getVisitorPhone())
                .vehicleNumber(req.getVehicleNumber())
                .purpose(req.getPurpose())
                .passType(parseEnum(VisitorPass.PassType.class, req.getPassType()))
                .flatNumber(req.getFlatNumber())
                .resident(resident)
                .community(community)
                .status(VisitorPass.PassStatus.APPROVED)
                .build();

        if (req.getExpectedAt() != null && !req.getExpectedAt().isBlank()) {
            pass.setExpectedAt(LocalDateTime.parse(req.getExpectedAt()));
        }

        return toResponse(repo.save(pass));
    }

    @Transactional
    public VisitorPassResponse checkIn(Long id) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        if (pass.getStatus() != VisitorPass.PassStatus.APPROVED) {
            throw new IllegalStateException("Pass is not in APPROVED status");
        }
        pass.setStatus(VisitorPass.PassStatus.CHECKED_IN);
        pass.setCheckedInAt(LocalDateTime.now());
        return toResponse(repo.save(pass));
    }

    @Transactional
    public VisitorPassResponse checkInByCode(String passCode) {
        VisitorPass pass = repo.findByPassCode(passCode)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + passCode));
        if (pass.getStatus() != VisitorPass.PassStatus.APPROVED) {
            throw new IllegalStateException("Pass is not in APPROVED status");
        }
        pass.setStatus(VisitorPass.PassStatus.CHECKED_IN);
        pass.setCheckedInAt(LocalDateTime.now());
        return toResponse(repo.save(pass));
    }

    @Transactional
    public VisitorPassResponse checkOut(Long id) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        if (pass.getStatus() != VisitorPass.PassStatus.CHECKED_IN) {
            throw new IllegalStateException("Visitor has not checked in");
        }
        pass.setStatus(VisitorPass.PassStatus.CHECKED_OUT);
        pass.setCheckedOutAt(LocalDateTime.now());
        return toResponse(repo.save(pass));
    }

    @Transactional
    public void reject(Long id) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        pass.setStatus(VisitorPass.PassStatus.REJECTED);
        repo.save(pass);
    }

    private String generatePassCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private VisitorPassResponse toResponse(VisitorPass p) {
        return VisitorPassResponse.builder()
                .id(p.getId())
                .passCode(p.getPassCode())
                .visitorName(p.getVisitorName())
                .visitorPhone(p.getVisitorPhone())
                .vehicleNumber(p.getVehicleNumber())
                .purpose(p.getPurpose())
                .passType(p.getPassType() != null ? p.getPassType().name() : null)
                .status(p.getStatus().name())
                .expectedAt(formatDt(p.getExpectedAt()))
                .checkedInAt(formatDt(p.getCheckedInAt()))
                .checkedOutAt(formatDt(p.getCheckedOutAt()))
                .flatNumber(p.getFlatNumber())
                .residentId(p.getResident().getId())
                .residentName(p.getResident().getFullName())
                .communityId(p.getCommunity() != null ? p.getCommunity().getId() : null)
                .createdAt(formatDt(p.getCreatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
