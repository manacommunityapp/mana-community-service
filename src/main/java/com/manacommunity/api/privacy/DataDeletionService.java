package com.manacommunity.api.privacy;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.privacy.dto.DataDeletionRequestDto;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataDeletionService {

    private final DataDeletionRequestRepository requestRepo;
    private final AppUserRepository userRepo;
    private final FamilyMemberRepository familyMemberRepo;
    private final UserSessionRepository userSessionRepo;
    private final UserPrivacySettingsRepository privacySettingsRepo;
    private final MarketListingRepository marketListingRepo;
    private final PrivacyAuditService privacyAuditService;
    private final AuditService auditService;

    @Transactional
    public DataDeletionRequestDto submitRequest(Long userId, String reason, Long communityId) {
        // Check for existing pending request
        requestRepo.findFirstByUserIdAndStatusInOrderByRequestedAtDesc(
                userId, List.of(DataDeletionRequest.DeletionStatus.PENDING, DataDeletionRequest.DeletionStatus.VERIFIED))
                .ifPresent(req -> {
                    throw new InvalidInputException("An active data deletion request already exists for this account.");
                });

        String verificationToken = UUID.randomUUID().toString();

        DataDeletionRequest req = DataDeletionRequest.builder()
                .userId(userId)
                .communityId(communityId)
                .status(DataDeletionRequest.DeletionStatus.PENDING)
                .reason(reason)
                .verificationToken(verificationToken)
                .verificationExpiry(LocalDateTime.now().plusDays(7))
                .build();

        DataDeletionRequest saved = requestRepo.save(req);

        privacyAuditService.record(
                AuditAction.ACCOUNT_DELETION_REQUESTED.name(),
                "USER",
                String.valueOf(userId),
                communityId,
                "Reason: " + (reason != null ? reason : "Not specified")
        );
        auditService.record(
                AuditAction.ACCOUNT_DELETION_REQUESTED,
                AuditModule.PRIVACY,
                "DataDeletionRequest",
                String.valueOf(saved.getId())
        );

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DataDeletionRequestDto> getUserRequests(Long userId) {
        return requestRepo.findByUserIdOrderByRequestedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DataDeletionRequestDto> getCommunityRequests(Long communityId) {
        return requestRepo.findByCommunityIdAndStatusOrderByRequestedAtDesc(
                communityId, DataDeletionRequest.DeletionStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public DataDeletionRequestDto processDeletion(Long requestId, Long processedByUserId, String notes) {
        DataDeletionRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DataDeletionRequest", requestId));

        if (request.getStatus() == DataDeletionRequest.DeletionStatus.COMPLETED) {
            throw new InvalidInputException("Request has already been processed.");
        }

        Long targetUserId = request.getUserId();
        AppUser targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        // 1. Anonymize user PII in AppUser
        String anonId = "anon_" + UUID.randomUUID().toString().substring(0, 8);
        targetUser.setFullName("Deactivated User (" + anonId + ")");
        targetUser.setEmail(anonId + "@deleted.manacommunity.com");
        targetUser.setPhone("000000" + (1000 + (int)(Math.random() * 9000)));
        targetUser.setProfilePicUrl(null);
        targetUser.setGovtIdNumber(null);
        targetUser.setGovtIdType(null);
        targetUser.setDateOfBirth(null);
        targetUser.setIsActive(false);
        userRepo.save(targetUser);

        // 2. Delete Family Members associated with this user
        familyMemberRepo.findByUserIdOrderByCreatedAtAsc(targetUserId)
                .forEach(familyMemberRepo::delete);

        // 3. Mark all active marketplace listings as DELETED — they must not stay visible after account deletion
        try {
            marketListingRepo.findBySellerIdAndStatus(targetUserId, MarketListing.ListingStatus.ACTIVE)
                    .forEach(listing -> {
                        listing.setStatus(MarketListing.ListingStatus.DELETED);
                        marketListingRepo.save(listing);
                    });
        } catch (Exception e) {
            log.warn("Could not remove marketplace listings for user {}: {}", targetUserId, e.getMessage());
        }

        // 4. Clear active user login sessions
        try {
            userSessionRepo.deleteAll(userSessionRepo.findByUserIdAndStatus(targetUserId, "ACTIVE"));
            userSessionRepo.deleteAll(userSessionRepo.findByUserIdAndStatus(targetUserId, "LOGGED_IN"));
        } catch (Exception e) {
            log.warn("Could not clear sessions for user {}: {}", targetUserId, e.getMessage());
        }

        // 5. Clean privacy settings
        privacySettingsRepo.findByUserId(targetUserId)
                .ifPresent(privacySettingsRepo::delete);

        // 5. Update deletion request record
        request.setStatus(DataDeletionRequest.DeletionStatus.COMPLETED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedByUserId);
        request.setNotes(notes);
        DataDeletionRequest saved = requestRepo.save(request);

        // 6. Record audit log
        privacyAuditService.record(
                AuditAction.ACCOUNT_DELETION_COMPLETED.name(),
                "USER",
                String.valueOf(targetUserId),
                request.getCommunityId(),
                "Data deletion executed by admin ID: " + processedByUserId
        );
        auditService.record(
                AuditAction.ACCOUNT_DELETION_COMPLETED,
                AuditModule.PRIVACY,
                "DataDeletionRequest",
                String.valueOf(saved.getId())
        );

        return toDto(saved);
    }

    @Transactional
    public DataDeletionRequestDto rejectRequest(Long requestId, Long processedByUserId, String notes) {
        DataDeletionRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DataDeletionRequest", requestId));

        request.setStatus(DataDeletionRequest.DeletionStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedByUserId);
        request.setNotes(notes);
        DataDeletionRequest saved = requestRepo.save(request);

        auditService.record(
                AuditAction.CONFIG_UPDATED,
                AuditModule.PRIVACY,
                "DataDeletionRequest",
                String.valueOf(saved.getId()),
                "PENDING",
                "REJECTED"
        );

        return toDto(saved);
    }

    @Transactional
    public DataDeletionRequestDto cancelRequest(Long requestId, Long userId) {
        DataDeletionRequest request;
        if (requestId != null) {
            request = requestRepo.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("DataDeletionRequest", requestId));
        } else {
            request = requestRepo.findFirstByUserIdAndStatusInOrderByRequestedAtDesc(
                    userId, List.of(DataDeletionRequest.DeletionStatus.PENDING, DataDeletionRequest.DeletionStatus.VERIFIED))
                    .orElseThrow(() -> new ResourceNotFoundException("Active DataDeletionRequest for user " + userId));
        }

        if (!request.getUserId().equals(userId)) {
            throw new InvalidInputException("You can only cancel your own deletion request.");
        }

        if (request.getStatus() != DataDeletionRequest.DeletionStatus.PENDING &&
            request.getStatus() != DataDeletionRequest.DeletionStatus.VERIFIED) {
            throw new InvalidInputException("Only pending deletion requests can be cancelled.");
        }

        request.setStatus(DataDeletionRequest.DeletionStatus.CANCELLED);
        request.setProcessedAt(LocalDateTime.now());
        request.setNotes("Cancelled by resident");
        DataDeletionRequest saved = requestRepo.save(request);

        auditService.record(
                AuditAction.CONFIG_UPDATED,
                AuditModule.PRIVACY,
                "DataDeletionRequest",
                String.valueOf(saved.getId()),
                "PENDING",
                "CANCELLED"
        );

        return toDto(saved);
    }

    private DataDeletionRequestDto toDto(DataDeletionRequest r) {
        return DataDeletionRequestDto.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .communityId(r.getCommunityId())
                .status(r.getStatus().name())
                .reason(r.getReason())
                .requestedAt(r.getRequestedAt())
                .processedAt(r.getProcessedAt())
                .processedBy(r.getProcessedBy())
                .notes(r.getNotes())
                .build();
    }
}
