package com.manacommunity.api.fresh.controller;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.fresh.dto.PlaceFreshOrderRequest;
import com.manacommunity.api.fresh.entity.FreshOrder;
import com.manacommunity.api.fresh.entity.FreshOrderItem;
import com.manacommunity.api.fresh.entity.FreshProduct;
import com.manacommunity.api.fresh.repository.FreshOrderRepository;
import com.manacommunity.api.fresh.repository.FreshProductRepository;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/v1/fresh", "/api/fresh", "/fresh"})
@RequiredArgsConstructor
public class FreshProductController {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final FreshProductRepository productRepository;
    private final FreshOrderRepository   orderRepository;

    // ── Products ──────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<FreshProduct>>> listProducts(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long targetCommunityId = communityId != null ? communityId : (principal != null ? principal.getCommunityId() : null);
        List<FreshProduct> products = targetCommunityId != null
                ? productRepository.findByCommunityIdAndActiveTrueOrderByNameAsc(targetCommunityId)
                : productRepository.findByActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<FreshProduct>> getProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return productRepository.findById(id)
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
                    .orElse(ResponseEntity.notFound().build());
        }
        return findProductForCaller(id, principal)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FreshProduct>> createProduct(
            @RequestBody FreshProduct product,
            @AuthenticationPrincipal UserPrincipal principal) {
        // A client-supplied id would make save() an UPDATE of an arbitrary row
        // (including rows of other communities). Always insert.
        product.setId(null);
        product.setCommunityId(requireCommunityId(principal));
        FreshProduct saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FreshProduct>> updateProduct(
            @PathVariable Long id,
            @RequestBody FreshProduct patch,
            @AuthenticationPrincipal UserPrincipal principal) {
        return findProductForCaller(id, principal).map(existing -> {
            if (patch.getName()        != null) existing.setName(patch.getName());
            if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
            if (patch.getUnit()        != null) existing.setUnit(patch.getUnit());
            if (patch.getPrice()       != null) existing.setPrice(patch.getPrice());
            if (patch.getStock()       != null) existing.setStock(patch.getStock());
            if (patch.getImageUrl()    != null) existing.setImageUrl(patch.getImageUrl());
            if (patch.getActive()      != null) existing.setActive(patch.getActive());
            return ResponseEntity.ok(ApiResponse.ok(productRepository.save(existing)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Optional<FreshProduct> existing = findProductForCaller(id, principal);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        productRepository.delete(existing.get());
        return ResponseEntity.noContent().build();
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FreshOrder>>> listMyOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderRepository.findByResidentIdOrderByCreatedAtDesc(principal.getId())));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FreshOrder>> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return findOrderForCaller(id, principal)
                .map(o -> ResponseEntity.ok(ApiResponse.ok(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Place an order. Price and status are computed server-side from the
     * products referenced in the request; the client cannot supply them.
     */
    @PostMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FreshOrder>> placeOrder(
            @Valid @RequestBody PlaceFreshOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = requireCommunityId(principal);

        BigDecimal total = BigDecimal.ZERO;
        // Lines are captured as well as totalled. The order used to persist only the
        // computed total, so there was no record of what had actually been ordered —
        // nothing to pick, deliver or dispute against.
        List<FreshOrderItem> lines = new java.util.ArrayList<>();
        for (PlaceFreshOrderRequest.Item item : request.items()) {
            if (item == null || item.productId() == null
                    || item.quantity() == null || item.quantity() < 1) {
                throw new InvalidInputException("Each order item needs a productId and a quantity of at least 1.");
            }
            // Scoped to the caller's community AND to active products: an
            // unknown, inactive or foreign-community product is a 400.
            FreshProduct product = productRepository
                    .findByIdAndCommunityIdAndActiveTrue(item.productId(), communityId)
                    .orElseThrow(() -> new InvalidInputException(
                            "Product " + item.productId() + " is not available in your community."));
            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            total = total.add(price.multiply(BigDecimal.valueOf(item.quantity())));

            lines.add(FreshOrderItem.builder()
                    .productId(product.getId())
                    .quantity(item.quantity())
                    // Priced now, not looked up later: a catalogue change must not
                    // alter what this resident was charged.
                    .unitPrice(price.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        FreshOrder order = FreshOrder.builder()
                .communityId(communityId)
                .residentId(principal.getId())
                .status(FreshOrder.OrderStatus.PENDING)
                .totalAmount(total.setScale(2, RoundingMode.HALF_UP))
                .deliveryAddress(request.deliveryAddress())
                .deliverySlot(request.deliverySlot())
                .notes(request.notes())
                .build();

        lines.forEach(order::addItem);

        FreshOrder saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @PutMapping("/orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FreshOrder>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return orderRepository.findByIdAndResidentId(id, principal.getId()).map(order -> {
            order.setStatus(FreshOrder.OrderStatus.CANCELLED);
            return ResponseEntity.ok(ApiResponse.ok(orderRepository.save(order)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── tenancy helpers ───────────────────────────────────────────────────────

    private static boolean isSuperAdmin(UserPrincipal principal) {
        if (principal == null || principal.getAuthorities() == null) return false;
        for (GrantedAuthority authority : principal.getAuthorities()) {
            if (ROLE_SUPER_ADMIN.equals(authority.getAuthority())) return true;
        }
        return false;
    }

    private Long requireCommunityId(UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        if (communityId == null) {
            throw new InvalidInputException("You are not associated with any community.");
        }
        return communityId;
    }

    /** Product lookup scoped to the caller's community (SUPER_ADMIN is global). */
    private Optional<FreshProduct> findProductForCaller(Long id, UserPrincipal principal) {
        return isSuperAdmin(principal)
                ? productRepository.findById(id)
                : productRepository.findByIdAndCommunityId(id, requireCommunityId(principal));
    }

    /**
     * Order lookup: a resident sees only their own orders; a community admin
     * sees orders of their own community; SUPER_ADMIN is global.
     */
    private Optional<FreshOrder> findOrderForCaller(Long id, UserPrincipal principal) {
        if (isSuperAdmin(principal)) return orderRepository.findById(id);
        Optional<FreshOrder> own = orderRepository.findByIdAndResidentId(id, principal.getId());
        if (own.isPresent()) return own;
        if (isCommunityAdmin(principal)) {
            return orderRepository.findByIdAndCommunityId(id, requireCommunityId(principal));
        }
        return Optional.empty();
    }

    private static boolean isCommunityAdmin(UserPrincipal principal) {
        if (principal == null || principal.getAuthorities() == null) return false;
        for (GrantedAuthority authority : principal.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_COMMUNITY_ADMIN".equals(role)) return true;
        }
        return false;
    }
}
