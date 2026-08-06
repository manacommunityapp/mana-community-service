package com.manacommunity.api.cfbos.charge.engine;

import com.manacommunity.api.cfbos.charge.dto.ChargeCalculationResult;
import com.manacommunity.api.cfbos.charge.dto.PropertyContext;
import com.manacommunity.api.cfbos.charge.entity.TierConfig;
import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ChargeCalculationEngine {

    private final ExpressionParser parser = new SpelExpressionParser();

    public ChargeCalculationResult calculate(CalculationMethod method,
                                              BigDecimal fixedAmount,
                                              BigDecimal ratePerUnit,
                                              PropertyContext context) {
        if (method != CalculationMethod.FIXED && ratePerUnit == null) {
            throw new CfbosException("ratePerUnit is required for " + method + " calculation",
                    HttpStatus.BAD_REQUEST, "CFBOS_MISSING_RATE");
        }
        return switch (method) {
            case FIXED -> fixed(fixedAmount);
            case AREA_BASED -> areaBased(ratePerUnit, context);
            case UNIT_BASED -> unitBased(ratePerUnit, context);
            case CONSUMPTION_BASED, METER_BASED -> consumptionBased(ratePerUnit, context);
            case OCCUPANCY_BASED -> occupancyBased(ratePerUnit, context);
            default -> throw new CfbosException("Unsupported calculation method: " + method);
        };
    }

    public ChargeCalculationResult calculateSlab(List<TierConfig> tiers, BigDecimal quantity) {
        List<TierConfig> sorted = tiers.stream()
                .sorted(Comparator.comparing(TierConfig::getTierOrder)).toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal remaining = quantity;
        StringBuilder details = new StringBuilder();

        for (TierConfig tier : sorted) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal tierWidth = tier.getTierTo() != null
                    ? tier.getTierTo().subtract(tier.getTierFrom())
                    : remaining;

            BigDecimal applicable = remaining.min(tierWidth);
            BigDecimal tierAmount = applicable.multiply(tier.getRate())
                    .setScale(2, RoundingMode.HALF_UP)
                    .add(tier.getFixedCharge());
            total = total.add(tierAmount);
            remaining = remaining.subtract(applicable);

            details.append(String.format("%s×%s=%s; ",
                    applicable.stripTrailingZeros().toPlainString(),
                    tier.getRate().stripTrailingZeros().toPlainString(),
                    tierAmount.stripTrailingZeros().toPlainString()));
        }

        return ChargeCalculationResult.builder()
                .method(CalculationMethod.SLAB_BASED)
                .amount(total)
                .quantity(quantity)
                .calculationDetails(details.toString().trim())
                .build();
    }

    public BigDecimal evaluateFormula(String expression, Map<String, Object> variables) {
        // SimpleEvaluationContext (not StandardEvaluationContext) is used deliberately: it
        // disallows SpEL type references, constructor calls and bean references, which closes
        // off the T(java.lang.Runtime).getRuntime().exec(...) style RCE that a StandardEvaluationContext
        // would allow when evaluating admin-supplied formula expressions. MapAccessor remains
        // needed so bare property names ("area", not "#area") resolve against the Map root object,
        // which is the SpEL syntax required by ChargeCalculationEngineTest.
        SimpleEvaluationContext ctx = SimpleEvaluationContext
                .forPropertyAccessors(new MapAccessor())
                .withInstanceMethods()
                .build();

        Object result = parser.parseExpression(expression).getValue(ctx, variables);
        if (result instanceof Number num) {
            return new BigDecimal(num.toString()).setScale(2, RoundingMode.HALF_UP);
        }
        throw new CfbosException("Formula did not return a number: " + expression);
    }

    private ChargeCalculationResult fixed(BigDecimal amount) {
        return ChargeCalculationResult.builder()
                .method(CalculationMethod.FIXED).amount(amount)
                .quantity(BigDecimal.ONE).rate(amount)
                .calculationDetails("Fixed: " + amount).build();
    }

    private ChargeCalculationResult areaBased(BigDecimal ratePerUnit, PropertyContext ctx) {
        BigDecimal area = ctx.getArea() != null ? ctx.getArea() : BigDecimal.ZERO;
        BigDecimal amount = area.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP);
        return ChargeCalculationResult.builder()
                .method(CalculationMethod.AREA_BASED).amount(amount)
                .quantity(area).rate(ratePerUnit)
                .calculationDetails(area + " × " + ratePerUnit).build();
    }

    private ChargeCalculationResult unitBased(BigDecimal ratePerUnit, PropertyContext ctx) {
        BigDecimal units = ctx.getParkingSlots() != null
                ? new BigDecimal(ctx.getParkingSlots()) : BigDecimal.ONE;
        BigDecimal amount = units.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP);
        return ChargeCalculationResult.builder()
                .method(CalculationMethod.UNIT_BASED).amount(amount)
                .quantity(units).rate(ratePerUnit)
                .calculationDetails(units + " × " + ratePerUnit).build();
    }

    private ChargeCalculationResult consumptionBased(BigDecimal ratePerUnit, PropertyContext ctx) {
        BigDecimal consumption = ctx.getConsumption() != null ? ctx.getConsumption() : BigDecimal.ZERO;
        BigDecimal amount = consumption.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP);
        return ChargeCalculationResult.builder()
                .method(CalculationMethod.CONSUMPTION_BASED).amount(amount)
                .quantity(consumption).rate(ratePerUnit)
                .calculationDetails(consumption + " × " + ratePerUnit).build();
    }

    private ChargeCalculationResult occupancyBased(BigDecimal ratePerUnit, PropertyContext ctx) {
        BigDecimal occupants = ctx.getOccupants() != null
                ? new BigDecimal(ctx.getOccupants()) : BigDecimal.ONE;
        BigDecimal amount = occupants.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP);
        return ChargeCalculationResult.builder()
                .method(CalculationMethod.OCCUPANCY_BASED).amount(amount)
                .quantity(occupants).rate(ratePerUnit)
                .calculationDetails(occupants + " × " + ratePerUnit).build();
    }
}
