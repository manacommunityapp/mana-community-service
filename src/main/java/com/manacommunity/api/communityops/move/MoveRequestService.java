package com.manacommunity.api.communityops.move;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoveRequestService {

    private final MoveRequestRepository moveRepo;
    private final MoveChecklistItemRepository itemRepo;
    private final MoveChecklistCompletionRepository completionRepo;

    @Transactional
    public MoveRequest createRequest(Long communityId, Long residentId, MoveRequestDto req) {
        MoveRequest move = moveRepo.save(MoveRequest.builder()
            .communityId(communityId)
            .residentUserId(residentId)
            .flatNumber(req.flatNumber())
            .moveType(req.moveType())
            .plannedDate(req.plannedDate())
            .plannedTime(req.plannedTime())
            .vehicleNumber(req.vehicleNumber())
            .moversCompany(req.moversCompany())
            .contactNumber(req.contactNumber())
            .status(MoveStatus.REQUESTED)
            .build());

        // Copy global (community_id=0) checklist items for this community+moveType
        List<MoveChecklistItem> templates = itemRepo
            .findByCommunityIdInAndMoveTypeOrderByDisplayOrderAsc(List.of(0L, communityId), req.moveType());
        for (MoveChecklistItem item : templates) {
            completionRepo.save(MoveChecklistCompletion.builder()
                .moveRequestId(move.getId())
                .checklistItemId(item.getId())
                .completed(false)
                .build());
        }
        return move;
    }

    @Transactional
    public MoveRequest approve(Long moveId, Long adminId, String notes) {
        MoveRequest move = getById(moveId);
        move.setStatus(MoveStatus.APPROVED);
        move.setReviewedBy(adminId);
        move.setReviewedAt(Instant.now());
        move.setReviewNotes(notes);
        move.setUpdatedAt(Instant.now());
        return moveRepo.save(move);
    }

    @Transactional
    public MoveRequest reject(Long moveId, Long adminId, String notes) {
        MoveRequest move = getById(moveId);
        move.setStatus(MoveStatus.REJECTED);
        move.setReviewedBy(adminId);
        move.setReviewedAt(Instant.now());
        move.setReviewNotes(notes);
        move.setUpdatedAt(Instant.now());
        return moveRepo.save(move);
    }

    @Transactional
    public MoveChecklistCompletion updateChecklist(Long moveId, Long itemId, boolean completed, Long userId, String notes) {
        MoveChecklistCompletion comp = completionRepo
            .findByMoveRequestIdAndChecklistItemId(moveId, itemId)
            .orElseGet(() -> MoveChecklistCompletion.builder()
                .moveRequestId(moveId).checklistItemId(itemId).build());
        comp.setCompleted(completed);
        comp.setCompletedBy(userId);
        comp.setCompletedAt(completed ? Instant.now() : null);
        comp.setNotes(notes);
        return completionRepo.save(comp);
    }

    @Transactional
    public MoveRequest issueNoc(Long moveId, Long adminId) {
        MoveRequest move = getById(moveId);
        List<MoveChecklistCompletion> completions = completionRepo.findByMoveRequestId(moveId);
        List<MoveChecklistItem> mandatoryItems = itemRepo
            .findByCommunityIdInAndMoveTypeOrderByDisplayOrderAsc(
                List.of(0L, move.getCommunityId()), move.getMoveType())
            .stream().filter(MoveChecklistItem::isMandatory).toList();

        boolean allMandatoryDone = mandatoryItems.stream().allMatch(item ->
            completions.stream().anyMatch(c -> c.getChecklistItemId().equals(item.getId()) && c.isCompleted()));

        if (!allMandatoryDone) throw new IllegalStateException("All mandatory checklist items must be completed before NOC");
        move.setNocIssued(true);
        move.setNocIssuedAt(Instant.now());
        move.setUpdatedAt(Instant.now());
        return moveRepo.save(move);
    }

    public List<MoveRequest> getForCommunity(Long communityId, MoveStatus status) {
        return status != null ? moveRepo.findByCommunityIdAndStatus(communityId, status) : moveRepo.findByCommunityId(communityId);
    }

    public List<MoveRequest> getForResident(Long residentId) {
        return moveRepo.findByResidentUserId(residentId);
    }

    public List<MoveChecklistCompletion> getChecklist(Long moveId) {
        return completionRepo.findByMoveRequestId(moveId);
    }

    private MoveRequest getById(Long id) {
        return moveRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Move request not found"));
    }
}
