package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.dto.HomeServiceBidRequest;
import com.manacommunity.api.homeservice.dto.HomeServiceRequirementRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceRequirementEntity;
import com.manacommunity.api.homeservice.model.entity.RequirementWorkerResponseEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceBidStatus;
import com.manacommunity.api.homeservice.model.enums.HomeServiceRequirementStatus;
import com.manacommunity.api.homeservice.repository.HomeServiceRequirementRepository;
import com.manacommunity.api.homeservice.repository.RequirementWorkerResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("homeServiceRequirementService")
@RequiredArgsConstructor
public class HomeServiceRequirementService {
    private final HomeServiceRequirementRepository requirementRepository;
    private final RequirementWorkerResponseRepository responseRepository;

    @Transactional
    public HomeServiceRequirementEntity createRequirement(HomeServiceRequirementRequest req) {
        HomeServiceRequirementEntity entity = HomeServiceRequirementEntity.builder()
                .id(UUID.randomUUID().toString())
                .communityId(req.getCommunityId())
                .residentUserId(req.getResidentUserId())
                .residentName(req.getResidentName())
                .flatNumber(req.getFlatNumber())
                .tower(req.getTower())
                .categoryId(req.getCategoryId())
                .frequency(req.getFrequency())
                .preferredDays(req.getPreferredDays() != null ? String.join(",", req.getPreferredDays()) : null)
                .startDate(req.getStartDate())
                .preferredStartTime(req.getPreferredStartTime())
                .preferredEndTime(req.getPreferredEndTime())
                .budgetMin(req.getBudgetMin())
                .budgetMax(req.getBudgetMax())
                .description(req.getDescription())
                .status(HomeServiceRequirementStatus.OPEN)
                .responsesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return requirementRepository.save(entity);
    }

    public List<HomeServiceRequirementEntity> getCommunityRequirements(String communityId) {
        return requirementRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @Transactional
    public RequirementWorkerResponseEntity submitBid(HomeServiceBidRequest req) {
        RequirementWorkerResponseEntity bid = RequirementWorkerResponseEntity.builder()
                .id(UUID.randomUUID().toString())
                .requestId(req.getRequestId())
                .workerId(req.getWorkerId())
                .workerName(req.getWorkerName())
                .workerPhone(req.getWorkerPhone())
                .proposedPrice(req.getProposedPrice())
                .message(req.getMessage())
                .status(HomeServiceBidStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        bid = responseRepository.save(bid);

        requirementRepository.findById(req.getRequestId()).ifPresent(r -> {
            r.setResponsesCount((r.getResponsesCount() != null ? r.getResponsesCount() : 0) + 1);
            requirementRepository.save(r);
        });

        return bid;
    }

    public List<RequirementWorkerResponseEntity> getBidsForRequirement(String requestId) {
        return responseRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
    }
}
