package com.manacommunity.api.fresh.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for placing a Fresh order.
 *
 * Deliberately carries NO price, status, community or resident fields: the
 * client used to POST the {@code FreshOrder} entity directly, which let a
 * resident set {@code totalAmount} and {@code status} themselves. The server
 * now derives those from the referenced products and the authenticated
 * principal.
 */
public record PlaceFreshOrderRequest(
        @NotEmpty(message = "At least one order item is required.")
        @Valid
        List<Item> items,

        @Size(max = 2000)
        String deliveryAddress,

        @Size(max = 100)
        String deliverySlot,

        @Size(max = 2000)
        String notes
) {
    public record Item(
            @NotNull(message = "productId is required.")
            Long productId,

            @NotNull(message = "quantity is required.")
            @Min(value = 1, message = "quantity must be at least 1.")
            Integer quantity
    ) {}
}
