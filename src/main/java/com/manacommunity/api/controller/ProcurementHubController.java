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
    private final LoggedInUserService loggedInUserService;

    public ProcurementHubController(
            SupplierProfileRepository supplierRepo,
            SupplierProductRepository productRepo,
            ProcurementRFQRepository rfqRepo,
            SupplierQuoteRepository quoteRepo,
            ProcurementPORepository poRepo,
            LoggedInUserService loggedInUserService) {
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
        this.rfqRepo = rfqRepo;
        this.quoteRepo = quoteRepo;
        this.poRepo = poRepo;
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

    // ── Helpers ───────────────────────────────────────────────────────────

    private Long communityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
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
}
