package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.model.entity.*;
import com.manacommunity.api.homeservice.model.enums.HomeServiceVerificationStatus;
import com.manacommunity.api.homeservice.model.enums.HomeServiceWorkerType;
import com.manacommunity.api.homeservice.repository.*;
import com.manacommunity.api.homeservice.exception.HomeServiceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service("homeServiceWorkerService")
@RequiredArgsConstructor
public class HomeServiceWorkerService {
    private final HomeServiceWorkerRepository workerRepository;
    private final WorkerServiceSkillRepository skillRepository;
    private final WorkerAvailabilitySlotRepository slotRepository;
    private final WorkerFlatAssignmentRepository assignmentRepository;
    private final HomeServiceCategoryRepository categoryRepository;

    @PostConstruct
    @Transactional
    public void initDefaultWorkers() {
        if (workerRepository.count() > 0) return;
        log.info("[HomeService] Seeding default verified community workers");

        seedCategory("cat-plumb", "PLUMBING", "Plumbing", "water");
        seedCategory("cat-elec", "ELECTRICAL", "Electrical", "flash");
        seedCategory("cat-clean", "CLEANING", "Cleaning", "sparkles");
        seedCategory("cat-app", "APPLIANCE", "Appliance", "tv");
        seedCategory("cat-carp", "CARPENTRY", "Carpentry", "hammer");
        seedCategory("cat-paint", "PAINTING", "Painting", "color-palette");
        seedCategory("cat-maid", "MAID", "Maid & Cook", "people");
        seedCategory("cat-pest", "PEST_CONTROL", "Pest Control", "bug");

        seedWorker("worker-1", "Raju Sharma (Master Plumber)", "PLUMBING", "+919876543210", new BigDecimal("4.8"), 54, 9, "₹200 - ₹500", "TOP RATED", "Pipe leaks, taps, bathroom fittings, flush tanks");
        seedWorker("worker-2", "Spark Electrical Works (Vinod)", "ELECTRICAL", "+919876543211", new BigDecimal("4.7"), 62, 12, "₹250 - ₹800", "FAST RESPONSE", "MCB tripping, fan, chandelier, geyser & smart switches");
        seedWorker("worker-3", "Urban CleanPro Team", "CLEANING", "+919876543212", new BigDecimal("4.9"), 88, 6, "₹600 - ₹2,200", "POPULAR", "Deep kitchen, bathroom scrub, sofa & balcony cleaning");
        seedWorker("worker-4", "QuickFix AC & Appliances", "APPLIANCE", "+919876543215", new BigDecimal("4.6"), 39, 8, "₹350 - ₹1,200", null, "AC gas refill, filter cleaning, fridge, washing machine");
        seedWorker("worker-5", "Kumar Wooden Craft", "CARPENTRY", "+919876543213", new BigDecimal("4.4"), 28, 15, "₹300 - ₹1,500", null, "Door locks, hinge fixing, modular kitchen & wardrobe work");
        seedWorker("worker-6", "Sunil Wall Paints & Textures", "PAINTING", "+919876543214", new BigDecimal("4.5"), 31, 10, "₹18 - ₹28/sqft", null, "Interior touch-up, waterproof coating, balcony paint");
        seedWorker("worker-7", "Laxmi Domestic Services", "MAID", "+919876543216", new BigDecimal("4.8"), 47, 5, "₹3,000 - ₹7,000/mo", "RECOMMENDED", "Home cooking (North & South Indian), housekeeping, utensil wash");
        seedWorker("worker-8", "Shield Pest Solutions", "PEST_CONTROL", "+919876543217", new BigDecimal("4.7"), 26, 7, "₹750 - ₹1,800", null, "Cockroach herbal gel, termite, mosquito & rodent control");
    }

    private void seedCategory(String id, String code, String name, String icon) {
        if (categoryRepository.findByCode(code).isPresent()) return;
        categoryRepository.save(HomeServiceCategoryEntity.builder()
                .id(id)
                .code(code)
                .name(name)
                .icon(icon)
                .active(true)
                .displayOrder(1)
                .build());
    }

