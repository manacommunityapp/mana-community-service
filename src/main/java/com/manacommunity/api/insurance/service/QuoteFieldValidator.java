package com.manacommunity.api.insurance.service;

import com.manacommunity.api.insurance.entity.InsuranceEnums.Category;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Server-side, allow-list based validation of quote inputs per product type. Unknown fields are rejected
 * (data minimisation: identity documents, medical records etc. can never be smuggled through).
 */
public final class QuoteFieldValidator {
    private QuoteFieldValidator() {}

    private static final Pattern PINCODE = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern REG_NO = Pattern.compile("^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$");

    private static final Map<Category, Set<String>> ALLOWED = new EnumMap<>(Category.class);
    static {
        Set<String> health = Set.of("pincode", "memberAges", "sumInsuredWanted", "preExistingConditionsDeclared");
        ALLOWED.put(Category.HEALTH, health);
        ALLOWED.put(Category.FAMILY_FLOATER, health);
        ALLOWED.put(Category.PARENTS, health);
        ALLOWED.put(Category.LIFE_TERM, Set.of("age", "annualIncome", "smoker", "coverWanted", "policyTermYears"));
        ALLOWED.put(Category.MOTOR_CAR, Set.of("registrationNumber", "pincode", "previousInsurerKnown"));
        ALLOWED.put(Category.MOTOR_TWO_WHEELER, Set.of("registrationNumber", "pincode", "previousInsurerKnown"));
        ALLOWED.put(Category.HOME, Set.of("pincode", "propertyType", "sumInsuredWanted"));
        ALLOWED.put(Category.TRAVEL, Set.of("destination", "startDate", "endDate", "travellers"));
        ALLOWED.put(Category.PERSONAL_ACCIDENT, Set.of("age", "coverWanted"));
        ALLOWED.put(Category.OTHER, Set.of());
    }

    /** @return map of field -> error message; empty when valid. */
    public static Map<String, String> validate(Category category, Map<String, Object> f, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        Set<String> allowed = ALLOWED.getOrDefault(category, Set.of());
        for (String key : f.keySet()) {
            if (!allowed.contains(key)) errors.put(key, "Field is not accepted for this product type");
        }
        switch (category) {
            case HEALTH, FAMILY_FLOATER, PARENTS -> {
                pincode(f, errors, true);
                Object ages = f.get("memberAges");
                if (!(ages instanceof List<?> l) || l.isEmpty() || l.size() > 12) {
                    errors.put("memberAges", "Provide 1 to 12 member ages");
                } else {
                    for (Object a : l) {
                        if (!isInt(a, 0, 100)) { errors.put("memberAges", "Each age must be 0 to 100"); break; }
                    }
                }
                if (category == Category.PARENTS && f.get("memberAges") instanceof List<?> l2) {
                    for (Object a : l2) if (isInt(a, 0, 17)) errors.put("memberAges", "Parents' cover is for adults");
                }
            }
            case LIFE_TERM -> {
                if (!isInt(f.get("age"), 18, 65)) errors.put("age", "Age must be 18 to 65");
                if (!isNumber(f.get("annualIncome"), 1, 1_000_000_000)) errors.put("annualIncome", "Enter a positive income");
                if (!(f.get("smoker") instanceof Boolean)) errors.put("smoker", "Select yes or no");
            }
            case MOTOR_CAR, MOTOR_TWO_WHEELER -> {
                Object reg = f.get("registrationNumber");
                String norm = reg == null ? "" : reg.toString().replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
                if (!REG_NO.matcher(norm).matches()) errors.put("registrationNumber", "Enter a valid registration number");
                pincode(f, errors, true);
            }
            case HOME -> {
                pincode(f, errors, true);
                if (!(f.get("propertyType") instanceof String s) || s.isBlank() || s.length() > 30)
                    errors.put("propertyType", "Select a property type");
                if (!isNumber(f.get("sumInsuredWanted"), 1, 10_000_000_000L)) errors.put("sumInsuredWanted", "Enter a positive amount");
            }
            case TRAVEL -> {
                if (!(f.get("destination") instanceof String s) || s.isBlank() || s.length() > 60)
                    errors.put("destination", "Enter a destination");
                LocalDate start = date(f.get("startDate")), end = date(f.get("endDate"));
                if (start == null) errors.put("startDate", "Enter a valid start date");
                else if (start.isBefore(today)) errors.put("startDate", "Start date cannot be in the past");
                if (end == null) errors.put("endDate", "Enter a valid end date");
                if (start != null && end != null) {
                    if (end.isBefore(start)) errors.put("endDate", "End date must be on or after start date");
                    else if (ChronoUnit.DAYS.between(start, end) > 365) errors.put("endDate", "Trip cannot exceed 365 days");
                }
                if (!isInt(f.get("travellers"), 1, 10)) errors.put("travellers", "Travellers must be 1 to 10");
            }
            case PERSONAL_ACCIDENT -> {
                if (!isInt(f.get("age"), 18, 70)) errors.put("age", "Age must be 18 to 70");
            }
            default -> errors.put("category", "Quotes are not available for this category");
        }
        return errors;
    }

    private static void pincode(Map<String, Object> f, Map<String, String> errors, boolean required) {
        Object p = f.get("pincode");
        if (p == null) { if (required) errors.put("pincode", "Enter a 6-digit pincode"); return; }
        if (!PINCODE.matcher(p.toString()).matches()) errors.put("pincode", "Enter a valid 6-digit pincode");
    }

    private static boolean isInt(Object o, int min, int max) {
        if (!(o instanceof Number n)) return false;
        double d = n.doubleValue();
        return d == Math.rint(d) && d >= min && d <= max;
    }

    private static boolean isNumber(Object o, double min, double max) {
        return o instanceof Number n && n.doubleValue() >= min && n.doubleValue() <= max;
    }

    private static LocalDate date(Object o) {
        try { return o == null ? null : LocalDate.parse(o.toString()); } catch (Exception e) { return null; }
    }
}
