package com.manacommunity.api.fresh.controller;

import com.manacommunity.api.fresh.entity.FreshOrder;
import com.manacommunity.api.fresh.entity.FreshProduct;
import com.manacommunity.api.fresh.repository.FreshOrderRepository;
import com.manacommunity.api.fresh.repository.FreshProductRepository;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fresh")
@RequiredArgsConstructor
public class FreshProductController {

    private final FreshProductRepository productRepository;
    private final FreshOrderRepository   orderRepository;

    // ── Products ──────────────────────────────────────────────────────────────

    @GetMapping("/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FreshProduct>>> listProducts(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(ApiResponse.ok(
                productRepository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId)));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FreshProduct>> getProduct(
            @PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FreshProduct>> createProduct(
            @RequestBody FreshProduct product,
            @AuthenticationPrincipal UserPrincipal principal) {
        product.setCommunityId(principal.getCommunityId());
        FreshProduct saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FreshProduct>> updateProduct(
            @PathVariable Long id,
            @RequestBody FreshProduct patch) {
        return productRepository.findById(id).map(existing -> {
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
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        productRepository.deleteById(id);
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
    public ResponseEntity<ApiResponse<FreshOrder>> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(o -> ResponseEntity.ok(ApiResponse.ok(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FreshOrder>> placeOrder(
            @RequestBody FreshOrder order,
            @AuthenticationPrincipal UserPrincipal principal) {
        order.setCommunityId(principal.getCommunityId());
        order.setResidentId(principal.getId());
        FreshOrder saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @PutMapping("/orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FreshOrder>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return orderRepository.findById(id).map(order -> {
            if (!order.getResidentId().equals(principal.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<ApiResponse<FreshOrder>>build();
            }
            order.setStatus(FreshOrder.OrderStatus.CANCELLED);
            return ResponseEntity.ok(ApiResponse.ok(orderRepository.save(order)));
        }).orElse(ResponseEntity.notFound().build());
    }
}