    private void seedWorker(String id, String name, String category, String phone, BigDecimal rating, int reviews, int exp, String priceRange, String badge, String spec) {
        HomeServiceWorkerEntity worker = HomeServiceWorkerEntity.builder()
                .id(id)
                .communityId("comm-mana-1")
                .workerType(HomeServiceWorkerType.COMMUNITY_WORKER)
                .displayName(name)
                .primaryPhone(phone)
                .verificationStatus(HomeServiceVerificationStatus.VERIFIED)
                .policeVerified(true)
                .communityVerified(true)
                .securityVerified(true)
                .rating(rating)
                .totalReviews(reviews)
                .experienceYears(exp)
                .active(true)
                .build();
        workerRepository.save(worker);

        skillRepository.save(WorkerServiceSkillEntity.builder()
                .id(UUID.randomUUID().toString())
                .workerId(id)
                .categoryId(category)
                .isPrimary(true)
                .experienceYears(exp)
                .basePriceMonthly(new BigDecimal("3000.00"))
                .basePricePerVisit(new BigDecimal("350.00"))
                .build());
    }

    public List<HomeServiceWorkerEntity> getWorkers(String communityId, String category, BigDecimal minRating, HomeServiceVerificationStatus status) {
        List<HomeServiceWorkerEntity> workers = workerRepository.searchWorkers(communityId, minRating, status);

        // Enrich with primary skill / category
        for (HomeServiceWorkerEntity w : workers) {
            List<WorkerServiceSkillEntity> skills = skillRepository.findByWorkerId(w.getId());
            if (!skills.isEmpty()) {
                w.setCategory(skills.get(0).getCategoryId());
            } else {
                w.setCategory("PLUMBING");
            }
            w.setPriceRange(w.getPriceRange() != null ? w.getPriceRange() : "₹250 - ₹750");
            w.setStatusText(Boolean.TRUE.equals(w.getActive()) ? "Available Today" : "Busy");
            if (w.getRating() != null && w.getRating().compareTo(new BigDecimal("4.8")) >= 0) {
                w.setBadge("TOP RATED");
            }
        }

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            return workers.stream()
                    .filter(w -> category.equalsIgnoreCase(w.getCategory()))
                    .toList();
        }

        return workers;
    }

    public HomeServiceWorkerEntity getWorkerById(String workerId) {
        HomeServiceWorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new HomeServiceNotFoundException("Worker not found: " + workerId));
        List<WorkerServiceSkillEntity> skills = skillRepository.findByWorkerId(worker.getId());
        if (!skills.isEmpty()) {
            worker.setCategory(skills.get(0).getCategoryId());
        }
        return worker;
    }

    public List<WorkerServiceSkillEntity> getWorkerSkills(String workerId) {
        return skillRepository.findByWorkerId(workerId);
    }

    public List<WorkerAvailabilitySlotEntity> getWorkerSlots(String workerId) {
        return slotRepository.findByWorkerId(workerId);
    }

    public List<WorkerFlatAssignmentEntity> getWorkerFlats(String workerId) {
        return assignmentRepository.findByWorkerIdAndActiveTrue(workerId);
    }

    @Transactional
    public HomeServiceWorkerEntity registerWorker(HomeServiceWorkerEntity worker) {
        if (worker.getId() == null) {
            worker.setId(UUID.randomUUID().toString());
        }
        worker.setCreatedAt(LocalDateTime.now());
        worker.setUpdatedAt(LocalDateTime.now());
        return workerRepository.save(worker);
    }

    @Transactional
    public HomeServiceWorkerEntity updateVerification(String workerId, HomeServiceVerificationStatus status, boolean policeVerified, boolean communityVerified) {
        HomeServiceWorkerEntity worker = getWorkerById(workerId);
        worker.setVerificationStatus(status);
        worker.setPoliceVerified(policeVerified);
        worker.setCommunityVerified(communityVerified);
        worker.setSecurityVerified(true);
        worker.setUpdatedAt(LocalDateTime.now());
        return workerRepository.save(worker);
    }
}
