package com.manacommunity.api.sports.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.TournamentBudgetRequest;
import com.manacommunity.api.sports.dto.TournamentBudgetResponse;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.model.TournamentBudget;
import com.manacommunity.api.sports.model.TournamentBudgetLineItem;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import com.manacommunity.api.sports.repository.SportsTournamentPlayerPaymentRepository;
import com.manacommunity.api.sports.repository.TournamentBudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentBudgetService {

    private final TournamentBudgetRepository budgetRepository;
    private final SportsTournamentRepository tournamentRepository;
    private final SportsTournamentPlayerPaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<TournamentBudgetResponse> listByCommunity(Long communityId) {
        return budgetRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .map(b -> toResponse(b, paymentRepository.sumAmountByTournament(b.getTournament().getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TournamentBudgetResponse getByTournament(Long tournamentId) {
        TournamentBudget budget = budgetRepository.findByTournamentIdWithLineItems(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("No budget found for tournament " + tournamentId));
        return toResponse(budget, paymentRepository.sumAmountByTournament(tournamentId));
    }

    @Transactional
    public TournamentBudgetResponse createOrUpdate(Long tournamentId, TournamentBudgetRequest request) {
        SportsTournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + tournamentId));

        TournamentBudget budget = budgetRepository.findByTournamentId(tournamentId)
                .orElseGet(() -> TournamentBudget.builder()
                        .tournament(tournament)
                        .communityId(request.getCommunityId())
                        .build());

        budget.setTotalPlannedAmount(request.getTotalPlannedAmount());
        budget.setNotes(request.getNotes());
        budget.getLineItems().clear();

        if (request.getLineItems() != null) {
            request.getLineItems().forEach(li -> {
                TournamentBudgetLineItem item = TournamentBudgetLineItem.builder()
                        .budget(budget)
                        .category(li.getCategory())
                        .description(li.getDescription())
                        .plannedAmount(li.getPlannedAmount())
                        .build();
                budget.getLineItems().add(item);
            });
        }

        TournamentBudget saved = budgetRepository.save(budget);
        return toResponse(saved, paymentRepository.sumAmountByTournament(tournamentId));
    }

    @Transactional
    public void delete(Long tournamentId) {
        TournamentBudget budget = budgetRepository.findByTournamentId(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("No budget found for tournament " + tournamentId));
        budgetRepository.delete(budget);
    }

    private TournamentBudgetResponse toResponse(TournamentBudget b, BigDecimal collected) {
        BigDecimal variance = collected.subtract(b.getTotalPlannedAmount());
        List<TournamentBudgetResponse.LineItemResponse> items = b.getLineItems().stream()
                .map(li -> TournamentBudgetResponse.LineItemResponse.builder()
                        .id(li.getId())
                        .category(li.getCategory())
                        .description(li.getDescription())
                        .plannedAmount(li.getPlannedAmount())
                        .createdAt(li.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return TournamentBudgetResponse.builder()
                .id(b.getId())
                .tournamentId(b.getTournament().getId())
                .tournamentName(b.getTournament().getName())
                .communityId(b.getCommunityId())
                .totalPlannedAmount(b.getTotalPlannedAmount())
                .totalCollectedAmount(collected)
                .variance(variance)
                .currency(b.getCurrency())
                .notes(b.getNotes())
                .lineItems(items)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
