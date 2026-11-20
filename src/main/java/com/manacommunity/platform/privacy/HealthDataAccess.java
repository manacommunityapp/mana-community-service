package com.manacommunity.platform.privacy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method that reads health/medical data.
 * {@link HealthDataAccessAspect} intercepts annotated methods and records
 * every access in {@code health_data_access_log}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HealthDataAccess {
    /** Human-readable label for the record type being accessed. */
    String recordType();

    /** Purpose of the access (e.g. "DOCTOR_VIEW", "PATIENT_VIEW"). */
    String purpose() default "SERVICE_OPERATION";
}
