package com.manacommunity.api.controller;

import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/procurement-hub")
public class ProcurementHubController {

    private final SupplierProfileRepository supplierRepo;
    private final SupplierProductRepository productRepo;
    private final ProcurementRFQRepository rfqRepo;
    private final SupplierQuoteRepository quoteRepo;
    private final ProcurementPORepository poRepo;
    private final BulkBuyCampaignRepository campaignRepo;
    private final BulkBuyCommitmentRepository commitmentRepo;
    private final AuctionBidRepository auctionBidRepo;
    private final LoggedInUserService loggedInUserService;

    public ProcurementHubController(
            SupplierProfileRepository supplierRepo,
            SupplierProductRepository productRepo,
            ProcurementRFQRepository rfqRepo,
            SupplierQuoteRepository quoteRepo,
            ProcurementPORepository poRepo,
            BulkBuyCampaignRepository campaignRepo,
            BulkBuyCommitmentRepository commitmentRepo,
            AuctionBidRepository auctionBidRepo,
            LoggedInUserService loggedInUserService) {
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
        this.rfqRepo = rfqRepo;
        this.quoteRepo = quoteRepo;
        this.poRepo = poRepo;
        this.campaignRepo = campaignRepo;
        this.commitmentRepo = commitmentRepo;
        this.auctionBidRepo = auctionBidRepo;
        this.loggedInUserService = loggedInUserService;
    }

    // ── Supplier Marketplace ──────────────────────────────────────────────

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierProfile>> getSuppliers(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean verified) {

        List<SupplierProfile> suppliers;
        if (city != null || search != null) {
            suppliers = supplierRepo.search(city, search);
        } else if (Boolean.TRUE.equals(verified)) {
            suppliers = supplierRepo.findByActiveTrueAndVerificationStatus(SupplierVerificationStatus.VERIFIED);
        } else {
            suppliers = supplierRepo.findByActiveTrue();
        }

        if (category != null) {
            try {
                ProcurementCategory cat = ProcurementCategory.valueOf(category);
                suppliers = suppliers.stream()
                        .filter(s -> s.getCategories() != null && s.getCategories().contains(cat.name()))
                        .toList();
            } catch (IllegalArgumentException ignored) {}
        }

        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierProfile> getSupplier(@PathVariable Long id) {
        return supplierRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/suppliers/{id}/products")
    public ResponseEntity<List<SupplierProduct>> getSupplierProducts(
            @PathVariable Long id,
            @RequestParam(required = false) String category) {

        List<SupplierProduct> products;
        if (category != null) {
            try {
                ProcurementCategory cat = ProcurementCategory.valueOf(category);
                products = productRepo.findBySupplierIdAndCategoryAndActiveTrue(id, cat);
            } catch (IllegalArgumentException e) {
                products = productRepo.findBySupplierIdAndActiveTrue(id);
            }
        } else {
            products = productRepo.findBySupplierIdAndActiveTrue(id);
        }

        return ResponseEntity.ok(products);
    }

    @GetMapping("/products")
    public ResponseEntity<List<SupplierProduct>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        ProcurementCategory cat = null;
        if (category != null) {
            try { cat = ProcurementCategory.valueOf(category); } catch (IllegalArgumentException ignored) {}
        }

        return ResponseEntity.ok(productRepo.search(cat, search));
    }

    // ── RFQ Management ────────────────────────────────────────────────────

    @GetMapping("/rfqs")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<ProcurementRFQ>> getRFQs(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        List<ProcurementRFQ> rfqs;

        if (status != null) {
            try {
                RFQStatus s = RFQStatus.valueOf(status);
                rfqs = rfqRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, s);
            } catch (IllegalArgumentException e) {
                rfqs = rfqRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
            }
        } else {
            rfqs = rfqRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }

        return ResponseEntity.ok(rfqs);
    }

