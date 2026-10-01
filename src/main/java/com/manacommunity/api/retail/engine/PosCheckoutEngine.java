package com.manacommunity.api.retail.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PosCheckoutEngine {

    public record PosLineItem(Long productId, String name, int qty, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal taxRatePercent) {}

    public record PosBillSummary(BigDecimal subtotal, BigDecimal totalDiscount, BigDecimal taxableAmount, BigDecimal totalTax, BigDecimal netPayable) {}

    /**
     * Computes a full POS receipt summary with line discounts, taxable base, and GST taxes.
     */
    public PosBillSummary computeBillSummary(List<PosLineItem> items) {
        if (items == null || items.isEmpty()) {
            return new PosBillSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (PosLineItem item : items) {
            BigDecimal lineGross = item.unitPrice().multiply(BigDecimal.valueOf(item.qty()));
            subtotal = subtotal.add(lineGross);

            BigDecimal lineDiscount = BigDecimal.ZERO;
            if (item.discountPercent() != null && item.discountPercent().compareTo(BigDecimal.ZERO) > 0) {
                lineDiscount = lineGross.multiply(item.discountPercent())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            totalDiscount = totalDiscount.add(lineDiscount);

            BigDecimal lineTaxable = lineGross.subtract(lineDiscount);
            BigDecimal lineTax = BigDecimal.ZERO;
            if (item.taxRatePercent() != null && item.taxRatePercent().compareTo(BigDecimal.ZERO) > 0) {
                lineTax = lineTaxable.multiply(item.taxRatePercent())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            totalTax = totalTax.add(lineTax);
        }

        BigDecimal taxableAmount = subtotal.subtract(totalDiscount).max(BigDecimal.ZERO);
        BigDecimal netPayable = taxableAmount.add(totalTax).setScale(2, RoundingMode.HALF_UP);

        return new PosBillSummary(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                totalDiscount.setScale(2, RoundingMode.HALF_UP),
                taxableAmount.setScale(2, RoundingMode.HALF_UP),
                totalTax.setScale(2, RoundingMode.HALF_UP),
                netPayable
        );
    }

    /**
     * Computes change to be returned to customer upon cash tender.
     */
    public BigDecimal calculateTenderChange(BigDecimal netPayable, BigDecimal tenderedAmount) {
        if (tenderedAmount == null || netPayable == null) {
            return BigDecimal.ZERO;
        }
        if (tenderedAmount.compareTo(netPayable) < 0) {
            throw new IllegalArgumentException("Tendered amount is less than total payable bill");
        }
        return tenderedAmount.subtract(netPayable).setScale(2, RoundingMode.HALF_UP);
    }
}
