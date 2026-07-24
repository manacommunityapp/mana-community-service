package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodCorporateAccount;
import com.manacommunity.api.food.entity.FoodCorporateCafeteria;
import com.manacommunity.api.food.entity.FoodCorporateCafeteriaMenu;
import com.manacommunity.api.food.entity.FoodCorporateCateringRequest;
import com.manacommunity.api.food.entity.FoodCorporateMealCard;
import com.manacommunity.api.food.entity.FoodCorporateMealCardTransaction;
import com.manacommunity.api.food.repository.FoodCorporateAccountRepository;
import com.manacommunity.api.food.repository.FoodCorporateCafeteriaMenuRepository;
import com.manacommunity.api.food.repository.FoodCorporateCafeteriaRepository;
import com.manacommunity.api.food.repository.FoodCorporateCateringRequestRepository;
import com.manacommunity.api.food.repository.FoodCorporateMealCardRepository;
import com.manacommunity.api.food.repository.FoodCorporateMealCardTransactionRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodCorporateService {

    private final FoodCorporateAccountRepository accountRepo;
    private final FoodCorporateMealCardRepository mealCardRepo;
    private final FoodCorporateMealCardTransactionRepository transactionRepo;
    private final FoodCorporateCafeteriaRepository cafeteriaRepo;
    private final FoodCorporateCafeteriaMenuRepository cafeteriaMenuRepo;
    private final FoodCorporateCateringRequestRepository cateringRequestRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAccounts(Long communityId) {
        List<FoodCorporateAccount> accounts = accountRepo.findByCommunityId(communityId);
        return accounts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createAccount(Map<String, Object> request, Long communityId) {
        Community community = new Community();
        community.setId(communityId);

        FoodCorporateAccount account = FoodCorporateAccount.builder()
                .companyName((String) request.get("companyName"))
                .gstNumber((String) request.get("gstNumber"))
                .contactPerson((String) request.get("contactPerson"))
                .contactEmail((String) request.get("contactEmail"))
                .contactPhone((String) request.get("contactPhone"))
                .billingAddress((String) request.get("billingAddress"))
                .creditLimit(request.containsKey("creditLimit")
                        ? new BigDecimal(request.get("creditLimit").toString()) : null)
                .status(FoodCorporateAccount.AccountStatus.ACTIVE)
                .community(community)
                .build();

        FoodCorporateAccount saved = accountRepo.save(account);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> issueMealCard(Map<String, Object> request, Long communityId) {
        Long accountId = Long.valueOf(request.get("accountId").toString());
        FoodCorporateAccount account = accountRepo.findByIdAndCommunityId(accountId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCorporateAccount", accountId));

        Community community = new Community();
        community.setId(communityId);

        AppUser user = new AppUser();
        user.setId(Long.valueOf(request.get("userId").toString()));

        String cardNumber = "MC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FoodCorporateMealCard card = FoodCorporateMealCard.builder()
                .account(account)
                .user(user)
                .cardNumber(cardNumber)
                .balance(request.containsKey("balance")
                        ? new BigDecimal(request.get("balance").toString()) : BigDecimal.ZERO)
                .dailyLimit(request.containsKey("dailyLimit")
                        ? new BigDecimal(request.get("dailyLimit").toString()) : null)
                .monthlyLimit(request.containsKey("monthlyLimit")
                        ? new BigDecimal(request.get("monthlyLimit").toString()) : null)
                .status(FoodCorporateMealCard.CardStatus.ACTIVE)
                .validFrom(LocalDate.now())
                .validUntil(request.containsKey("validUntil")
                        ? LocalDate.parse((String) request.get("validUntil")) : LocalDate.now().plusYears(1))
                .community(community)
                .build();

        FoodCorporateMealCard saved = mealCardRepo.save(card);
        return toMealCardResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMealCards(Long accountId) {
        List<FoodCorporateMealCard> cards = mealCardRepo.findByAccountId(accountId);
        return cards.stream().map(this::toMealCardResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getCardTransactions(Long cardId, Pageable pageable) {
        Page<FoodCorporateMealCardTransaction> transactions = transactionRepo.findByCardId(cardId, pageable);
        return transactions.map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("cardId", t.getCard().getId());
            map.put("transactionType", t.getTransactionType().name());
            map.put("amount", t.getAmount());
            map.put("orderId", t.getOrderId());
            map.put("balanceAfter", t.getBalanceAfter());
            map.put("description", t.getDescription());
            map.put("createdAt", t.getCreatedAt());
            return map;
        });
    }

    @Transactional
    public Map<String, Object> debitMealCard(Long cardId, BigDecimal amount, Long orderId, String description) {
        FoodCorporateMealCard card = mealCardRepo.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCorporateMealCard", cardId));

        BigDecimal newBalance = card.getBalance().subtract(amount);
        card.setBalance(newBalance);
        mealCardRepo.save(card);

        FoodCorporateMealCardTransaction transaction = FoodCorporateMealCardTransaction.builder()
                .card(card)
                .transactionType(FoodCorporateMealCardTransaction.CardTransactionType.DEBIT)
                .amount(amount)
                .orderId(orderId)
                .balanceAfter(newBalance)
                .description(description)
                .build();

        FoodCorporateMealCardTransaction saved = transactionRepo.save(transaction);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("cardId", saved.getCard().getId());
        map.put("transactionType", saved.getTransactionType().name());
        map.put("amount", saved.getAmount());
        map.put("orderId", saved.getOrderId());
        map.put("balanceAfter", saved.getBalanceAfter());
        map.put("description", saved.getDescription());
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCafeterias(Long accountId) {
        List<FoodCorporateCafeteria> cafeterias = cafeteriaRepo.findByAccountIdAndStatus(
                accountId, FoodCorporateCafeteria.CafeteriaStatus.ACTIVE.name());
        return cafeterias.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("accountId", c.getAccount().getId());
            map.put("name", c.getName());
            map.put("location", c.getLocation());
            map.put("capacity", c.getCapacity());
            map.put("status", c.getStatus().name());
            map.put("communityId", c.getCommunity().getId());
            map.put("createdAt", c.getCreatedAt());
            map.put("updatedAt", c.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createCafeteria(Map<String, Object> request, Long communityId) {
        Long accountId = Long.valueOf(request.get("accountId").toString());
        FoodCorporateAccount account = accountRepo.findByIdAndCommunityId(accountId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCorporateAccount", accountId));

        Community community = new Community();
        community.setId(communityId);

        FoodCorporateCafeteria cafeteria = FoodCorporateCafeteria.builder()
                .account(account)
                .name((String) request.get("name"))
                .location((String) request.get("location"))
                .capacity(request.containsKey("capacity") ? (Integer) request.get("capacity") : null)
                .status(FoodCorporateCafeteria.CafeteriaStatus.ACTIVE)
                .community(community)
                .build();

        FoodCorporateCafeteria saved = cafeteriaRepo.save(cafeteria);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("accountId", saved.getAccount().getId());
        map.put("name", saved.getName());
        map.put("location", saved.getLocation());
        map.put("capacity", saved.getCapacity());
        map.put("status", saved.getStatus().name());
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCafeteriaMenu(Long cafeteriaId, LocalDate date) {
        List<FoodCorporateCafeteriaMenu> menus = cafeteriaMenuRepo.findByCafeteriaIdAndDate(cafeteriaId, date);
        return menus.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("cafeteriaId", m.getCafeteria().getId());
            map.put("date", m.getDate());
            map.put("mealType", m.getMealType());
            map.put("items", m.getItems());
            map.put("price", m.getPrice());
            map.put("totalPlates", m.getTotalPlates());
            map.put("bookedPlates", m.getBookedPlates());
            map.put("communityId", m.getCommunity().getId());
            map.put("createdAt", m.getCreatedAt());
            map.put("updatedAt", m.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createCateringRequest(Map<String, Object> request, AppUser user, Community community) {
        Long accountId = Long.valueOf(request.get("accountId").toString());
        FoodCorporateAccount account = accountRepo.findByIdAndCommunityId(accountId, community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FoodCorporateAccount", accountId));

        FoodCorporateCateringRequest cateringRequest = FoodCorporateCateringRequest.builder()
                .account(account)
                .requestedBy(user)
                .eventName((String) request.get("eventName"))
                .eventDate(request.containsKey("eventDate") ? LocalDate.parse((String) request.get("eventDate")) : null)
                .guestCount(request.containsKey("guestCount") ? (Integer) request.get("guestCount") : null)
                .budget(request.containsKey("budget") ? new BigDecimal(request.get("budget").toString()) : null)
                .menuPreferences((String) request.get("menuPreferences"))
                .dietaryRequirements((String) request.get("dietaryRequirements"))
                .venue((String) request.get("venue"))
                .status(FoodCorporateCateringRequest.CorporateCateringStatus.PENDING)
                .notes((String) request.get("notes"))
                .community(community)
                .build();

        FoodCorporateCateringRequest saved = cateringRequestRepo.save(cateringRequest);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("accountId", saved.getAccount().getId());
        map.put("requestedById", saved.getRequestedBy().getId());
        map.put("eventName", saved.getEventName());
        map.put("eventDate", saved.getEventDate());
        map.put("guestCount", saved.getGuestCount());
        map.put("budget", saved.getBudget());
        map.put("menuPreferences", saved.getMenuPreferences());
        map.put("dietaryRequirements", saved.getDietaryRequirements());
        map.put("venue", saved.getVenue());
        map.put("status", saved.getStatus().name());
        map.put("notes", saved.getNotes());
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodCorporateAccount account) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", account.getId());
        map.put("companyName", account.getCompanyName());
        map.put("gstNumber", account.getGstNumber());
        map.put("contactPerson", account.getContactPerson());
        map.put("contactEmail", account.getContactEmail());
        map.put("contactPhone", account.getContactPhone());
        map.put("billingAddress", account.getBillingAddress());
        map.put("creditLimit", account.getCreditLimit());
        map.put("balance", account.getBalance());
        map.put("status", account.getStatus() != null ? account.getStatus().name() : null);
        map.put("communityId", account.getCommunity().getId());
        map.put("createdAt", account.getCreatedAt());
        map.put("updatedAt", account.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toMealCardResponse(FoodCorporateMealCard card) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", card.getId());
        map.put("accountId", card.getAccount().getId());
        map.put("userId", card.getUser().getId());
        map.put("cardNumber", card.getCardNumber());
        map.put("balance", card.getBalance());
        map.put("dailyLimit", card.getDailyLimit());
        map.put("monthlyLimit", card.getMonthlyLimit());
        map.put("status", card.getStatus() != null ? card.getStatus().name() : null);
        map.put("validFrom", card.getValidFrom());
        map.put("validUntil", card.getValidUntil());
        map.put("communityId", card.getCommunity().getId());
        map.put("createdAt", card.getCreatedAt());
        map.put("updatedAt", card.getUpdatedAt());
        return map;
    }
}
