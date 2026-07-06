package com.manacommunity.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create / update payload for an {@link com.manacommunity.api.model.Expense}.
 *
 * <p>The receipt file (if any) is uploaded as a separate multipart part, so it is
 * NOT part of this JSON body. {@code category} is a free-form string to match the
 * entity (e.g. EVENT, MAINTENANCE, SPORTS, UTILITIES, SECURITY, CLEANING, OTHER).</p>
 */
public record ExpenseRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "Category is required")
        @Size(max = 30, message = "Category must be at most 30 characters")
        String category,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {}