    @GetMapping("/rfqs/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementRFQ> getRFQ(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return rfqRepo.findByIdAndCommunityId(id, communityId(principal))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rfqs")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementRFQ> createRFQ(
            @RequestBody RFQRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = communityId(principal);

        String rfqNumber = "RFQ-" + communityId + "-" + System.currentTimeMillis() % 100000;

        ProcurementRFQ rfq = ProcurementRFQ.builder()
                .rfqNumber(rfqNumber)
                .title(req.title())
                .description(req.description())
                .status(RFQStatus.DRAFT)
                .communityId(communityId)
                .requestedBy(user.getName() != null ? user.getName() : user.getEmail())
                .deliveryLocation(req.deliveryLocation())
                .deliveryDeadline(java.time.LocalDate.parse(req.deliveryDeadline()))
                .quotationDeadline(java.time.LocalDate.parse(req.quotationDeadline()))
                .estimatedValue(req.estimatedValue())
                .approvedBudget(req.approvedBudget())
                .notes(req.notes())
                .build();

        ProcurementRFQ saved = rfqRepo.save(rfq);

        if (req.items() != null) {
            for (RFQItemRequest itemReq : req.items()) {
                ProcurementCategory cat = ProcurementCategory.OTHER;
                try { cat = ProcurementCategory.valueOf(itemReq.category()); } catch (Exception ignored) {}

                RFQItem item = RFQItem.builder()
                        .rfq(saved)
                        .productName(itemReq.productName())
                        .description(itemReq.description())
                        .category(cat)
                        .unit(itemReq.unit() != null ? itemReq.unit() : "units")
                        .quantity(itemReq.quantity())
                        .targetPricePerUnit(itemReq.targetPricePerUnit())
                        .specifications(itemReq.specifications())
                        .build();
                saved.getItems().add(item);
            }
            saved = rfqRepo.save(saved);
        }

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/rfqs/{id}/publish")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementRFQ> publishRFQ(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return rfqRepo.findByIdAndCommunityId(id, communityId(principal))
                .map(rfq -> {
                    rfq.setStatus(RFQStatus.OPEN);
                    return ResponseEntity.ok(rfqRepo.save(rfq));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rfqs/{id}/cancel")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementRFQ> cancelRFQ(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        return rfqRepo.findByIdAndCommunityId(id, communityId(principal))
                .map(rfq -> {
                    rfq.setStatus(RFQStatus.CANCELLED);
                    return ResponseEntity.ok(rfqRepo.save(rfq));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Quotes ────────────────────────────────────────────────────────────

    @GetMapping("/rfqs/{rfqId}/quotes")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<SupplierQuoteDto>> getRFQQuotes(
            @PathVariable Long rfqId,
            @AuthenticationPrincipal UserPrincipal principal) {

        rfqRepo.findByIdAndCommunityId(rfqId, communityId(principal))
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        List<SupplierQuote> quotes = quoteRepo.findByRfqIdOrderByGrandTotalAsc(rfqId);
        List<SupplierQuoteDto> dtos = quotes.stream().map(this::toQuoteDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/rfqs/{rfqId}/award/{quoteId}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementPODto> awardQuote(
            @PathVariable Long rfqId,
            @PathVariable Long quoteId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);

        ProcurementRFQ rfq = rfqRepo.findByIdAndCommunityId(rfqId, communityId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        SupplierQuote quote = quoteRepo.findByIdAndRfqId(quoteId, rfqId)
                .orElseThrow(() -> new RuntimeException("Quote not found"));

        // Mark other quotes as rejected
        quoteRepo.findByRfqIdOrderByGrandTotalAsc(rfqId).forEach(q -> {
            if (!q.getId().equals(quoteId)) {
                q.setStatus(QuoteStatus.REJECTED);
                quoteRepo.save(q);
            }
        });

        quote.setStatus(QuoteStatus.AWARDED);
        quoteRepo.save(quote);

        rfq.setStatus(RFQStatus.AWARDED);
        rfqRepo.save(rfq);

        String poNumber = "PO-" + communityId + "-" + System.currentTimeMillis() % 100000;

        ProcurementPO po = ProcurementPO.builder()
                .poNumber(poNumber)
                .rfqId(rfqId)
                .rfqNumber(rfq.getRfqNumber())
                .supplier(quote.getSupplier())
                .communityId(communityId)
                .status(POStatus.SENT)
                .subTotal(quote.getSubTotal())
                .gstAmount(quote.getGstAmount())
                .deliveryCharge(quote.getDeliveryCharge())
                .grandTotal(quote.getGrandTotal())
                .deliveryDeadline(quote.getDeliveryDate())
                .deliveryAddress(rfq.getDeliveryLocation())
                .paymentTerms(quote.getPaymentTerms())
                .notes(quote.getNotes())
                .build();

        for (SupplierQuoteItem qi : quote.getItems()) {
            POItem poItem = POItem.builder()
                    .purchaseOrder(po)
                    .productName(qi.getProductName())
                    .unit(qi.getUnit())
                    .quantity(qi.getQuantity())
                    .pricePerUnit(qi.getPricePerUnit())
                    .gstPercent(qi.getGstPercent())
                    .totalPrice(qi.getTotalPrice())
                    .leadTimeDays(qi.getLeadTimeDays())
                    .notes(qi.getNotes())
                    .build();
            po.getItems().add(poItem);
        }

        ProcurementPO savedPO = poRepo.save(po);
        return ResponseEntity.ok(toPODto(savedPO));
    }

    // ── Purchase Orders ───────────────────────────────────────────────────

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<ProcurementPODto>> getPurchaseOrders(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        List<ProcurementPO> pos;

        if (status != null) {
            try {
                POStatus s = POStatus.valueOf(status);
                pos = poRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, s);
            } catch (IllegalArgumentException e) {
                pos = poRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
            }
        } else {
            pos = poRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }

        return ResponseEntity.ok(pos.stream().map(this::toPODto).toList());
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementPODto> getPurchaseOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return poRepo.findByIdAndCommunityId(id, communityId(principal))
                .map(po -> ResponseEntity.ok(toPODto(po)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/purchase-orders/{id}/status")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementPODto> updatePOStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        return poRepo.findByIdAndCommunityId(id, communityId(principal))
                .map(po -> {
                    String statusStr = body.get("status");
                    if (statusStr != null) {
                        try { po.setStatus(POStatus.valueOf(statusStr)); } catch (IllegalArgumentException ignored) {}
                    }
                    return ResponseEntity.ok(toPODto(poRepo.save(po)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    // ── Phase 2: Supplier Management ──────────────────────────────────────────

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<SupplierProfile> createSupplier(@RequestBody SupplierRequest req) {
        SupplierProfile supplier = SupplierProfile.builder()
                .businessName(req.businessName())
                .contactPerson(req.contactPerson())
                .mobile(req.mobile())
                .email(req.email())
                .gstNumber(req.gstNumber())
                .address(req.address())
                .city(req.city())
                .pincode(req.pincode())
                .categories(req.categories())
                .serviceArea(req.serviceArea())
                .description(req.description())
                .verificationStatus(SupplierVerificationStatus.VERIFIED)
                .rating(java.math.BigDecimal.ZERO)
                .reviewCount(0)
                .fulfillmentRate(java.math.BigDecimal.valueOf(100))
                .onTimeRate(java.math.BigDecimal.valueOf(100))
                .totalOrders(0)
                .active(true)
                .build();
        return ResponseEntity.ok(supplierRepo.save(supplier));
    }

    @PutMapping("/suppliers/{id}/verify")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<SupplierProfile> verifySupplier(@PathVariable Long id) {
        return supplierRepo.findById(id)
                .map(s -> {
                    s.setVerificationStatus(SupplierVerificationStatus.VERIFIED);
                    return ResponseEntity.ok(supplierRepo.save(s));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/suppliers/{id}/products")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<SupplierProduct> addSupplierProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest req) {

        SupplierProfile supplier = supplierRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        ProcurementCategory cat = ProcurementCategory.OTHER;
        try { cat = ProcurementCategory.valueOf(req.category()); } catch (Exception ignored) {}

        SupplierProduct product = SupplierProduct.builder()
                .supplier(supplier)
                .name(req.name())
                .description(req.description())
                .category(cat)
                .unit(req.unit() != null ? req.unit() : "units")
                .wholesalePrice(req.wholesalePrice())
                .retailPrice(req.retailPrice())
                .moq(req.moq() != null ? req.moq() : 1)
                .gstPercent(req.gstPercent() != null ? req.gstPercent() : java.math.BigDecimal.valueOf(18))
                .leadTimeDays(req.leadTimeDays() != null ? req.leadTimeDays() : 3)
                .inStock(true)
                .active(true)
                .build();
        return ResponseEntity.ok(productRepo.save(product));
    }

    // ── Phase 2: Quote Submission ──────────────────────────────────────────────

    @PostMapping("/rfqs/{rfqId}/quotes")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<SupplierQuoteDto> submitQuote(
            @PathVariable Long rfqId,
            @RequestBody QuoteSubmitRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProcurementRFQ rfq = rfqRepo.findByIdAndCommunityId(rfqId, communityId(principal))
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        SupplierProfile supplier = supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        java.math.BigDecimal subTotal = req.items().stream()
                .map(i -> i.pricePerUnit().multiply(java.math.BigDecimal.valueOf(i.quantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal gstAmount = req.items().stream()
                .map(i -> {
                    java.math.BigDecimal lineTotal = i.pricePerUnit().multiply(java.math.BigDecimal.valueOf(i.quantity()));
                    java.math.BigDecimal gstPct = i.gstPercent() != null ? i.gstPercent() : java.math.BigDecimal.valueOf(18);
                    return lineTotal.multiply(gstPct).divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                })
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal deliveryCharge = req.deliveryCharge() != null ? req.deliveryCharge() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal grandTotal = subTotal.add(gstAmount).add(deliveryCharge);

        SupplierQuote quote = SupplierQuote.builder()
                .rfq(rfq)
                .supplier(supplier)
                .status(QuoteStatus.SUBMITTED)
                .subTotal(subTotal)
                .gstAmount(gstAmount)
                .deliveryCharge(deliveryCharge)
                .grandTotal(grandTotal)
                .validUntil(java.time.LocalDate.parse(req.validUntil()))
                .deliveryDate(java.time.LocalDate.parse(req.deliveryDate()))
                .paymentTerms(req.paymentTerms())
                .notes(req.notes())
                .build();

        for (QuoteItemRequest itemReq : req.items()) {
            java.math.BigDecimal lineTotal = itemReq.pricePerUnit().multiply(java.math.BigDecimal.valueOf(itemReq.quantity()));
            SupplierQuoteItem item = SupplierQuoteItem.builder()
                    .quote(quote)
                    .rfqItemId(itemReq.rfqItemId())
                    .productName(itemReq.productName())
                    .unit(itemReq.unit() != null ? itemReq.unit() : "units")
                    .quantity(itemReq.quantity())
                    .pricePerUnit(itemReq.pricePerUnit())
                    .gstPercent(itemReq.gstPercent() != null ? itemReq.gstPercent() : java.math.BigDecimal.valueOf(18))
                    .totalPrice(lineTotal)
                    .leadTimeDays(itemReq.leadTimeDays() != null ? itemReq.leadTimeDays() : 3)
                    .notes(itemReq.notes())
                    .build();
            quote.getItems().add(item);
        }

        SupplierQuote saved = quoteRepo.save(quote);
        rfq.setQuotesReceived(rfq.getQuotesReceived() + 1);
        if (rfq.getStatus() == RFQStatus.OPEN) {
            rfq.setStatus(RFQStatus.QUOTES_RECEIVED);
        }
        rfqRepo.save(rfq);

        return ResponseEntity.ok(toQuoteDto(saved));
    }

    // ── Phase 2: Analytics ────────────────────────────────────────────────────

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementAnalyticsDto> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);

        List<ProcurementPO> allPOs = poRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        java.math.BigDecimal totalSpend = poRepo.sumFulfilledSpend(communityId);
        if (totalSpend == null) totalSpend = java.math.BigDecimal.ZERO;

        java.math.BigDecimal pendingPOValue = allPOs.stream()
                .filter(po -> po.getStatus() == POStatus.SENT || po.getStatus() == POStatus.ACKNOWLEDGED)
                .map(ProcurementPO::getGrandTotal)
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, long[]> catMap = new java.util.LinkedHashMap<>();
        for (ProcurementPO po : allPOs) {
            if (po.getStatus() == POStatus.CANCELLED) continue;
            String categories = po.getSupplier().getCategories();
            String cat = "OTHER";
            if (categories != null && !categories.isBlank()) {
                cat = categories.split(",")[0].trim();
            }
            catMap.computeIfAbsent(cat, k -> new long[]{0L, 0L});
            catMap.get(cat)[0] += po.getGrandTotal() != null ? po.getGrandTotal().longValue() : 0L;
            catMap.get(cat)[1]++;
        }
        List<CategorySpendDto> categorySpend = catMap.entrySet().stream()
                .map(e -> new CategorySpendDto(e.getKey(), e.getValue()[0], (int) e.getValue()[1]))
                .sorted((a, b) -> Long.compare(b.spend(), a.spend()))
                .toList();

        Map<Long, long[]> supplierMap = new java.util.LinkedHashMap<>();
        Map<Long, String> supplierNames = new java.util.HashMap<>();
        for (ProcurementPO po : allPOs) {
            if (po.getStatus() == POStatus.CANCELLED) continue;
            Long sid = po.getSupplier().getId();
            supplierMap.computeIfAbsent(sid, k -> new long[]{0L, 0L});
            supplierMap.get(sid)[0] += po.getGrandTotal() != null ? po.getGrandTotal().longValue() : 0L;
            supplierMap.get(sid)[1]++;
            supplierNames.put(sid, po.getSupplier().getBusinessName());
        }
        List<SupplierSpendDto> topSuppliers = supplierMap.entrySet().stream()
                .map(e -> new SupplierSpendDto(e.getKey(), supplierNames.get(e.getKey()), e.getValue()[0], (int) e.getValue()[1]))
                .sorted((a, b) -> Long.compare(b.spend(), a.spend()))
                .limit(5)
                .toList();

        List<ProcurementRFQ> allRFQs = rfqRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        int awardedRFQs = (int) allRFQs.stream()
                .filter(r -> r.getStatus() == RFQStatus.AWARDED || r.getStatus() == RFQStatus.CLOSED)
                .count();
        double avgQuotes = allRFQs.isEmpty() ? 0
                : allRFQs.stream().mapToInt(ProcurementRFQ::getQuotesReceived).average().orElse(0);

        java.math.BigDecimal totalEstimated = allRFQs.stream()
                .filter(r -> r.getStatus() == RFQStatus.AWARDED || r.getStatus() == RFQStatus.CLOSED)
                .map(ProcurementRFQ::getEstimatedValue)
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long savings = Math.max(0L, totalEstimated.longValue() - totalSpend.longValue());
        double savingsRate = totalEstimated.compareTo(java.math.BigDecimal.ZERO) > 0
                ? Math.round((savings * 1000.0 / totalEstimated.longValue())) / 10.0
                : 0.0;

        return ResponseEntity.ok(new ProcurementAnalyticsDto(
                totalSpend.longValue(), savings, savingsRate,
                categorySpend, topSuppliers,
                allRFQs.size(), awardedRFQs,
                Math.round(avgQuotes * 10.0) / 10.0,
                pendingPOValue.longValue()
        ));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ProcurementStatsDto> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        long totalRFQs = rfqRepo.findByCommunityIdOrderByCreatedAtDesc(communityId).size();
        long activeRFQs = rfqRepo.countByCommunityIdAndStatusIn(communityId,
                List.of(RFQStatus.OPEN, RFQStatus.QUOTES_RECEIVED, RFQStatus.COMPARING));
        long totalPOs = poRepo.countByCommunityId(communityId);
        BigDecimal totalSpend = poRepo.sumFulfilledSpend(communityId);
        long activeSuppliers = supplierRepo.countVerifiedSuppliers();

        List<ProcurementRFQ> rfqs = rfqRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        double avgQuotes = rfqs.isEmpty() ? 0 :
                rfqs.stream().mapToInt(ProcurementRFQ::getQuotesReceived).average().orElse(0);

        ProcurementStatsDto dto = new ProcurementStatsDto(
                (int) totalRFQs, (int) activeRFQs, (int) totalPOs,
                totalSpend != null ? totalSpend.longValue() : 0L,
                0L,
                (int) activeSuppliers,
                Math.round(avgQuotes * 10.0) / 10.0
        );

        return ResponseEntity.ok(dto);
    }

    // ── BulkBuy Campaigns ─────────────────────────────────────────────────

    @GetMapping("/bulk-buy/campaigns")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BulkBuyCampaignDto>> getCampaigns(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        Long userId = userId(principal);

        List<BulkBuyCampaign> campaigns;
        if (status != null) {
            try {
                BulkBuyCampaignStatus s = BulkBuyCampaignStatus.valueOf(status);
                campaigns = campaignRepo.findByCommunityIdAndStatusOrderByDeadlineDateAsc(communityId, s);
            } catch (IllegalArgumentException e) {
                campaigns = campaignRepo.findByCommunityIdOrderByDeadlineDateAsc(communityId);
            }
        } else {
            campaigns = campaignRepo.findByCommunityIdOrderByDeadlineDateAsc(communityId);
        }

        List<BulkBuyCampaignDto> dtos = campaigns.stream()
                .map(c -> toCampaignDto(c, userId))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/bulk-buy/campaigns/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BulkBuyCampaignDto> getCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        Long userId = userId(principal);

        return campaignRepo.findByIdAndCommunityId(id, communityId)
                .map(c -> ResponseEntity.ok(toCampaignDto(c, userId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-buy/campaigns")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BulkBuyCampaignDto> createCampaign(
            @RequestBody BulkBuyCampaignRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = communityId(principal);

        ProcurementCategory cat = ProcurementCategory.OTHER;
        try { cat = ProcurementCategory.valueOf(req.category()); } catch (Exception ignored) {}

        BulkBuyCampaign campaign = BulkBuyCampaign.builder()
                .title(req.title())
                .description(req.description())
                .productName(req.productName())
                .category(cat)
                .unit(req.unit() != null ? req.unit() : "units")
                .minOrderQuantity(req.minOrderQuantity() != null ? req.minOrderQuantity() : 100)
                .targetQuantity(req.targetQuantity())
                .priceTiers(serializeTiers(req.priceTiers()))
                .retailPrice(req.retailPrice())
                .deadlineDate(java.time.LocalDate.parse(req.deadlineDate()))
                .deliveryDate(req.deliveryDate() != null ? java.time.LocalDate.parse(req.deliveryDate()) : null)
                .status(BulkBuyCampaignStatus.DRAFT)
                .communityId(communityId)
                .createdBy(user.getName() != null ? user.getName() : user.getEmail())
                .build();

        return ResponseEntity.ok(toCampaignDto(campaignRepo.save(campaign), userId(principal)));
    }

    @PostMapping("/bulk-buy/campaigns/{id}/activate")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BulkBuyCampaignDto> activateCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return campaignRepo.findByIdAndCommunityId(id, communityId(principal))
                .map(c -> {
                    c.setStatus(BulkBuyCampaignStatus.ACTIVE);
                    return ResponseEntity.ok(toCampaignDto(campaignRepo.save(c), userId(principal)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-buy/campaigns/{id}/commit")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BulkBuyCommitmentDto> commitToCampaign(
            @PathVariable Long id,
            @RequestBody CommitRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        AppUser user = loggedInUserService.resolve(principal);
        Long userId = user.getId();

        BulkBuyCampaign campaign = campaignRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() != BulkBuyCampaignStatus.ACTIVE) {
            return ResponseEntity.badRequest().build();
        }

        // Cancel any existing active commitment
        commitmentRepo.findByCampaignIdAndUserIdAndStatusNot(id, userId, CommitmentStatus.CANCELLED)
                .ifPresent(existing -> {
                    campaign.setCurrentQuantity(Math.max(0, campaign.getCurrentQuantity() - existing.getQuantity()));
                    campaign.setCommitmentCount(Math.max(0, campaign.getCommitmentCount() - 1));
                    existing.setStatus(CommitmentStatus.CANCELLED);
                    commitmentRepo.save(existing);
                });

        int newTotal = campaign.getCurrentQuantity() + req.quantity();
        java.math.BigDecimal price = effectivePrice(campaign.getPriceTiers(), newTotal);
        java.math.BigDecimal totalAmt = price != null
                ? price.multiply(java.math.BigDecimal.valueOf(req.quantity()))
                : java.math.BigDecimal.ZERO;

        BulkBuyCommitment commitment = BulkBuyCommitment.builder()
                .campaignId(id)
                .userId(userId)
                .userName(user.getName() != null ? user.getName() : user.getEmail())
                .quantity(req.quantity())
                .pricePerUnit(price)
                .totalAmount(totalAmt)
                .status(CommitmentStatus.PENDING)
                .build();

        commitmentRepo.save(commitment);

        campaign.setCurrentQuantity(newTotal);
        campaign.setCommitmentCount(campaign.getCommitmentCount() + 1);
        if (newTotal >= campaign.getMinOrderQuantity()) {
            campaign.setStatus(BulkBuyCampaignStatus.COMMITTED);
        }
        campaignRepo.save(campaign);

        return ResponseEntity.ok(toCommitmentDto(commitment));
    }

    @DeleteMapping("/bulk-buy/campaigns/{id}/commit")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> cancelCommitment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        AppUser user = loggedInUserService.resolve(principal);

        BulkBuyCampaign campaign = campaignRepo.findByIdAndCommunityId(id, communityId)
                .orElse(null);
        if (campaign == null) return ResponseEntity.notFound().build();

        commitmentRepo.findByCampaignIdAndUserIdAndStatusNot(id, user.getId(), CommitmentStatus.CANCELLED)
                .ifPresent(c -> {
                    campaign.setCurrentQuantity(Math.max(0, campaign.getCurrentQuantity() - c.getQuantity()));
                    campaign.setCommitmentCount(Math.max(0, campaign.getCommitmentCount() - 1));
                    if (campaign.getStatus() == BulkBuyCampaignStatus.COMMITTED
                            && campaign.getCurrentQuantity() < campaign.getMinOrderQuantity()) {
                        campaign.setStatus(BulkBuyCampaignStatus.ACTIVE);
                    }
                    c.setStatus(CommitmentStatus.CANCELLED);
                    commitmentRepo.save(c);
                    campaignRepo.save(campaign);
                });

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk-buy/campaigns/{id}/commitments")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BulkBuyCommitmentDto>> getCampaignCommitments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        campaignRepo.findByIdAndCommunityId(id, communityId(principal))
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        List<BulkBuyCommitmentDto> dtos = commitmentRepo.findByCampaignIdOrderByCreatedAtDesc(id)
                .stream().map(this::toCommitmentDto).toList();
        return ResponseEntity.ok(dtos);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Long communityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }

    private Long userId(UserPrincipal principal) {
        return loggedInUserService.resolve(principal).getId();
    }

    private String serializeTiers(List<PriceTierRequest> tiers) {
        if (tiers == null || tiers.isEmpty()) return "";
        return tiers.stream()
                .filter(t -> t.minQuantity() != null && t.pricePerUnit() != null)
                .map(t -> t.minQuantity() + ":" + t.pricePerUnit().toPlainString())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private List<PriceTierDto> parseTiers(String tiersStr) {
        if (tiersStr == null || tiersStr.isBlank()) return List.of();
        return Arrays.stream(tiersStr.split(","))
                .filter(s -> s.contains(":"))
                .map(s -> {
                    String[] p = s.split(":");
                    return new PriceTierDto(Integer.parseInt(p[0].trim()), new java.math.BigDecimal(p[1].trim()));
                })
                .toList();
    }

    private java.math.BigDecimal effectivePrice(String tiersStr, int currentQuantity) {
        List<PriceTierDto> tiers = parseTiers(tiersStr);
        if (tiers.isEmpty()) return null;
        return tiers.stream()
                .filter(t -> currentQuantity >= t.minQuantity())
                .max(java.util.Comparator.comparingInt(PriceTierDto::minQuantity))
                .map(PriceTierDto::pricePerUnit)
                .orElse(tiers.get(0).pricePerUnit());
    }

    private BulkBuyCampaignDto toCampaignDto(BulkBuyCampaign c, Long userId) {
        List<PriceTierDto> tiers = parseTiers(c.getPriceTiers());
        java.math.BigDecimal currentPrice = effectivePrice(c.getPriceTiers(), c.getCurrentQuantity());

        Integer myQty = null;
        Long myCommitmentId = null;
        if (userId != null) {
            var myCommitment = commitmentRepo.findByCampaignIdAndUserIdAndStatusNot(
                    c.getId(), userId, CommitmentStatus.CANCELLED);
            if (myCommitment.isPresent()) {
                myQty = myCommitment.get().getQuantity();
                myCommitmentId = myCommitment.get().getId();
            }
        }

        return new BulkBuyCampaignDto(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getProductName(), c.getCategory().name(), c.getUnit(),
                c.getMinOrderQuantity(), c.getTargetQuantity(), c.getCurrentQuantity(),
                tiers, currentPrice, c.getRetailPrice(),
                c.getSupplierId(), c.getSupplierName(),
                c.getDeadlineDate().toString(),
                c.getDeliveryDate() != null ? c.getDeliveryDate().toString() : null,
                c.getStatus().name(), c.getCommitmentCount(),
                c.getCommunityId(), c.getCreatedBy(),
                myQty, myCommitmentId,
                c.getCreatedAt().toString()
        );
    }

    private BulkBuyCommitmentDto toCommitmentDto(BulkBuyCommitment c) {
        return new BulkBuyCommitmentDto(
                c.getId(), c.getCampaignId(),
                c.getUserId(), c.getUserName(),
                c.getQuantity(),
                c.getPricePerUnit(), c.getTotalAmount(),
                c.getStatus().name(), c.getCreatedAt().toString()
        );
    }

    private SupplierQuoteDto toQuoteDto(SupplierQuote q) {
        return new SupplierQuoteDto(
                q.getId(), q.getRfq().getId(),
                q.getSupplier().getId(), q.getSupplier().getBusinessName(),
                q.getSupplier().getRating() != null ? q.getSupplier().getRating().doubleValue() : 0.0,
                q.getStatus().name(), q.getItems(),
                q.getSubTotal(), q.getGstAmount(), q.getDeliveryCharge(), q.getGrandTotal(),
                q.getValidUntil().toString(), q.getDeliveryDate().toString(),
                q.getPaymentTerms(), q.getNotes(), null,
                q.getCreatedAt().toString()
        );
    }

    private ProcurementPODto toPODto(ProcurementPO po) {
        return new ProcurementPODto(
                po.getId(), po.getPoNumber(),
                po.getRfqId(), po.getRfqNumber(),
                po.getSupplier().getId(), po.getSupplier().getBusinessName(),
                po.getStatus().name(), po.getItems(),
                po.getSubTotal(), po.getGstAmount(), po.getDeliveryCharge(), po.getGrandTotal(),
                po.getDeliveryDeadline().toString(), po.getDeliveryAddress(),
                po.getPaymentTerms(), po.getNotes(),
                po.getCreatedAt().toString(), po.getUpdatedAt().toString()
        );
    }

    // ── Request / Response Records ────────────────────────────────────────

    public record RFQRequest(
            String title, String description,
            List<RFQItemRequest> items,
            String deliveryLocation, String deliveryDeadline, String quotationDeadline,
            BigDecimal estimatedValue, BigDecimal approvedBudget, String notes
    ) {}

    public record RFQItemRequest(
            String productName, String description, String category,
            String unit, Integer quantity, BigDecimal targetPricePerUnit, String specifications
    ) {}

    public record SupplierQuoteDto(
            Long id, Long rfqId,
            Long supplierId, String supplierName, Double supplierRating,
            String status, List<SupplierQuoteItem> items,
            BigDecimal subTotal, BigDecimal gstAmount, BigDecimal deliveryCharge, BigDecimal grandTotal,
            String validUntil, String deliveryDate, String paymentTerms, String notes, Double score,
            String createdAt
    ) {}

    public record ProcurementPODto(
            Long id, String poNumber,
            Long rfqId, String rfqNumber,
            Long supplierId, String supplierName,
            String status, List<POItem> items,
            BigDecimal subTotal, BigDecimal gstAmount, BigDecimal deliveryCharge, BigDecimal grandTotal,
            String deliveryDeadline, String deliveryAddress,
            String paymentTerms, String notes,
            String createdAt, String updatedAt
    ) {}

    public record ProcurementStatsDto(
            int totalRFQs, int activeRFQs, int totalPOs,
            long totalSpend, long totalSavings,
            int activeSuppliers, double avgQuotesPerRFQ
    ) {}

    // Phase 2 records
    public record SupplierRequest(
            String businessName, String contactPerson, String mobile, String email,
            String gstNumber, String address, String city, String pincode,
            String categories, String serviceArea, String description
    ) {}

    public record ProductRequest(
            String name, String description, String category, String unit,
            java.math.BigDecimal wholesalePrice, java.math.BigDecimal retailPrice,
            Integer moq, java.math.BigDecimal gstPercent, Integer leadTimeDays
    ) {}

    public record QuoteSubmitRequest(
            Long supplierId,
            List<QuoteItemRequest> items,
            java.math.BigDecimal deliveryCharge,
            String validUntil, String deliveryDate, String paymentTerms, String notes
    ) {}

    public record QuoteItemRequest(
            Long rfqItemId, String productName, String unit, Integer quantity,
            java.math.BigDecimal pricePerUnit, java.math.BigDecimal gstPercent,
            Integer leadTimeDays, String notes
    ) {}

    public record CategorySpendDto(String category, long spend, int poCount) {}
    public record SupplierSpendDto(Long supplierId, String supplierName, long spend, int poCount) {}

    public record ProcurementAnalyticsDto(
            long totalSpend, long totalSavings, double savingsRate,
            List<CategorySpendDto> categorySpend,
            List<SupplierSpendDto> topSuppliers,
            int totalRFQs, int awardedRFQs, double avgQuotesPerRFQ, long pendingPOValue
    ) {}

    // Phase 4: BulkBuy records
    public record PriceTierRequest(Integer minQuantity, java.math.BigDecimal pricePerUnit) {}
    public record PriceTierDto(Integer minQuantity, java.math.BigDecimal pricePerUnit) {}

    public record BulkBuyCampaignRequest(
            String title, String description,
            String productName, String category, String unit,
            Integer minOrderQuantity, Integer targetQuantity,
            List<PriceTierRequest> priceTiers,
            java.math.BigDecimal retailPrice,
            String deadlineDate, String deliveryDate
    ) {}

    public record BulkBuyCampaignDto(
            Long id, String title, String description,
            String productName, String category, String unit,
            Integer minOrderQuantity, Integer targetQuantity, Integer currentQuantity,
            List<PriceTierDto> priceTiers,
            java.math.BigDecimal currentEffectivePrice, java.math.BigDecimal retailPrice,
            Long supplierId, String supplierName,
            String deadlineDate, String deliveryDate,
            String status, Integer commitmentCount,
            Long communityId, String createdBy,
            Integer myCommitmentQuantity, Long myCommitmentId,
            String createdAt
    ) {}

    public record CommitRequest(Integer quantity) {}

    public record BulkBuyCommitmentDto(
            Long id, Long campaignId,
            Long userId, String userName,
            Integer quantity,
            java.math.BigDecimal pricePerUnit, java.math.BigDecimal totalAmount,
            String status, String createdAt
    ) {}

    // ── Reverse Auction endpoints ─────────────────────────────────────────

    @GetMapping("/auctions")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<AuctionRFQDto>> getOpenAuctions(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        Long userId = userId(principal);

        List<ProcurementRFQ> rfqs = rfqRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsReverseAuction()) && r.getStatus() == RFQStatus.OPEN)
                .toList();

        List<AuctionRFQDto> dtos = rfqs.stream()
                .map(r -> toAuctionRFQDto(r, userId, communityId))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/rfqs/{id}/enable-auction")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<AuctionRFQDto> enableAuctionMode(
            @PathVariable Long id,
            @RequestBody EnableAuctionRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        ProcurementRFQ rfq = rfqRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        rfq.setIsReverseAuction(true);
        rfq.setAuctionDeadline(java.time.LocalDateTime.parse(req.auctionDeadline()));
        if (req.reservePrice() != null) rfq.setReservePrice(req.reservePrice());
        if (rfq.getStatus() == RFQStatus.DRAFT) rfq.setStatus(RFQStatus.OPEN);

        ProcurementRFQ saved = rfqRepo.save(rfq);
        return ResponseEntity.ok(toAuctionRFQDto(saved, userId(principal), communityId));
    }

    @GetMapping("/auctions/{rfqId}/bids")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<AuctionBidDto>> getAuctionBids(
            @PathVariable Long rfqId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        rfqRepo.findByIdAndCommunityId(rfqId, communityId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        List<AuctionBid> bids = auctionBidRepo.findByRfqIdAndStatusOrderByBidAmountAsc(rfqId, AuctionBidStatus.ACTIVE);
        Long userId = userId(principal);
        List<AuctionBidDto> dtos = new ArrayList<>();
        for (int i = 0; i < bids.size(); i++) {
            AuctionBid b = bids.get(i);
            dtos.add(toBidDto(b, i + 1, b.getSupplierId() != null && b.getSupplierId().equals(userId)));
        }
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/auctions/{rfqId}/bids")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<AuctionBidDto> placeAuctionBid(
            @PathVariable Long rfqId,
            @RequestBody PlaceAuctionBidRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        rfqRepo.findByIdAndCommunityId(rfqId, communityId)
                .filter(r -> Boolean.TRUE.equals(r.getIsReverseAuction()) && r.getStatus() == RFQStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("Auction not found or not open"));

        AppUser user = loggedInUserService.resolve(principal);
        Long userId = user.getId();
        String name = user.getName() != null ? user.getName() : user.getEmail();

        // Cancel existing active bid if any
        auctionBidRepo.findByRfqIdAndSupplierIdAndStatusNot(rfqId, userId, AuctionBidStatus.WITHDRAWN)
                .ifPresent(existing -> {
                    existing.setStatus(AuctionBidStatus.WITHDRAWN);
                    auctionBidRepo.save(existing);
                });

        AuctionBid bid = AuctionBid.builder()
                .rfqId(rfqId)
                .supplierId(userId)
                .supplierName(name)
                .bidAmount(req.bidAmount())
                .deliveryDays(req.deliveryDays())
                .notes(req.notes())
                .status(AuctionBidStatus.ACTIVE)
                .communityId(communityId)
                .build();

        AuctionBid saved = auctionBidRepo.save(bid);

        // Compute rank
        List<AuctionBid> all = auctionBidRepo.findByRfqIdAndStatusOrderByBidAmountAsc(rfqId, AuctionBidStatus.ACTIVE);
        int rank = 1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(saved.getId())) { rank = i + 1; break; }
        }
        return ResponseEntity.ok(toBidDto(saved, rank, true));
    }

    @DeleteMapping("/auctions/{rfqId}/bids")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> withdrawAuctionBid(
            @PathVariable Long rfqId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        rfqRepo.findByIdAndCommunityId(rfqId, communityId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        auctionBidRepo.findByRfqIdAndSupplierIdAndStatusNot(rfqId, userId(principal), AuctionBidStatus.WITHDRAWN)
                .ifPresent(b -> {
                    b.setStatus(AuctionBidStatus.WITHDRAWN);
                    auctionBidRepo.save(b);
                });

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auctions/{rfqId}/bids/{bidId}/award")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<AuctionBidDto> awardAuctionBid(
            @PathVariable Long rfqId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = communityId(principal);
        ProcurementRFQ rfq = rfqRepo.findByIdAndCommunityId(rfqId, communityId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        AuctionBid bid = auctionBidRepo.findById(bidId)
                .filter(b -> b.getRfqId().equals(rfqId) && b.getStatus() == AuctionBidStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        // Withdraw all other bids
        auctionBidRepo.findByRfqIdAndStatusOrderByBidAmountAsc(rfqId, AuctionBidStatus.ACTIVE)
                .forEach(b -> {
                    if (!b.getId().equals(bidId)) {
                        b.setStatus(AuctionBidStatus.WITHDRAWN);
                        auctionBidRepo.save(b);
                    }
                });

        bid.setStatus(AuctionBidStatus.AWARDED);
        auctionBidRepo.save(bid);

        rfq.setStatus(RFQStatus.AWARDED);
        rfqRepo.save(rfq);

        return ResponseEntity.ok(toBidDto(bid, 1, false));
    }

    private AuctionRFQDto toAuctionRFQDto(ProcurementRFQ r, Long userId, Long communityId) {
        List<AuctionBid> activeBids = auctionBidRepo.findByRfqIdAndStatusOrderByBidAmountAsc(r.getId(), AuctionBidStatus.ACTIVE);
        int bidCount = activeBids.size();
        java.math.BigDecimal lowestBid = activeBids.isEmpty() ? null : activeBids.get(0).getBidAmount();

        AuctionBidDto myBid = null;
        if (userId != null) {
            for (int i = 0; i < activeBids.size(); i++) {
                AuctionBid b = activeBids.get(i);
                if (b.getSupplierId() != null && b.getSupplierId().equals(userId)) {
                    myBid = toBidDto(b, i + 1, true);
                    break;
                }
            }
        }

        String firstItem = r.getItems().isEmpty() ? r.getTitle()
                : r.getItems().get(0).getProductName();

        return new AuctionRFQDto(
                r.getId(), r.getRfqNumber(), r.getTitle(), r.getDescription(),
                r.getStatus().name(), communityId, r.getRequestedBy(),
                firstItem,
                r.getDeliveryLocation(),
                r.getDeliveryDeadline().toString(),
                r.getQuotationDeadline().toString(),
                r.getAuctionDeadline() != null ? r.getAuctionDeadline().toString() : null,
                r.getReservePrice(),
                r.getEstimatedValue(),
                bidCount, lowestBid, myBid,
                r.getCreatedAt().toString()
        );
    }

    private AuctionBidDto toBidDto(AuctionBid b, int rank, boolean isMyBid) {
        return new AuctionBidDto(
                b.getId(), b.getRfqId(),
                b.getSupplierId(), b.getSupplierName(),
                b.getBidAmount(), b.getDeliveryDays(), b.getNotes(),
                b.getStatus().name(), rank, isMyBid,
                b.getCreatedAt().toString(), b.getUpdatedAt().toString()
        );
    }

    // Phase 5: Reverse Auction records
    public record EnableAuctionRequest(String auctionDeadline, java.math.BigDecimal reservePrice) {}

    public record PlaceAuctionBidRequest(
            java.math.BigDecimal bidAmount, Integer deliveryDays, String notes
    ) {}

    public record AuctionBidDto(
            Long id, Long rfqId,
            Long supplierId, String supplierName,
            java.math.BigDecimal bidAmount, Integer deliveryDays, String notes,
            String status, Integer rank, Boolean isMyBid,
            String createdAt, String updatedAt
    ) {}

    public record AuctionRFQDto(
            Long id, String rfqNumber, String title, String description,
            String status, Long communityId, String requestedBy,
            String primaryItem,
            String deliveryLocation, String deliveryDeadline, String quotationDeadline,
            String auctionDeadline, java.math.BigDecimal reservePrice,
            java.math.BigDecimal estimatedValue,
            Integer bidCount, java.math.BigDecimal lowestBid, AuctionBidDto myBid,
            String createdAt
    ) {}
}
