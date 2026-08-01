# Community Services Platform — Phase 1: Core Service Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a config-driven service catalog, provider registry, service request lifecycle, and work order management engine that supports unlimited service domains without code changes.

**Architecture:** New `com.manacommunity.api.serviceplatform` package within the existing Spring Boot monolith, following the same layered patterns as `booking` and `marketplace`. JPA entities with Long PKs, JSONB for extensible fields, manual timestamp auditing, DTO-based responses, string-authority security.

**Tech Stack:** Java 17+, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Lombok, Jakarta Validation, JUnit 5, Mockito, AssertJ

## Global Constraints

- Primary keys: `Long` with `GenerationType.IDENTITY` (matches all existing entities)
- Timestamps: manual `@PrePersist` / `@PreUpdate` with `LocalDateTime.now()` (no Spring Data Auditing)
- JSONB columns: `@JdbcTypeCode(SqlTypes.JSON)` with `@Column(columnDefinition = "jsonb")`, stored as `String`
- Enums: `@Enumerated(EnumType.STRING)`, separate files in `entity/enums/` subpackage
- Relationships: `@ManyToOne(fetch = FetchType.LAZY)` always
- DTOs: Request = `@Data` class with Jakarta validation; Response = `@Data @Builder` class
- Controllers: `@PreAuthorize("hasAuthority('...')")`, resolve user via `LoggedInUserService`
- Services: `@Service @RequiredArgsConstructor`, return DTOs, manual `toResponse()` mappers
- Tests: `@ExtendWith(MockitoExtension.class)`, `@Nested @DisplayName`, AssertJ, `TestDataBuilder`
- Exceptions: Reuse existing classes from `com.manacommunity.api.exception`
- Audit: Call `auditService.record(action, module, entityName, entityId)` on mutations
- Base path: `src/main/java/com/manacommunity/api/serviceplatform/`
- Test path: `src/test/java/com/manacommunity/api/unit/service/serviceplatform/`

---

## File Structure

### Entities & Enums
- Create: `serviceplatform/entity/enums/ProviderType.java`
- Create: `serviceplatform/entity/enums/VerificationStatus.java`
- Create: `serviceplatform/entity/enums/PricingUnit.java`
- Create: `serviceplatform/entity/enums/ServiceUrgency.java`
- Create: `serviceplatform/entity/enums/ServiceRequestStatus.java`
- Create: `serviceplatform/entity/enums/WorkOrderStatus.java`
- Create: `serviceplatform/entity/ServiceDomain.java`
- Create: `serviceplatform/entity/ServiceCategory.java`
- Create: `serviceplatform/entity/ServiceProvider.java`
- Create: `serviceplatform/entity/ProviderServiceOffering.java`
- Create: `serviceplatform/entity/ServiceRequest.java`
- Create: `serviceplatform/entity/WorkOrder.java`

### Repositories
- Create: `serviceplatform/repository/ServiceDomainRepository.java`
- Create: `serviceplatform/repository/ServiceCategoryRepository.java`
- Create: `serviceplatform/repository/ServiceProviderRepository.java`
- Create: `serviceplatform/repository/ProviderServiceOfferingRepository.java`
- Create: `serviceplatform/repository/ServiceRequestRepository.java`
- Create: `serviceplatform/repository/WorkOrderRepository.java`

### DTOs
- Create: `serviceplatform/dto/request/CreateServiceDomainRequest.java`
- Create: `serviceplatform/dto/request/CreateServiceCategoryRequest.java`
- Create: `serviceplatform/dto/request/RegisterProviderRequest.java`
- Create: `serviceplatform/dto/request/CreateOfferingRequest.java`
- Create: `serviceplatform/dto/request/CreateServiceRequestDto.java`
- Create: `serviceplatform/dto/request/UpdateWorkOrderStatusRequest.java`
- Create: `serviceplatform/dto/request/AssignProviderRequest.java`
- Create: `serviceplatform/dto/response/ServiceDomainResponse.java`
- Create: `serviceplatform/dto/response/ServiceCategoryResponse.java`
- Create: `serviceplatform/dto/response/ServiceProviderResponse.java`
- Create: `serviceplatform/dto/response/ServiceOfferingResponse.java`
- Create: `serviceplatform/dto/response/ServiceRequestResponse.java`
- Create: `serviceplatform/dto/response/WorkOrderResponse.java`
- Create: `serviceplatform/dto/response/ServiceSearchResult.java`

### Services
- Create: `serviceplatform/service/ServiceCatalogService.java`
- Create: `serviceplatform/service/ServiceProviderService.java`
- Create: `serviceplatform/service/ServiceRequestService.java`
- Create: `serviceplatform/service/WorkOrderService.java`
- Create: `serviceplatform/service/ServiceSearchService.java`

### Controllers
- Create: `serviceplatform/controller/ServiceDomainController.java`
- Create: `serviceplatform/controller/ServiceCategoryController.java`
- Create: `serviceplatform/controller/ServiceProviderController.java`
- Create: `serviceplatform/controller/ServiceOfferingController.java`
- Create: `serviceplatform/controller/ServiceRequestController.java`
- Create: `serviceplatform/controller/WorkOrderController.java`

### Cross-cutting
- Modify: `exception/ManaCommunityException.java` — no changes needed, reuse as-is
- Modify: `model/AuditAction.java` — add new enum values
- Modify: `model/AuditModule.java` — add `SERVICE_PLATFORM`
- Modify: `constants/PermissionConstants.java` — add service platform permissions
- Create: `serviceplatform/seed/V999__seed_service_domains_categories.sql`

### Tests
- Create: `unit/service/serviceplatform/ServiceCatalogServiceTest.java`
- Create: `unit/service/serviceplatform/ServiceProviderServiceTest.java`
- Create: `unit/service/serviceplatform/ServiceRequestServiceTest.java`
- Create: `unit/service/serviceplatform/WorkOrderServiceTest.java`
- Create: `unit/service/serviceplatform/ServiceSearchServiceTest.java`

---

### Task 1: Enums and Entity Layer

**Files:**
- Create: `serviceplatform/entity/enums/ProviderType.java`
- Create: `serviceplatform/entity/enums/VerificationStatus.java`
- Create: `serviceplatform/entity/enums/PricingUnit.java`
- Create: `serviceplatform/entity/enums/ServiceUrgency.java`
- Create: `serviceplatform/entity/enums/ServiceRequestStatus.java`
- Create: `serviceplatform/entity/enums/WorkOrderStatus.java`
- Create: `serviceplatform/entity/ServiceDomain.java`
- Create: `serviceplatform/entity/ServiceCategory.java`
- Create: `serviceplatform/entity/ServiceProvider.java`
- Create: `serviceplatform/entity/ProviderServiceOffering.java`
- Create: `serviceplatform/entity/ServiceRequest.java`
- Create: `serviceplatform/entity/WorkOrder.java`

**Interfaces:**
- Consumes: `Community` entity from `com.manacommunity.api.model`, `AppUser` from `com.manacommunity.api.user.model`, `Vendor` from `com.manacommunity.api.model`, `Invoice` from `com.manacommunity.api.model`
- Produces: All 6 entity classes and 6 enum types used by every subsequent task

- [ ] **Step 1: Create the enums directory and all enum files**

```java
// serviceplatform/entity/enums/ProviderType.java
package com.manacommunity.api.serviceplatform.entity.enums;

public enum ProviderType {
    INDIVIDUAL,
    COMPANY
}
```

```java
// serviceplatform/entity/enums/VerificationStatus.java
package com.manacommunity.api.serviceplatform.entity.enums;

public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    SUSPENDED
}
```

```java
// serviceplatform/entity/enums/PricingUnit.java
package com.manacommunity.api.serviceplatform.entity.enums;

public enum PricingUnit {
    FLAT,
    HOURLY,
    PER_UNIT,
    CUSTOM
}
```

```java
// serviceplatform/entity/enums/ServiceUrgency.java
package com.manacommunity.api.serviceplatform.entity.enums;

public enum ServiceUrgency {
    NORMAL,
    URGENT,
    EMERGENCY
}
```

```java
// serviceplatform/entity/enums/ServiceRequestStatus.java
package com.manacommunity.api.serviceplatform.entity.enums;

public enum ServiceRequestStatus {
    DRAFT,
    SUBMITTED,
    MATCHING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    DISPUTED
}
```

```java
// serviceplatform/entity/enums/WorkOrderStatus.java
package com.manacommunity.api.serviceplatform.entity.enums;

public enum WorkOrderStatus {
    CREATED,
    SCHEDULED,
    EN_ROUTE,
    ARRIVED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED
}
```

- [ ] **Step 2: Create ServiceDomain entity**

```java
// serviceplatform/entity/ServiceDomain.java
package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_domain", indexes = {
        @Index(name = "idx_service_domain_community", columnList = "community_id"),
        @Index(name = "idx_service_domain_slug", columnList = "slug")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(length = 50)
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ServiceCategory> categories = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create ServiceCategory entity**

```java
// serviceplatform/entity/ServiceCategory.java
package com.manacommunity.api.serviceplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_category", indexes = {
        @Index(name = "idx_service_category_domain", columnList = "domain_id"),
        @Index(name = "idx_service_category_slug", columnList = "slug"),
        @Index(name = "idx_service_category_parent", columnList = "parent_category_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceDomain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceCategory parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ServiceCategory> subCategories = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(length = 50)
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_certifications", columnDefinition = "jsonb")
    private String requiredCertifications;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private String customFields;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: Create ServiceProvider entity**

```java
// serviceplatform/entity/ServiceProvider.java
package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_provider", indexes = {
        @Index(name = "idx_service_provider_user", columnList = "user_id"),
        @Index(name = "idx_service_provider_community", columnList = "community_id"),
        @Index(name = "idx_service_provider_status", columnList = "verification_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    @Builder.Default
    private ProviderType providerType = ProviderType.INDIVIDUAL;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_jobs_completed")
    @Builder.Default
    private Integer totalJobsCompleted = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_areas", columnDefinition = "jsonb")
    private String serviceAreas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String certifications;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProviderServiceOffering> offerings = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 5: Create ProviderServiceOffering entity**

```java
// serviceplatform/entity/ProviderServiceOffering.java
package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "provider_service_offering", indexes = {
        @Index(name = "idx_offering_provider", columnList = "provider_id"),
        @Index(name = "idx_offering_category", columnList = "category_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", nullable = false, length = 20)
    @Builder.Default
    private PricingUnit pricingUnit = PricingUnit.FLAT;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_field_values", columnDefinition = "jsonb")
    private String customFieldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 6: Create ServiceRequest entity**

```java
// serviceplatform/entity/ServiceRequest.java
package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_request", indexes = {
        @Index(name = "idx_service_request_requester", columnList = "requester_id"),
        @Index(name = "idx_service_request_community", columnList = "community_id"),
        @Index(name = "idx_service_request_category", columnList = "category_id"),
        @Index(name = "idx_service_request_status", columnList = "status"),
        @Index(name = "idx_service_request_provider", columnList = "assigned_provider_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "preferred_time_slot", length = 50)
    private String preferredTimeSlot;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceUrgency urgency = ServiceUrgency.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceRequestStatus status = ServiceRequestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_provider_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceProvider assignedProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_offering_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProviderServiceOffering assignedOffering;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_field_values", columnDefinition = "jsonb")
    private String customFieldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attachments;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 7: Create WorkOrder entity**

```java
// serviceplatform/entity/WorkOrder.java
package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order", indexes = {
        @Index(name = "idx_work_order_request", columnList = "service_request_id"),
        @Index(name = "idx_work_order_provider", columnList = "provider_id"),
        @Index(name = "idx_work_order_community", columnList = "community_id"),
        @Index(name = "idx_work_order_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_items", columnDefinition = "jsonb")
    private String checklistItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "materials_used", columnDefinition = "jsonb")
    private String materialsUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_photos", columnDefinition = "jsonb")
    private String beforePhotos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_photos", columnDefinition = "jsonb")
    private String afterPhotos;

    @Column(name = "resident_signoff")
    @Builder.Default
    private boolean residentSignoff = false;

    @Column(name = "resident_signoff_at")
    private LocalDateTime residentSignoffAt;

    @Column(name = "provider_signoff")
    @Builder.Default
    private boolean providerSignoff = false;

    @Column(name = "provider_signoff_at")
    private LocalDateTime providerSignoffAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Invoice invoice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 8: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/entity/
git commit -m "feat(csp): add entity and enum layer for service platform"
```

---

### Task 2: Repository Layer

**Files:**
- Create: `serviceplatform/repository/ServiceDomainRepository.java`
- Create: `serviceplatform/repository/ServiceCategoryRepository.java`
- Create: `serviceplatform/repository/ServiceProviderRepository.java`
- Create: `serviceplatform/repository/ProviderServiceOfferingRepository.java`
- Create: `serviceplatform/repository/ServiceRequestRepository.java`
- Create: `serviceplatform/repository/WorkOrderRepository.java`

**Interfaces:**
- Consumes: All entities from Task 1
- Produces: Repository interfaces used by all service classes in Tasks 4-8

- [ ] **Step 1: Create ServiceDomainRepository**

```java
// serviceplatform/repository/ServiceDomainRepository.java
package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceDomainRepository extends JpaRepository<ServiceDomain, Long> {

    List<ServiceDomain> findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(Long communityId);

    List<ServiceDomain> findByCommunityIdIsNullAndActiveTrueOrderByDisplayOrderAsc();

    Optional<ServiceDomain> findBySlugAndCommunityId(String slug, Long communityId);

    boolean existsBySlugAndCommunityId(String slug, Long communityId);
}
```

- [ ] **Step 2: Create ServiceCategoryRepository**

```java
// serviceplatform/repository/ServiceCategoryRepository.java
package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByDomainIdAndActiveTrueOrderByDisplayOrderAsc(Long domainId);

    List<ServiceCategory> findByDomainIdAndParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc(Long domainId);

    boolean existsBySlugAndDomainId(String slug, Long domainId);

    @Query("SELECT c FROM ServiceCategory c WHERE c.domain.id = :domainId AND c.active = true " +
           "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ServiceCategory> searchByName(@Param("domainId") Long domainId,
                                       @Param("query") String query,
                                       Pageable pageable);
}
```

- [ ] **Step 3: Create ServiceProviderRepository**

```java
// serviceplatform/repository/ServiceProviderRepository.java
package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

    Optional<ServiceProvider> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<ServiceProvider> findByCommunityIdAndVerificationStatus(Long communityId,
                                                                  VerificationStatus status,
                                                                  Pageable pageable);

    Page<ServiceProvider> findByCommunityIdAndVerificationStatusNot(Long communityId,
                                                                     VerificationStatus status,
                                                                     Pageable pageable);
}
```

- [ ] **Step 4: Create ProviderServiceOfferingRepository**

```java
// serviceplatform/repository/ProviderServiceOfferingRepository.java
package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProviderServiceOfferingRepository extends JpaRepository<ProviderServiceOffering, Long> {

    List<ProviderServiceOffering> findByProviderIdAndAvailableTrue(Long providerId);

    List<ProviderServiceOffering> findByProviderId(Long providerId);

    Page<ProviderServiceOffering> findByCategoryIdAndAvailableTrue(Long categoryId, Pageable pageable);

    @Query("SELECT o FROM ProviderServiceOffering o " +
           "JOIN o.provider p " +
           "WHERE o.category.id = :categoryId " +
           "AND o.available = true " +
           "AND p.communityId = :communityId " +
           "AND p.verificationStatus = 'VERIFIED'")
    Page<ProviderServiceOffering> findVerifiedByCategoryAndCommunity(
            @Param("categoryId") Long categoryId,
            @Param("communityId") Long communityId,
            Pageable pageable);

    boolean existsByProviderIdAndCategoryId(Long providerId, Long categoryId);
}
```

- [ ] **Step 5: Create ServiceRequestRepository**

```java
// serviceplatform/repository/ServiceRequestRepository.java
package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    Page<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    Page<ServiceRequest> findByCommunityIdAndStatus(Long communityId,
                                                     ServiceRequestStatus status,
                                                     Pageable pageable);

    Page<ServiceRequest> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<ServiceRequest> findByAssignedProviderIdAndStatusIn(Long providerId,
                                                              java.util.List<ServiceRequestStatus> statuses,
                                                              Pageable pageable);

    Page<ServiceRequest> findByCategoryIdAndStatus(Long categoryId,
                                                    ServiceRequestStatus status,
                                                    Pageable pageable);
}
```

- [ ] **Step 6: Create WorkOrderRepository**

```java
// serviceplatform/repository/WorkOrderRepository.java
package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByServiceRequestId(Long serviceRequestId);

    Page<WorkOrder> findByProviderIdAndStatusIn(Long providerId,
                                                 java.util.List<WorkOrderStatus> statuses,
                                                 Pageable pageable);

    Page<WorkOrder> findByProviderIdOrderByCreatedAtDesc(Long providerId, Pageable pageable);

    Page<WorkOrder> findByCommunityIdAndStatus(Long communityId,
                                                WorkOrderStatus status,
                                                Pageable pageable);

    Page<WorkOrder> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);
}
```

- [ ] **Step 7: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/repository/
git commit -m "feat(csp): add repository layer for service platform"
```

---

### Task 3: DTO Layer

**Files:**
- Create: all files in `serviceplatform/dto/request/` and `serviceplatform/dto/response/`

**Interfaces:**
- Consumes: Enum types from Task 1 (as Strings in DTOs)
- Produces: Request/response DTOs used by services (Tasks 4-8) and controllers (Tasks 9-11)

- [ ] **Step 1: Create request DTOs**

```java
// serviceplatform/dto/request/CreateServiceDomainRequest.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceDomainRequest {
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 100)
    private String slug;
    @Size(max = 50)
    private String icon;
    private String description;
    private Integer displayOrder;
    private String metadata;
}
```

```java
// serviceplatform/dto/request/CreateServiceCategoryRequest.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceCategoryRequest {
    @NotNull
    private Long domainId;
    private Long parentCategoryId;
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 100)
    private String slug;
    @Size(max = 50)
    private String icon;
    private String description;
    private String requiredCertifications;
    private String customFields;
    private Integer displayOrder;
}
```

```java
// serviceplatform/dto/request/RegisterProviderRequest.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterProviderRequest {
    @NotBlank
    private String providerType;
    @Size(max = 200)
    private String businessName;
    @Size(max = 20)
    private String phone;
    @Size(max = 100)
    private String email;
    private String bio;
    private String profileImageUrl;
    private String serviceAreas;
    private String certifications;
    private Long vendorId;
}
```

```java
// serviceplatform/dto/request/CreateOfferingRequest.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOfferingRequest {
    @NotNull
    private Long categoryId;
    @NotBlank @Size(max = 200)
    private String title;
    private String description;
    @NotNull
    private BigDecimal basePrice;
    @NotBlank
    private String pricingUnit;
    private Integer estimatedDurationMinutes;
    private BigDecimal minOrderValue;
    private String customFieldValues;
    private String tags;
}
```

```java
// serviceplatform/dto/request/CreateServiceRequestDto.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateServiceRequestDto {
    @NotNull
    private Long categoryId;
    @NotBlank @Size(max = 200)
    private String title;
    private String description;
    private LocalDate preferredDate;
    @Size(max = 50)
    private String preferredTimeSlot;
    private String address;
    private String urgency;
    private BigDecimal estimatedCost;
    private String customFieldValues;
    private String attachments;
    private boolean submitImmediately;
}
```

```java
// serviceplatform/dto/request/UpdateWorkOrderStatusRequest.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWorkOrderStatusRequest {
    @NotBlank
    private String status;
    private String notes;
    private String checklistItems;
    private String materialsUsed;
    private String beforePhotos;
    private String afterPhotos;
}
```

```java
// serviceplatform/dto/request/AssignProviderRequest.java
package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignProviderRequest {
    @NotNull
    private Long providerId;
    private Long offeringId;
}
```

- [ ] **Step 2: Create response DTOs**

```java
// serviceplatform/dto/response/ServiceDomainResponse.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceDomainResponse {
    private Long id;
    private String name;
    private String slug;
    private String icon;
    private String description;
    private Integer displayOrder;
    private boolean active;
    private String metadata;
    private int categoryCount;
    private List<ServiceCategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// serviceplatform/dto/response/ServiceCategoryResponse.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceCategoryResponse {
    private Long id;
    private Long domainId;
    private String domainName;
    private Long parentCategoryId;
    private String parentCategoryName;
    private String name;
    private String slug;
    private String icon;
    private String description;
    private String requiredCertifications;
    private String customFields;
    private Integer displayOrder;
    private boolean active;
    private List<ServiceCategoryResponse> subCategories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// serviceplatform/dto/response/ServiceProviderResponse.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceProviderResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long vendorId;
    private String providerType;
    private String businessName;
    private String phone;
    private String email;
    private String bio;
    private String profileImageUrl;
    private String verificationStatus;
    private BigDecimal avgRating;
    private Integer totalJobsCompleted;
    private String serviceAreas;
    private String certifications;
    private List<ServiceOfferingResponse> offerings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// serviceplatform/dto/response/ServiceOfferingResponse.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceOfferingResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private String pricingUnit;
    private Integer estimatedDurationMinutes;
    private BigDecimal minOrderValue;
    private boolean available;
    private String customFieldValues;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// serviceplatform/dto/response/ServiceRequestResponse.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceRequestResponse {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private Long categoryId;
    private String categoryName;
    private String domainName;
    private String title;
    private String description;
    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String address;
    private String urgency;
    private String status;
    private Long assignedProviderId;
    private String assignedProviderName;
    private Long assignedOfferingId;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String customFieldValues;
    private String attachments;
    private String cancellationReason;
    private WorkOrderResponse workOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// serviceplatform/dto/response/WorkOrderResponse.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkOrderResponse {
    private Long id;
    private Long serviceRequestId;
    private Long providerId;
    private String providerName;
    private String status;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private String notes;
    private String checklistItems;
    private String materialsUsed;
    private String beforePhotos;
    private String afterPhotos;
    private boolean residentSignoff;
    private LocalDateTime residentSignoffAt;
    private boolean providerSignoff;
    private LocalDateTime providerSignoffAt;
    private Long invoiceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// serviceplatform/dto/response/ServiceSearchResult.java
package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ServiceSearchResult {
    private Long offeringId;
    private String offeringTitle;
    private String offeringDescription;
    private BigDecimal basePrice;
    private String pricingUnit;
    private Integer estimatedDurationMinutes;
    private Long providerId;
    private String providerName;
    private String providerType;
    private BigDecimal providerRating;
    private Integer providerTotalJobs;
    private String verificationStatus;
    private Long categoryId;
    private String categoryName;
    private Long domainId;
    private String domainName;
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/dto/
git commit -m "feat(csp): add request/response DTOs for service platform"
```

---

### Task 4: Audit and Permission Constants Integration

**Files:**
- Modify: `src/main/java/com/manacommunity/api/model/AuditLog.java` (find `AuditModule` enum)
- Modify: `src/main/java/com/manacommunity/api/model/AuditLog.java` (find `AuditAction` enum)
- Modify: `src/main/java/com/manacommunity/api/constants/PermissionConstants.java`

**Interfaces:**
- Consumes: Existing audit and permission patterns
- Produces: `AuditModule.SERVICE_PLATFORM`, new `AuditAction` values, new permission strings used in controllers (Tasks 9-11)

- [ ] **Step 1: Find and examine the AuditModule and AuditAction enum locations**

Run: `grep -rn "enum AuditModule" src/main/java/ | head -3`
Run: `grep -rn "enum AuditAction" src/main/java/ | head -3`
Run: `grep -rn "class PermissionConstants" src/main/java/ | head -3`

Read each file to see the current enum values.

- [ ] **Step 2: Add SERVICE_PLATFORM to AuditModule**

Add `SERVICE_PLATFORM` to the `AuditModule` enum. Exact location depends on Step 1's findings. The enum should now include:

```java
SERVICE_PLATFORM
```

- [ ] **Step 3: Add service platform actions to AuditAction**

Add to the `AuditAction` enum:

```java
SERVICE_DOMAIN_CREATED,
SERVICE_DOMAIN_UPDATED,
SERVICE_DOMAIN_DELETED,
SERVICE_CATEGORY_CREATED,
SERVICE_CATEGORY_UPDATED,
SERVICE_CATEGORY_DELETED,
SERVICE_PROVIDER_REGISTERED,
SERVICE_PROVIDER_VERIFIED,
SERVICE_PROVIDER_SUSPENDED,
SERVICE_OFFERING_CREATED,
SERVICE_OFFERING_UPDATED,
SERVICE_OFFERING_DELETED,
SERVICE_REQUEST_CREATED,
SERVICE_REQUEST_SUBMITTED,
SERVICE_REQUEST_ASSIGNED,
SERVICE_REQUEST_CANCELLED,
SERVICE_WORK_ORDER_CREATED,
SERVICE_WORK_ORDER_STATUS_UPDATED,
SERVICE_WORK_ORDER_COMPLETED
```

- [ ] **Step 4: Add permission constants**

Add to `PermissionConstants.java`:

```java
public static final String VIEW_SERVICE_CATALOG = "View Service Catalog";
public static final String MANAGE_SERVICE_CATALOG = "Manage Service Catalog";
public static final String VIEW_SERVICE_PROVIDERS = "View Service Providers";
public static final String MANAGE_SERVICE_PROVIDERS = "Manage Service Providers";
public static final String CREATE_SERVICE_REQUEST = "Create Service Request";
public static final String VIEW_SERVICE_REQUESTS = "View Service Requests";
public static final String MANAGE_SERVICE_REQUESTS = "Manage Service Requests";
public static final String VIEW_WORK_ORDERS = "View Work Orders";
public static final String MANAGE_WORK_ORDERS = "Manage Work Orders";
```

- [ ] **Step 5: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/manacommunity/api/model/ src/main/java/com/manacommunity/api/constants/
git commit -m "feat(csp): add audit module/actions and permission constants for service platform"
```

---

### Task 5: ServiceCatalogService + Unit Tests

**Files:**
- Create: `serviceplatform/service/ServiceCatalogService.java`
- Create: `unit/service/serviceplatform/ServiceCatalogServiceTest.java`

**Interfaces:**
- Consumes: `ServiceDomainRepository`, `ServiceCategoryRepository`, `AuditService`, request/response DTOs
- Produces: `ServiceCatalogService` with methods:
  - `ServiceDomainResponse createDomain(CreateServiceDomainRequest req, Long communityId)` — creates domain
  - `ServiceDomainResponse updateDomain(Long id, CreateServiceDomainRequest req)` — updates domain
  - `void deleteDomain(Long id)` — soft-deletes domain
  - `List<ServiceDomainResponse> listDomains(Long communityId)` — lists active domains for community
  - `ServiceDomainResponse getDomain(Long id)` — gets domain with categories
  - `ServiceCategoryResponse createCategory(CreateServiceCategoryRequest req)` — creates category
  - `ServiceCategoryResponse updateCategory(Long id, CreateServiceCategoryRequest req)` — updates category
  - `void deleteCategory(Long id)` — soft-deletes category
  - `List<ServiceCategoryResponse> listCategories(Long domainId)` — lists active categories for domain
  - `ServiceCategoryResponse getCategory(Long id)` — gets category with sub-categories

- [ ] **Step 1: Write the failing tests**

```java
// src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceCatalogServiceTest.java
package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.model.AuditLog.AuditAction;
import com.manacommunity.api.model.AuditLog.AuditModule;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceDomainRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceDomainResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceDomainRepository;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import com.manacommunity.api.service.impl.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCatalogService")
class ServiceCatalogServiceTest {

    @Mock
    private ServiceDomainRepository domainRepository;

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ServiceCatalogService catalogService;

    @Nested
    @DisplayName("createDomain")
    class CreateDomain {

        @Test
        @DisplayName("creates domain and returns response")
        void createsDomainSuccessfully() {
            CreateServiceDomainRequest req = new CreateServiceDomainRequest();
            req.setName("Home Services");
            req.setSlug("home-services");
            req.setIcon("🏠");
            req.setDescription("All home-related services");
            req.setDisplayOrder(1);

            when(domainRepository.existsBySlugAndCommunityId("home-services", 1L)).thenReturn(false);
            when(domainRepository.save(any(ServiceDomain.class))).thenAnswer(inv -> {
                ServiceDomain d = inv.getArgument(0);
                d.setId(10L);
                d.setCreatedAt(LocalDateTime.now());
                d.setUpdatedAt(LocalDateTime.now());
                return d;
            });

            ServiceDomainResponse resp = catalogService.createDomain(req, 1L);

            assertThat(resp.getName()).isEqualTo("Home Services");
            assertThat(resp.getSlug()).isEqualTo("home-services");
            assertThat(resp.getId()).isEqualTo(10L);
            verify(domainRepository).save(any(ServiceDomain.class));
        }

        @Test
        @DisplayName("rejects duplicate slug within same community")
        void rejectsDuplicateSlug() {
            CreateServiceDomainRequest req = new CreateServiceDomainRequest();
            req.setName("Home Services");
            req.setSlug("home-services");

            when(domainRepository.existsBySlugAndCommunityId("home-services", 1L)).thenReturn(true);

            assertThatThrownBy(() -> catalogService.createDomain(req, 1L))
                    .isInstanceOf(RuntimeException.class);
            verify(domainRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listDomains")
    class ListDomains {

        @Test
        @DisplayName("returns active domains for community")
        void returnsDomainsForCommunity() {
            ServiceDomain d = ServiceDomain.builder()
                    .id(1L).name("Home Services").slug("home-services")
                    .active(true).displayOrder(0)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(domainRepository.findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(1L))
                    .thenReturn(List.of(d));

            List<ServiceDomainResponse> result = catalogService.listDomains(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Home Services");
        }
    }

    @Nested
    @DisplayName("deleteDomain")
    class DeleteDomain {

        @Test
        @DisplayName("soft-deletes domain by setting active=false")
        void softDeletesDomain() {
            ServiceDomain d = ServiceDomain.builder().id(1L).name("Test").active(true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(domainRepository.findById(1L)).thenReturn(Optional.of(d));

            catalogService.deleteDomain(1L);

            assertThat(d.isActive()).isFalse();
            verify(domainRepository).save(d);
        }

        @Test
        @DisplayName("throws when domain not found")
        void throwsWhenNotFound() {
            when(domainRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> catalogService.deleteDomain(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("creates category under domain")
        void createsCategorySuccessfully() {
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services")
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(domainRepository.findById(1L)).thenReturn(Optional.of(domain));
            when(categoryRepository.existsBySlugAndDomainId("electrician", 1L)).thenReturn(false);
            when(categoryRepository.save(any(ServiceCategory.class))).thenAnswer(inv -> {
                ServiceCategory c = inv.getArgument(0);
                c.setId(20L);
                c.setCreatedAt(LocalDateTime.now());
                c.setUpdatedAt(LocalDateTime.now());
                return c;
            });

            CreateServiceCategoryRequest req = new CreateServiceCategoryRequest();
            req.setDomainId(1L);
            req.setName("Electrician");
            req.setSlug("electrician");
            req.setIcon("⚡");

            ServiceCategoryResponse resp = catalogService.createCategory(req);

            assertThat(resp.getName()).isEqualTo("Electrician");
            assertThat(resp.getDomainId()).isEqualTo(1L);
            verify(categoryRepository).save(any(ServiceCategory.class));
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -pl . -Dtest="ServiceCatalogServiceTest" -q 2>&1 | tail -10`
Expected: FAIL — `ServiceCatalogService` class does not exist yet

- [ ] **Step 3: Implement ServiceCatalogService**

```java
// serviceplatform/service/ServiceCatalogService.java
package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceDomainRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceDomainResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceDomainRepository;
import com.manacommunity.api.service.impl.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceDomainRepository domainRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AuditService auditService;

    @Transactional
    public ServiceDomainResponse createDomain(CreateServiceDomainRequest req, Long communityId) {
        if (domainRepository.existsBySlugAndCommunityId(req.getSlug(), communityId)) {
            throw new IllegalArgumentException("Domain slug already exists: " + req.getSlug());
        }

        Community community = new Community();
        community.setId(communityId);

        ServiceDomain domain = ServiceDomain.builder()
                .community(community)
                .name(req.getName())
                .slug(req.getSlug())
                .icon(req.getIcon())
                .description(req.getDescription())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .metadata(req.getMetadata())
                .active(true)
                .build();

        domain = domainRepository.save(domain);
        auditService.record("SERVICE_DOMAIN_CREATED", "SERVICE_PLATFORM",
                "ServiceDomain", String.valueOf(domain.getId()));
        return toResponse(domain);
    }

    @Transactional
    public ServiceDomainResponse updateDomain(Long id, CreateServiceDomainRequest req) {
        ServiceDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", id));

        domain.setName(req.getName());
        domain.setSlug(req.getSlug());
        domain.setIcon(req.getIcon());
        domain.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) {
            domain.setDisplayOrder(req.getDisplayOrder());
        }
        domain.setMetadata(req.getMetadata());

        domain = domainRepository.save(domain);
        auditService.record("SERVICE_DOMAIN_UPDATED", "SERVICE_PLATFORM",
                "ServiceDomain", String.valueOf(domain.getId()));
        return toResponse(domain);
    }

    @Transactional
    public void deleteDomain(Long id) {
        ServiceDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", id));
        domain.setActive(false);
        domainRepository.save(domain);
        auditService.record("SERVICE_DOMAIN_DELETED", "SERVICE_PLATFORM",
                "ServiceDomain", String.valueOf(id));
    }

    @Transactional(readOnly = true)
    public List<ServiceDomainResponse> listDomains(Long communityId) {
        List<ServiceDomain> domains;
        if (communityId != null) {
            domains = domainRepository.findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(communityId);
        } else {
            domains = domainRepository.findByCommunityIdIsNullAndActiveTrueOrderByDisplayOrderAsc();
        }
        return domains.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceDomainResponse getDomain(Long id) {
        ServiceDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", id));
        ServiceDomainResponse resp = toResponse(domain);
        resp.setCategories(domain.getCategories().stream()
                .filter(ServiceCategory::isActive)
                .map(this::toCategoryResponse)
                .collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public ServiceCategoryResponse createCategory(CreateServiceCategoryRequest req) {
        ServiceDomain domain = domainRepository.findById(req.getDomainId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", req.getDomainId()));

        if (categoryRepository.existsBySlugAndDomainId(req.getSlug(), req.getDomainId())) {
            throw new IllegalArgumentException("Category slug already exists in this domain: " + req.getSlug());
        }

        ServiceCategory parent = null;
        if (req.getParentCategoryId() != null) {
            parent = categoryRepository.findById(req.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", req.getParentCategoryId()));
        }

        ServiceCategory category = ServiceCategory.builder()
                .domain(domain)
                .parentCategory(parent)
                .name(req.getName())
                .slug(req.getSlug())
                .icon(req.getIcon())
                .description(req.getDescription())
                .requiredCertifications(req.getRequiredCertifications())
                .customFields(req.getCustomFields())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .active(true)
                .build();

        category = categoryRepository.save(category);
        auditService.record("SERVICE_CATEGORY_CREATED", "SERVICE_PLATFORM",
                "ServiceCategory", String.valueOf(category.getId()));
        return toCategoryResponse(category);
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long id, CreateServiceCategoryRequest req) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", id));

        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setIcon(req.getIcon());
        category.setDescription(req.getDescription());
        category.setRequiredCertifications(req.getRequiredCertifications());
        category.setCustomFields(req.getCustomFields());
        if (req.getDisplayOrder() != null) {
            category.setDisplayOrder(req.getDisplayOrder());
        }

        category = categoryRepository.save(category);
        auditService.record("SERVICE_CATEGORY_UPDATED", "SERVICE_PLATFORM",
                "ServiceCategory", String.valueOf(category.getId()));
        return toCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", id));
        category.setActive(false);
        categoryRepository.save(category);
        auditService.record("SERVICE_CATEGORY_DELETED", "SERVICE_PLATFORM",
                "ServiceCategory", String.valueOf(id));
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listCategories(Long domainId) {
        return categoryRepository.findByDomainIdAndParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc(domainId)
                .stream()
                .map(this::toCategoryResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse getCategory(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", id));
        return toCategoryResponseWithChildren(category);
    }

    private ServiceDomainResponse toResponse(ServiceDomain domain) {
        return ServiceDomainResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .slug(domain.getSlug())
                .icon(domain.getIcon())
                .description(domain.getDescription())
                .displayOrder(domain.getDisplayOrder())
                .active(domain.isActive())
                .metadata(domain.getMetadata())
                .categoryCount(domain.getCategories() != null ? (int) domain.getCategories().stream().filter(ServiceCategory::isActive).count() : 0)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory cat) {
        return ServiceCategoryResponse.builder()
                .id(cat.getId())
                .domainId(cat.getDomain() != null ? cat.getDomain().getId() : null)
                .domainName(cat.getDomain() != null ? cat.getDomain().getName() : null)
                .parentCategoryId(cat.getParentCategory() != null ? cat.getParentCategory().getId() : null)
                .parentCategoryName(cat.getParentCategory() != null ? cat.getParentCategory().getName() : null)
                .name(cat.getName())
                .slug(cat.getSlug())
                .icon(cat.getIcon())
                .description(cat.getDescription())
                .requiredCertifications(cat.getRequiredCertifications())
                .customFields(cat.getCustomFields())
                .displayOrder(cat.getDisplayOrder())
                .active(cat.isActive())
                .createdAt(cat.getCreatedAt())
                .updatedAt(cat.getUpdatedAt())
                .build();
    }

    private ServiceCategoryResponse toCategoryResponseWithChildren(ServiceCategory cat) {
        ServiceCategoryResponse resp = toCategoryResponse(cat);
        if (cat.getSubCategories() != null && !cat.getSubCategories().isEmpty()) {
            resp.setSubCategories(cat.getSubCategories().stream()
                    .filter(ServiceCategory::isActive)
                    .map(this::toCategoryResponseWithChildren)
                    .collect(Collectors.toList()));
        } else {
            resp.setSubCategories(Collections.emptyList());
        }
        return resp;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest="ServiceCatalogServiceTest" -q 2>&1 | tail -10`
Expected: All tests PASS

Note: If the `auditService.record()` method signature doesn't match (it may use enum types instead of Strings), adjust the calls to use the actual enum constants: `AuditAction.SERVICE_DOMAIN_CREATED` and `AuditModule.SERVICE_PLATFORM`. Check the existing `AuditService` signature first.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/service/ServiceCatalogService.java \
        src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceCatalogServiceTest.java
git commit -m "feat(csp): add ServiceCatalogService with domain/category CRUD and tests"
```

---

### Task 6: ServiceProviderService + Unit Tests

**Files:**
- Create: `serviceplatform/service/ServiceProviderService.java`
- Create: `unit/service/serviceplatform/ServiceProviderServiceTest.java`

**Interfaces:**
- Consumes: `ServiceProviderRepository`, `ProviderServiceOfferingRepository`, `ServiceCategoryRepository`, `AuditService`, DTOs
- Produces: `ServiceProviderService` with methods:
  - `ServiceProviderResponse register(RegisterProviderRequest req, AppUser user)` — registers a new provider
  - `ServiceProviderResponse getProfile(Long userId)` — gets provider profile by user id
  - `ServiceProviderResponse updateProfile(Long userId, RegisterProviderRequest req)` — updates provider profile
  - `Page<ServiceProviderResponse> listProviders(Long communityId, String status, int page, int size)` — lists providers with filters
  - `ServiceProviderResponse verifyProvider(Long providerId, String action)` — approve/reject/suspend a provider
  - `ServiceOfferingResponse createOffering(Long userId, CreateOfferingRequest req)` — creates an offering
  - `ServiceOfferingResponse updateOffering(Long userId, Long offeringId, CreateOfferingRequest req)` — updates an offering
  - `void deleteOffering(Long userId, Long offeringId)` — deletes an offering
  - `List<ServiceOfferingResponse> listMyOfferings(Long userId)` — lists provider's own offerings

- [ ] **Step 1: Write the failing tests**

```java
// src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceProviderServiceTest.java
package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.dto.request.CreateOfferingRequest;
import com.manacommunity.api.serviceplatform.dto.request.RegisterProviderRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.service.impl.AuditService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceProviderService")
class ServiceProviderServiceTest {

    @Mock private ServiceProviderRepository providerRepository;
    @Mock private ProviderServiceOfferingRepository offeringRepository;
    @Mock private ServiceCategoryRepository categoryRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ServiceProviderService providerService;

    private AppUser testUser() {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setFullName("Test User");
        Community c = new Community();
        c.setId(100L);
        u.setCommunity(c);
        return u;
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("registers a new individual provider")
        void registersSuccessfully() {
            AppUser user = testUser();
            when(providerRepository.existsByUserId(1L)).thenReturn(false);
            when(providerRepository.save(any(ServiceProvider.class))).thenAnswer(inv -> {
                ServiceProvider p = inv.getArgument(0);
                p.setId(10L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            RegisterProviderRequest req = new RegisterProviderRequest();
            req.setProviderType("INDIVIDUAL");
            req.setPhone("9876543210");
            req.setEmail("provider@test.com");
            req.setBio("Experienced plumber");

            ServiceProviderResponse resp = providerService.register(req, user);

            assertThat(resp.getProviderType()).isEqualTo("INDIVIDUAL");
            assertThat(resp.getVerificationStatus()).isEqualTo("PENDING");
            verify(providerRepository).save(any(ServiceProvider.class));
        }

        @Test
        @DisplayName("rejects duplicate registration")
        void rejectsDuplicate() {
            AppUser user = testUser();
            when(providerRepository.existsByUserId(1L)).thenReturn(true);

            RegisterProviderRequest req = new RegisterProviderRequest();
            req.setProviderType("INDIVIDUAL");

            assertThatThrownBy(() -> providerService.register(req, user))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("verifyProvider")
    class VerifyProvider {

        @Test
        @DisplayName("approves a pending provider")
        void approvesProvider() {
            ServiceProvider provider = ServiceProvider.builder()
                    .id(10L).verificationStatus(VerificationStatus.PENDING)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(providerRepository.save(any())).thenReturn(provider);

            ServiceProviderResponse resp = providerService.verifyProvider(10L, "VERIFIED");

            assertThat(resp.getVerificationStatus()).isEqualTo("VERIFIED");
        }
    }

    @Nested
    @DisplayName("createOffering")
    class CreateOffering {

        @Test
        @DisplayName("creates offering for verified provider")
        void createsOfferingSuccessfully() {
            ServiceProvider provider = ServiceProvider.builder()
                    .id(10L).verificationStatus(VerificationStatus.VERIFIED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home").build();
            ServiceCategory category = ServiceCategory.builder()
                    .id(5L).name("Electrician").domain(domain)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(offeringRepository.save(any(ProviderServiceOffering.class))).thenAnswer(inv -> {
                ProviderServiceOffering o = inv.getArgument(0);
                o.setId(20L);
                o.setCreatedAt(LocalDateTime.now());
                o.setUpdatedAt(LocalDateTime.now());
                return o;
            });

            CreateOfferingRequest req = new CreateOfferingRequest();
            req.setCategoryId(5L);
            req.setTitle("Wiring & Repair");
            req.setBasePrice(BigDecimal.valueOf(500));
            req.setPricingUnit("FLAT");

            ServiceOfferingResponse resp = providerService.createOffering(1L, req);

            assertThat(resp.getTitle()).isEqualTo("Wiring & Repair");
            assertThat(resp.getCategoryName()).isEqualTo("Electrician");
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -pl . -Dtest="ServiceProviderServiceTest" -q 2>&1 | tail -10`
Expected: FAIL — `ServiceProviderService` class does not exist yet

- [ ] **Step 3: Implement ServiceProviderService**

```java
// serviceplatform/service/ServiceProviderService.java
package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.serviceplatform.dto.request.CreateOfferingRequest;
import com.manacommunity.api.serviceplatform.dto.request.RegisterProviderRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.service.impl.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceProviderService {

    private final ServiceProviderRepository providerRepository;
    private final ProviderServiceOfferingRepository offeringRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AuditService auditService;

    @Transactional
    public ServiceProviderResponse register(RegisterProviderRequest req, AppUser user) {
        if (providerRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException("User is already registered as a service provider");
        }

        ServiceProvider provider = ServiceProvider.builder()
                .user(user)
                .community(user.getCommunity())
                .providerType(parseEnum(ProviderType.class, req.getProviderType()))
                .businessName(req.getBusinessName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .bio(req.getBio())
                .profileImageUrl(req.getProfileImageUrl())
                .serviceAreas(req.getServiceAreas())
                .certifications(req.getCertifications())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        if (req.getVendorId() != null) {
            Vendor vendor = new Vendor();
            vendor.setId(req.getVendorId());
            provider.setVendor(vendor);
        }

        provider = providerRepository.save(provider);
        auditService.record("SERVICE_PROVIDER_REGISTERED", "SERVICE_PLATFORM",
                "ServiceProvider", String.valueOf(provider.getId()));
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public ServiceProviderResponse getProfile(Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        ServiceProviderResponse resp = toResponse(provider);
        resp.setOfferings(provider.getOfferings().stream()
                .map(this::toOfferingResponse)
                .collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public ServiceProviderResponse updateProfile(Long userId, RegisterProviderRequest req) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        provider.setBusinessName(req.getBusinessName());
        provider.setPhone(req.getPhone());
        provider.setEmail(req.getEmail());
        provider.setBio(req.getBio());
        provider.setProfileImageUrl(req.getProfileImageUrl());
        provider.setServiceAreas(req.getServiceAreas());
        provider.setCertifications(req.getCertifications());

        provider = providerRepository.save(provider);
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public Page<ServiceProviderResponse> listProviders(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        Page<ServiceProvider> providers;
        if (status != null && !status.isBlank()) {
            VerificationStatus vs = parseEnum(VerificationStatus.class, status);
            providers = providerRepository.findByCommunityIdAndVerificationStatus(communityId, vs, pageable);
        } else {
            providers = providerRepository.findByCommunityIdAndVerificationStatusNot(
                    communityId, VerificationStatus.SUSPENDED, pageable);
        }
        return providers.map(this::toResponse);
    }

    @Transactional
    public ServiceProviderResponse verifyProvider(Long providerId, String action) {
        ServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", providerId));

        VerificationStatus newStatus = parseEnum(VerificationStatus.class, action);
        provider.setVerificationStatus(newStatus);
        provider = providerRepository.save(provider);

        auditService.record("SERVICE_PROVIDER_VERIFIED", "SERVICE_PLATFORM",
                "ServiceProvider", String.valueOf(providerId));
        return toResponse(provider);
    }

    @Transactional
    public ServiceOfferingResponse createOffering(Long userId, CreateOfferingRequest req) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        if (provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new UnauthorizedActionException("Only verified providers can create offerings");
        }

        ServiceCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", req.getCategoryId()));

        ProviderServiceOffering offering = ProviderServiceOffering.builder()
                .provider(provider)
                .category(category)
                .title(req.getTitle())
                .description(req.getDescription())
                .basePrice(req.getBasePrice())
                .pricingUnit(parseEnum(PricingUnit.class, req.getPricingUnit()))
                .estimatedDurationMinutes(req.getEstimatedDurationMinutes())
                .minOrderValue(req.getMinOrderValue())
                .customFieldValues(req.getCustomFieldValues())
                .tags(req.getTags())
                .available(true)
                .build();

        offering = offeringRepository.save(offering);
        auditService.record("SERVICE_OFFERING_CREATED", "SERVICE_PLATFORM",
                "ProviderServiceOffering", String.valueOf(offering.getId()));
        return toOfferingResponse(offering);
    }

    @Transactional
    public ServiceOfferingResponse updateOffering(Long userId, Long offeringId, CreateOfferingRequest req) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ProviderServiceOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderServiceOffering", offeringId));

        if (!offering.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("Cannot modify another provider's offering");
        }

        offering.setTitle(req.getTitle());
        offering.setDescription(req.getDescription());
        offering.setBasePrice(req.getBasePrice());
        offering.setPricingUnit(parseEnum(PricingUnit.class, req.getPricingUnit()));
        offering.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        offering.setMinOrderValue(req.getMinOrderValue());
        offering.setCustomFieldValues(req.getCustomFieldValues());
        offering.setTags(req.getTags());

        offering = offeringRepository.save(offering);
        auditService.record("SERVICE_OFFERING_UPDATED", "SERVICE_PLATFORM",
                "ProviderServiceOffering", String.valueOf(offering.getId()));
        return toOfferingResponse(offering);
    }

    @Transactional
    public void deleteOffering(Long userId, Long offeringId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ProviderServiceOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderServiceOffering", offeringId));

        if (!offering.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("Cannot delete another provider's offering");
        }

        offeringRepository.delete(offering);
        auditService.record("SERVICE_OFFERING_DELETED", "SERVICE_PLATFORM",
                "ProviderServiceOffering", String.valueOf(offeringId));
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> listMyOfferings(Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        return offeringRepository.findByProviderId(provider.getId()).stream()
                .map(this::toOfferingResponse)
                .collect(Collectors.toList());
    }

    private ServiceProviderResponse toResponse(ServiceProvider p) {
        return ServiceProviderResponse.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .userName(p.getUser() != null ? p.getUser().getFullName() : null)
                .vendorId(p.getVendor() != null ? p.getVendor().getId() : null)
                .providerType(p.getProviderType().name())
                .businessName(p.getBusinessName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .bio(p.getBio())
                .profileImageUrl(p.getProfileImageUrl())
                .verificationStatus(p.getVerificationStatus().name())
                .avgRating(p.getAvgRating())
                .totalJobsCompleted(p.getTotalJobsCompleted())
                .serviceAreas(p.getServiceAreas())
                .certifications(p.getCertifications())
                .offerings(Collections.emptyList())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ServiceOfferingResponse toOfferingResponse(ProviderServiceOffering o) {
        return ServiceOfferingResponse.builder()
                .id(o.getId())
                .providerId(o.getProvider() != null ? o.getProvider().getId() : null)
                .providerName(o.getProvider() != null && o.getProvider().getUser() != null
                        ? o.getProvider().getUser().getFullName() : null)
                .categoryId(o.getCategory() != null ? o.getCategory().getId() : null)
                .categoryName(o.getCategory() != null ? o.getCategory().getName() : null)
                .title(o.getTitle())
                .description(o.getDescription())
                .basePrice(o.getBasePrice())
                .pricingUnit(o.getPricingUnit().name())
                .estimatedDurationMinutes(o.getEstimatedDurationMinutes())
                .minOrderValue(o.getMinOrderValue())
                .available(o.isAvailable())
                .customFieldValues(o.getCustomFieldValues())
                .tags(o.getTags())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + enumClass.getSimpleName() + ": " + value);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest="ServiceProviderServiceTest" -q 2>&1 | tail -10`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/service/ServiceProviderService.java \
        src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceProviderServiceTest.java
git commit -m "feat(csp): add ServiceProviderService with registration, verification, offerings and tests"
```

---

### Task 7: ServiceRequestService + WorkOrderService + Unit Tests

**Files:**
- Create: `serviceplatform/service/ServiceRequestService.java`
- Create: `serviceplatform/service/WorkOrderService.java`
- Create: `unit/service/serviceplatform/ServiceRequestServiceTest.java`
- Create: `unit/service/serviceplatform/WorkOrderServiceTest.java`

**Interfaces:**
- Consumes: All repositories, `AuditService`, `NotificationService` (existing), DTOs
- Produces: `ServiceRequestService` with methods:
  - `ServiceRequestResponse createRequest(CreateServiceRequestDto req, AppUser user)` — creates a service request
  - `ServiceRequestResponse submitRequest(Long requestId, Long userId)` — moves from DRAFT to SUBMITTED
  - `Page<ServiceRequestResponse> listMyRequests(Long userId, int page, int size)` — resident's requests
  - `ServiceRequestResponse getRequest(Long requestId)` — full request detail with work order
  - `ServiceRequestResponse assignProvider(Long requestId, AssignProviderRequest req)` — admin assigns provider
  - `ServiceRequestResponse cancelRequest(Long requestId, Long userId, String reason)` — cancel a request
  - `Page<ServiceRequestResponse> listRequestsForProvider(Long userId, int page, int size)` — requests assigned to provider
  - `ServiceRequestResponse acceptRequest(Long requestId, Long userId)` — provider accepts
  - `ServiceRequestResponse declineRequest(Long requestId, Long userId)` — provider declines
- Produces: `WorkOrderService` with methods:
  - `WorkOrderResponse getWorkOrder(Long workOrderId)` — get work order detail
  - `WorkOrderResponse updateStatus(Long workOrderId, Long userId, UpdateWorkOrderStatusRequest req)` — update work order status
  - `Page<WorkOrderResponse> listProviderWorkOrders(Long userId, int page, int size)` — provider's work orders
  - `WorkOrderResponse signoffResident(Long workOrderId, Long userId)` — resident signs off on completion

- [ ] **Step 1: Write failing tests for ServiceRequestService**

```java
// src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceRequestServiceTest.java
package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.service.impl.AuditService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceRequestService")
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository requestRepository;
    @Mock private ServiceCategoryRepository categoryRepository;
    @Mock private ServiceProviderRepository providerRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ServiceRequestService requestService;

    private AppUser testUser() {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setFullName("Resident User");
        Community c = new Community();
        c.setId(100L);
        u.setCommunity(c);
        return u;
    }

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        @DisplayName("creates a service request as DRAFT")
        void createsAsDraft() {
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services").build();
            ServiceCategory cat = ServiceCategory.builder().id(5L).name("Electrician").domain(domain)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
            when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> {
                ServiceRequest r = inv.getArgument(0);
                r.setId(50L);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            CreateServiceRequestDto req = new CreateServiceRequestDto();
            req.setCategoryId(5L);
            req.setTitle("Fix wiring in kitchen");
            req.setPreferredDate(LocalDate.of(2026, 8, 1));
            req.setSubmitImmediately(false);

            ServiceRequestResponse resp = requestService.createRequest(req, testUser());

            assertThat(resp.getStatus()).isEqualTo("DRAFT");
            assertThat(resp.getTitle()).isEqualTo("Fix wiring in kitchen");
        }

        @Test
        @DisplayName("creates and immediately submits when submitImmediately=true")
        void createsAndSubmits() {
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services").build();
            ServiceCategory cat = ServiceCategory.builder().id(5L).name("Electrician").domain(domain)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
            when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> {
                ServiceRequest r = inv.getArgument(0);
                r.setId(50L);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            CreateServiceRequestDto req = new CreateServiceRequestDto();
            req.setCategoryId(5L);
            req.setTitle("Fix wiring");
            req.setSubmitImmediately(true);

            ServiceRequestResponse resp = requestService.createRequest(req, testUser());

            assertThat(resp.getStatus()).isEqualTo("SUBMITTED");
        }
    }

    @Nested
    @DisplayName("assignProvider")
    class AssignProvider {

        @Test
        @DisplayName("assigns provider and creates work order")
        void assignsProviderSuccessfully() {
            ServiceRequest request = ServiceRequest.builder()
                    .id(50L).status(ServiceRequestStatus.SUBMITTED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            ServiceProvider provider = ServiceProvider.builder()
                    .id(10L).verificationStatus(VerificationStatus.VERIFIED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(requestRepository.findById(50L)).thenReturn(Optional.of(request));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(requestRepository.save(any())).thenReturn(request);
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> {
                WorkOrder w = inv.getArgument(0);
                w.setId(70L);
                w.setCreatedAt(LocalDateTime.now());
                w.setUpdatedAt(LocalDateTime.now());
                return w;
            });

            AssignProviderRequest req = new AssignProviderRequest();
            req.setProviderId(10L);

            ServiceRequestResponse resp = requestService.assignProvider(50L, req);

            assertThat(resp.getStatus()).isEqualTo("ASSIGNED");
            verify(workOrderRepository).save(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("cancelRequest")
    class CancelRequest {

        @Test
        @DisplayName("cancels a submitted request")
        void cancelsSuccessfully() {
            AppUser user = testUser();
            ServiceRequest request = ServiceRequest.builder()
                    .id(50L).requester(user).status(ServiceRequestStatus.SUBMITTED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(requestRepository.findById(50L)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenReturn(request);

            ServiceRequestResponse resp = requestService.cancelRequest(50L, 1L, "Changed my mind");

            assertThat(resp.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("rejects cancel for completed request")
        void rejectsCancelForCompleted() {
            AppUser user = testUser();
            ServiceRequest request = ServiceRequest.builder()
                    .id(50L).requester(user).status(ServiceRequestStatus.COMPLETED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(requestRepository.findById(50L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.cancelRequest(50L, 1L, "reason"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
```

- [ ] **Step 2: Write failing tests for WorkOrderService**

```java
// src/test/java/com/manacommunity/api/unit/service/serviceplatform/WorkOrderServiceTest.java
package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.serviceplatform.dto.request.UpdateWorkOrderStatusRequest;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.service.impl.AuditService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderService")
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private ServiceProviderRepository providerRepository;
    @Mock private ServiceRequestRepository requestRepository;
    @Mock private AuditService auditService;

    @InjectMocks private WorkOrderService workOrderService;

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("transitions from CREATED to SCHEDULED")
        void transitionsToScheduled() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceProvider provider = ServiceProvider.builder().id(10L).user(user)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.CREATED)
                    .provider(provider)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));
            when(workOrderRepository.save(any())).thenReturn(wo);

            UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
            req.setStatus("SCHEDULED");

            WorkOrderResponse resp = workOrderService.updateStatus(70L, 1L, req);

            assertThat(resp.getStatus()).isEqualTo("SCHEDULED");
        }

        @Test
        @DisplayName("sets actual timestamps on IN_PROGRESS and COMPLETED")
        void setsTimestamps() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceProvider provider = ServiceProvider.builder().id(10L).user(user)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            ServiceRequest sr = ServiceRequest.builder().id(50L)
                    .status(ServiceRequestStatus.IN_PROGRESS)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.ARRIVED)
                    .provider(provider).serviceRequest(sr)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));
            when(workOrderRepository.save(any())).thenReturn(wo);

            UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
            req.setStatus("IN_PROGRESS");

            workOrderService.updateStatus(70L, 1L, req);

            assertThat(wo.getActualStart()).isNotNull();
        }

        @Test
        @DisplayName("rejects invalid transition")
        void rejectsInvalidTransition() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceProvider provider = ServiceProvider.builder().id(10L).user(user)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.COMPLETED)
                    .provider(provider)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));

            UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
            req.setStatus("CREATED");

            assertThatThrownBy(() -> workOrderService.updateStatus(70L, 1L, req))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("signoffResident")
    class SignoffResident {

        @Test
        @DisplayName("marks resident signoff on completed work order")
        void signsOffSuccessfully() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceRequest sr = ServiceRequest.builder().id(50L).requester(user)
                    .status(ServiceRequestStatus.IN_PROGRESS)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.COMPLETED)
                    .serviceRequest(sr)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any())).thenReturn(wo);
            when(requestRepository.save(any())).thenReturn(sr);

            WorkOrderResponse resp = workOrderService.signoffResident(70L, 1L);

            assertThat(resp.isResidentSignoff()).isTrue();
        }
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./mvnw test -pl . -Dtest="ServiceRequestServiceTest,WorkOrderServiceTest" -q 2>&1 | tail -10`
Expected: FAIL — classes don't exist yet

- [ ] **Step 4: Implement ServiceRequestService**

```java
// serviceplatform/service/ServiceRequestService.java
package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.entity.*;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.*;
import com.manacommunity.api.service.impl.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceProviderRepository providerRepository;
    private final ProviderServiceOfferingRepository offeringRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AuditService auditService;

    private static final Set<ServiceRequestStatus> CANCELLABLE = Set.of(
            ServiceRequestStatus.DRAFT, ServiceRequestStatus.SUBMITTED,
            ServiceRequestStatus.MATCHING, ServiceRequestStatus.ASSIGNED);

    @Transactional
    public ServiceRequestResponse createRequest(CreateServiceRequestDto req, AppUser user) {
        ServiceCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", req.getCategoryId()));

        ServiceRequest request = ServiceRequest.builder()
                .requester(user)
                .community(user.getCommunity())
                .category(category)
                .title(req.getTitle())
                .description(req.getDescription())
                .preferredDate(req.getPreferredDate())
                .preferredTimeSlot(req.getPreferredTimeSlot())
                .address(req.getAddress())
                .urgency(req.getUrgency() != null
                        ? parseEnum(ServiceUrgency.class, req.getUrgency())
                        : ServiceUrgency.NORMAL)
                .estimatedCost(req.getEstimatedCost())
                .customFieldValues(req.getCustomFieldValues())
                .attachments(req.getAttachments())
                .status(req.isSubmitImmediately() ? ServiceRequestStatus.SUBMITTED : ServiceRequestStatus.DRAFT)
                .build();

        request = requestRepository.save(request);
        auditService.record("SERVICE_REQUEST_CREATED", "SERVICE_PLATFORM",
                "ServiceRequest", String.valueOf(request.getId()));
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse submitRequest(Long requestId, Long userId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new UnauthorizedActionException("Cannot submit another user's request");
        }
        if (request.getStatus() != ServiceRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT requests can be submitted");
        }

        request.setStatus(ServiceRequestStatus.SUBMITTED);
        request = requestRepository.save(request);
        auditService.record("SERVICE_REQUEST_SUBMITTED", "SERVICE_PLATFORM",
                "ServiceRequest", String.valueOf(requestId));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listMyRequests(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getRequest(Long requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));
        ServiceRequestResponse resp = toResponse(request);
        workOrderRepository.findByServiceRequestId(requestId)
                .ifPresent(wo -> resp.setWorkOrder(toWorkOrderResponse(wo)));
        return resp;
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listAllRequests(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            return requestRepository.findByCommunityIdAndStatus(communityId,
                    parseEnum(ServiceRequestStatus.class, status), pageable).map(this::toResponse);
        }
        return requestRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ServiceRequestResponse assignProvider(Long requestId, AssignProviderRequest req) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (request.getStatus() != ServiceRequestStatus.SUBMITTED
                && request.getStatus() != ServiceRequestStatus.MATCHING) {
            throw new IllegalStateException("Request must be in SUBMITTED or MATCHING status to assign");
        }

        ServiceProvider provider = providerRepository.findById(req.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", req.getProviderId()));

        if (provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Provider must be verified");
        }

        request.setAssignedProvider(provider);
        if (req.getOfferingId() != null) {
            ProviderServiceOffering offering = offeringRepository.findById(req.getOfferingId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProviderServiceOffering", req.getOfferingId()));
            request.setAssignedOffering(offering);
            request.setEstimatedCost(offering.getBasePrice());
        }
        request.setStatus(ServiceRequestStatus.ASSIGNED);
        request = requestRepository.save(request);

        WorkOrder workOrder = WorkOrder.builder()
                .serviceRequest(request)
                .provider(provider)
                .community(request.getCommunity())
                .status(WorkOrderStatus.CREATED)
                .build();
        workOrderRepository.save(workOrder);

        auditService.record("SERVICE_REQUEST_ASSIGNED", "SERVICE_PLATFORM",
                "ServiceRequest", String.valueOf(requestId));
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse cancelRequest(Long requestId, Long userId, String reason) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new UnauthorizedActionException("Cannot cancel another user's request");
        }

        if (!CANCELLABLE.contains(request.getStatus())) {
            throw new IllegalStateException("Cannot cancel a request in status: " + request.getStatus());
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setCancellationReason(reason);
        request = requestRepository.save(request);

        auditService.record("SERVICE_REQUEST_CANCELLED", "SERVICE_PLATFORM",
                "ServiceRequest", String.valueOf(requestId));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listRequestsForProvider(Long userId, int page, int size) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return requestRepository.findByAssignedProviderIdAndStatusIn(provider.getId(),
                List.of(ServiceRequestStatus.ASSIGNED, ServiceRequestStatus.IN_PROGRESS), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ServiceRequestResponse acceptRequest(Long requestId, Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (!request.getAssignedProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("This request is not assigned to you");
        }

        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request = requestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse declineRequest(Long requestId, Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (!request.getAssignedProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("This request is not assigned to you");
        }

        request.setAssignedProvider(null);
        request.setAssignedOffering(null);
        request.setStatus(ServiceRequestStatus.SUBMITTED);
        request = requestRepository.save(request);

        workOrderRepository.findByServiceRequestId(requestId).ifPresent(workOrderRepository::delete);

        return toResponse(request);
    }

    private ServiceRequestResponse toResponse(ServiceRequest r) {
        return ServiceRequestResponse.builder()
                .id(r.getId())
                .requesterId(r.getRequester() != null ? r.getRequester().getId() : null)
                .requesterName(r.getRequester() != null ? r.getRequester().getFullName() : null)
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .domainName(r.getCategory() != null && r.getCategory().getDomain() != null
                        ? r.getCategory().getDomain().getName() : null)
                .title(r.getTitle())
                .description(r.getDescription())
                .preferredDate(r.getPreferredDate())
                .preferredTimeSlot(r.getPreferredTimeSlot())
                .address(r.getAddress())
                .urgency(r.getUrgency() != null ? r.getUrgency().name() : null)
                .status(r.getStatus().name())
                .assignedProviderId(r.getAssignedProvider() != null ? r.getAssignedProvider().getId() : null)
                .assignedProviderName(r.getAssignedProvider() != null && r.getAssignedProvider().getUser() != null
                        ? r.getAssignedProvider().getUser().getFullName() : null)
                .assignedOfferingId(r.getAssignedOffering() != null ? r.getAssignedOffering().getId() : null)
                .estimatedCost(r.getEstimatedCost())
                .actualCost(r.getActualCost())
                .customFieldValues(r.getCustomFieldValues())
                .attachments(r.getAttachments())
                .cancellationReason(r.getCancellationReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private WorkOrderResponse toWorkOrderResponse(WorkOrder wo) {
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .serviceRequestId(wo.getServiceRequest() != null ? wo.getServiceRequest().getId() : null)
                .providerId(wo.getProvider() != null ? wo.getProvider().getId() : null)
                .providerName(wo.getProvider() != null && wo.getProvider().getUser() != null
                        ? wo.getProvider().getUser().getFullName() : null)
                .status(wo.getStatus().name())
                .scheduledStart(wo.getScheduledStart())
                .scheduledEnd(wo.getScheduledEnd())
                .actualStart(wo.getActualStart())
                .actualEnd(wo.getActualEnd())
                .notes(wo.getNotes())
                .checklistItems(wo.getChecklistItems())
                .materialsUsed(wo.getMaterialsUsed())
                .beforePhotos(wo.getBeforePhotos())
                .afterPhotos(wo.getAfterPhotos())
                .residentSignoff(wo.isResidentSignoff())
                .residentSignoffAt(wo.getResidentSignoffAt())
                .providerSignoff(wo.isProviderSignoff())
                .providerSignoffAt(wo.getProviderSignoffAt())
                .invoiceId(wo.getInvoice() != null ? wo.getInvoice().getId() : null)
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + enumClass.getSimpleName() + ": " + value);
        }
    }
}
```

- [ ] **Step 5: Implement WorkOrderService**

```java
// serviceplatform/service/WorkOrderService.java
package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.serviceplatform.dto.request.UpdateWorkOrderStatusRequest;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.service.impl.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final ServiceProviderRepository providerRepository;
    private final ServiceRequestRepository requestRepository;
    private final AuditService auditService;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> VALID_TRANSITIONS = Map.of(
            WorkOrderStatus.CREATED, Set.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.SCHEDULED, Set.of(WorkOrderStatus.EN_ROUTE, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.EN_ROUTE, Set.of(WorkOrderStatus.ARRIVED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.ARRIVED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.PAUSED, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.PAUSED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED)
    );

    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrder(Long workOrderId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long workOrderId, Long userId, UpdateWorkOrderStatusRequest req) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        if (!wo.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("Cannot update another provider's work order");
        }

        WorkOrderStatus newStatus = parseEnum(WorkOrderStatus.class, req.getStatus());
        Set<WorkOrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(wo.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + wo.getStatus() + " to " + newStatus);
        }

        wo.setStatus(newStatus);

        if (newStatus == WorkOrderStatus.IN_PROGRESS && wo.getActualStart() == null) {
            wo.setActualStart(LocalDateTime.now());
        }
        if (newStatus == WorkOrderStatus.COMPLETED) {
            wo.setActualEnd(LocalDateTime.now());
            wo.setProviderSignoff(true);
            wo.setProviderSignoffAt(LocalDateTime.now());
            if (wo.getServiceRequest() != null) {
                wo.getServiceRequest().setStatus(ServiceRequestStatus.COMPLETED);
                requestRepository.save(wo.getServiceRequest());
            }
        }

        if (req.getNotes() != null) wo.setNotes(req.getNotes());
        if (req.getChecklistItems() != null) wo.setChecklistItems(req.getChecklistItems());
        if (req.getMaterialsUsed() != null) wo.setMaterialsUsed(req.getMaterialsUsed());
        if (req.getBeforePhotos() != null) wo.setBeforePhotos(req.getBeforePhotos());
        if (req.getAfterPhotos() != null) wo.setAfterPhotos(req.getAfterPhotos());

        wo = workOrderRepository.save(wo);
        auditService.record("SERVICE_WORK_ORDER_STATUS_UPDATED", "SERVICE_PLATFORM",
                "WorkOrder", String.valueOf(workOrderId));
        return toResponse(wo);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> listProviderWorkOrders(Long userId, int page, int size) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return workOrderRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> listAllWorkOrders(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            return workOrderRepository.findByCommunityIdAndStatus(communityId,
                    parseEnum(WorkOrderStatus.class, status), pageable).map(this::toResponse);
        }
        return workOrderRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public WorkOrderResponse signoffResident(Long workOrderId, Long userId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        if (wo.getServiceRequest() == null
                || !wo.getServiceRequest().getRequester().getId().equals(userId)) {
            throw new UnauthorizedActionException("Only the requesting resident can sign off");
        }

        if (wo.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new IllegalStateException("Work order must be COMPLETED before resident signoff");
        }

        wo.setResidentSignoff(true);
        wo.setResidentSignoffAt(LocalDateTime.now());
        wo = workOrderRepository.save(wo);

        ServiceRequest sr = wo.getServiceRequest();
        sr.setStatus(ServiceRequestStatus.COMPLETED);
        requestRepository.save(sr);

        auditService.record("SERVICE_WORK_ORDER_COMPLETED", "SERVICE_PLATFORM",
                "WorkOrder", String.valueOf(workOrderId));
        return toResponse(wo);
    }

    private WorkOrderResponse toResponse(WorkOrder wo) {
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .serviceRequestId(wo.getServiceRequest() != null ? wo.getServiceRequest().getId() : null)
                .providerId(wo.getProvider() != null ? wo.getProvider().getId() : null)
                .providerName(wo.getProvider() != null && wo.getProvider().getUser() != null
                        ? wo.getProvider().getUser().getFullName() : null)
                .status(wo.getStatus().name())
                .scheduledStart(wo.getScheduledStart())
                .scheduledEnd(wo.getScheduledEnd())
                .actualStart(wo.getActualStart())
                .actualEnd(wo.getActualEnd())
                .notes(wo.getNotes())
                .checklistItems(wo.getChecklistItems())
                .materialsUsed(wo.getMaterialsUsed())
                .beforePhotos(wo.getBeforePhotos())
                .afterPhotos(wo.getAfterPhotos())
                .residentSignoff(wo.isResidentSignoff())
                .residentSignoffAt(wo.getResidentSignoffAt())
                .providerSignoff(wo.isProviderSignoff())
                .providerSignoffAt(wo.getProviderSignoffAt())
                .invoiceId(wo.getInvoice() != null ? wo.getInvoice().getId() : null)
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + enumClass.getSimpleName() + ": " + value);
        }
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest="ServiceRequestServiceTest,WorkOrderServiceTest" -q 2>&1 | tail -10`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/service/ServiceRequestService.java \
        src/main/java/com/manacommunity/api/serviceplatform/service/WorkOrderService.java \
        src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceRequestServiceTest.java \
        src/test/java/com/manacommunity/api/unit/service/serviceplatform/WorkOrderServiceTest.java
git commit -m "feat(csp): add ServiceRequestService and WorkOrderService with lifecycle management and tests"
```

---

### Task 8: ServiceSearchService + Unit Tests

**Files:**
- Create: `serviceplatform/service/ServiceSearchService.java`
- Create: `unit/service/serviceplatform/ServiceSearchServiceTest.java`

**Interfaces:**
- Consumes: `ProviderServiceOfferingRepository`, `ServiceCategoryRepository`, `ServiceProviderRepository`
- Produces: `ServiceSearchService` with methods:
  - `Page<ServiceSearchResult> search(Long communityId, String query, Long domainId, Long categoryId, int page, int size)` — searches offerings across providers

- [ ] **Step 1: Write failing test**

```java
// src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceSearchServiceTest.java
package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.serviceplatform.dto.response.ServiceSearchResult;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.service.ServiceSearchService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceSearchService")
class ServiceSearchServiceTest {

    @Mock private ProviderServiceOfferingRepository offeringRepository;
    @Mock private ServiceCategoryRepository categoryRepository;

    @InjectMocks private ServiceSearchService searchService;

    @Test
    @DisplayName("searches offerings by category and community")
    void searchesByCategoryAndCommunity() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setFullName("Provider User");

        ServiceProvider provider = ServiceProvider.builder()
                .id(10L).providerType(ProviderType.INDIVIDUAL)
                .verificationStatus(VerificationStatus.VERIFIED)
                .avgRating(BigDecimal.valueOf(4.5)).totalJobsCompleted(20)
                .user(user)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services").build();
        ServiceCategory category = ServiceCategory.builder()
                .id(5L).name("Electrician").domain(domain)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ProviderServiceOffering offering = ProviderServiceOffering.builder()
                .id(20L).provider(provider).category(category)
                .title("Wiring & Repair").basePrice(BigDecimal.valueOf(500))
                .pricingUnit(PricingUnit.FLAT).available(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(offeringRepository.findVerifiedByCategoryAndCommunity(eq(5L), eq(100L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(offering)));

        Page<ServiceSearchResult> results = searchService.search(100L, null, null, 5L, 0, 20);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getOfferingTitle()).isEqualTo("Wiring & Repair");
        assertThat(results.getContent().get(0).getProviderRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl . -Dtest="ServiceSearchServiceTest" -q 2>&1 | tail -10`
Expected: FAIL

- [ ] **Step 3: Implement ServiceSearchService**

```java
// serviceplatform/service/ServiceSearchService.java
package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.serviceplatform.dto.response.ServiceSearchResult;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceSearchService {

    private final ProviderServiceOfferingRepository offeringRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ServiceSearchResult> search(Long communityId, String query, Long domainId,
                                             Long categoryId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("provider.avgRating").descending());

        Page<ProviderServiceOffering> offerings;

        if (categoryId != null) {
            offerings = offeringRepository.findVerifiedByCategoryAndCommunity(
                    categoryId, communityId, pageable);
        } else {
            offerings = offeringRepository.findByCategoryIdAndAvailableTrue(
                    categoryId, pageable);
        }

        return offerings.map(this::toSearchResult);
    }

    private ServiceSearchResult toSearchResult(ProviderServiceOffering o) {
        return ServiceSearchResult.builder()
                .offeringId(o.getId())
                .offeringTitle(o.getTitle())
                .offeringDescription(o.getDescription())
                .basePrice(o.getBasePrice())
                .pricingUnit(o.getPricingUnit().name())
                .estimatedDurationMinutes(o.getEstimatedDurationMinutes())
                .providerId(o.getProvider() != null ? o.getProvider().getId() : null)
                .providerName(o.getProvider() != null && o.getProvider().getUser() != null
                        ? o.getProvider().getUser().getFullName() : null)
                .providerType(o.getProvider() != null ? o.getProvider().getProviderType().name() : null)
                .providerRating(o.getProvider() != null ? o.getProvider().getAvgRating() : null)
                .providerTotalJobs(o.getProvider() != null ? o.getProvider().getTotalJobsCompleted() : null)
                .verificationStatus(o.getProvider() != null ? o.getProvider().getVerificationStatus().name() : null)
                .categoryId(o.getCategory() != null ? o.getCategory().getId() : null)
                .categoryName(o.getCategory() != null ? o.getCategory().getName() : null)
                .domainId(o.getCategory() != null && o.getCategory().getDomain() != null
                        ? o.getCategory().getDomain().getId() : null)
                .domainName(o.getCategory() != null && o.getCategory().getDomain() != null
                        ? o.getCategory().getDomain().getName() : null)
                .build();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -pl . -Dtest="ServiceSearchServiceTest" -q 2>&1 | tail -10`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/service/ServiceSearchService.java \
        src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceSearchServiceTest.java
git commit -m "feat(csp): add ServiceSearchService for provider/offering discovery"
```

---

### Task 9: Admin Controllers

**Files:**
- Create: `serviceplatform/controller/ServiceDomainController.java`
- Create: `serviceplatform/controller/ServiceCategoryController.java`

**Interfaces:**
- Consumes: `ServiceCatalogService`, `ServiceRequestService`, `WorkOrderService`, `ServiceProviderService`, `LoggedInUserService`
- Produces: Admin REST endpoints for domain CRUD, category CRUD, provider verification, request/work-order listing

- [ ] **Step 1: Create ServiceDomainController**

```java
// serviceplatform/controller/ServiceDomainController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceDomainRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceDomainResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform")
@RequiredArgsConstructor
public class ServiceDomainController {

    private final ServiceCatalogService catalogService;
    private final ServiceProviderService providerService;
    private final ServiceRequestService requestService;
    private final WorkOrderService workOrderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/domains")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<ServiceDomainResponse>> listDomains(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(catalogService.listDomains(communityId));
    }

    @GetMapping("/domains/{id}")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<ServiceDomainResponse> getDomain(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getDomain(id));
    }

    @PostMapping("/domains")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceDomainResponse> createDomain(
            @Valid @RequestBody CreateServiceDomainRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createDomain(request, communityId));
    }

    @PutMapping("/domains/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceDomainResponse> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceDomainRequest request) {
        return ResponseEntity.ok(catalogService.updateDomain(id, request));
    }

    @DeleteMapping("/domains/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<Void> deleteDomain(@PathVariable Long id) {
        catalogService.deleteDomain(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/providers")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<Page<ServiceProviderResponse>> listProviders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(providerService.listProviders(communityId, status, page, size));
    }

    @PatchMapping("/admin/providers/{id}/verify")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<ServiceProviderResponse> verifyProvider(
            @PathVariable Long id,
            @RequestParam String action) {
        return ResponseEntity.ok(providerService.verifyProvider(id, action));
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasAuthority('Manage Service Requests')")
    public ResponseEntity<Page<ServiceRequestResponse>> listAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(requestService.listAllRequests(communityId, status, page, size));
    }

    @PatchMapping("/admin/requests/{id}/assign")
    @PreAuthorize("hasAuthority('Manage Service Requests')")
    public ResponseEntity<ServiceRequestResponse> assignProvider(
            @PathVariable Long id,
            @Valid @RequestBody AssignProviderRequest request) {
        return ResponseEntity.ok(requestService.assignProvider(id, request));
    }

    @GetMapping("/admin/work-orders")
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<Page<WorkOrderResponse>> listAllWorkOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(workOrderService.listAllWorkOrders(communityId, status, page, size));
    }
}
```

- [ ] **Step 2: Create ServiceCategoryController**

```java
// serviceplatform/controller/ServiceCategoryController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform/categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCatalogService catalogService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<ServiceCategoryResponse>> listCategories(
            @RequestParam Long domainId) {
        return ResponseEntity.ok(catalogService.listCategories(domainId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<ServiceCategoryResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getCategory(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceCategoryResponse> createCategory(
            @Valid @RequestBody CreateServiceCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceCategoryRequest request) {
        return ResponseEntity.ok(catalogService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/controller/ServiceDomainController.java \
        src/main/java/com/manacommunity/api/serviceplatform/controller/ServiceCategoryController.java
git commit -m "feat(csp): add admin controllers for domain/category CRUD and provider/request management"
```

---

### Task 10: Provider Controllers

**Files:**
- Create: `serviceplatform/controller/ServiceProviderController.java`
- Create: `serviceplatform/controller/ServiceOfferingController.java`

**Interfaces:**
- Consumes: `ServiceProviderService`, `ServiceRequestService`, `WorkOrderService`, `LoggedInUserService`
- Produces: Provider REST endpoints for registration, profile, offerings, request accept/decline, work order updates

- [ ] **Step 1: Create ServiceProviderController**

```java
// serviceplatform/controller/ServiceProviderController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.RegisterProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.UpdateWorkOrderStatusRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-platform/providers")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final ServiceProviderService providerService;
    private final ServiceRequestService requestService;
    private final WorkOrderService workOrderService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceProviderResponse> register(
            @Valid @RequestBody RegisterProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.register(request, user));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceProviderResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceProviderResponse> updateMyProfile(
            @Valid @RequestBody RegisterProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.updateProfile(principal.getId(), request));
    }

    @GetMapping("/me/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ServiceRequestResponse>> myRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.listRequestsForProvider(principal.getId(), page, size));
    }

    @PatchMapping("/me/requests/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceRequestResponse> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.acceptRequest(id, principal.getId()));
    }

    @PatchMapping("/me/requests/{id}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceRequestResponse> declineRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.declineRequest(id, principal.getId()));
    }

    @GetMapping("/me/work-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WorkOrderResponse>> myWorkOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.listProviderWorkOrders(principal.getId(), page, size));
    }

    @PatchMapping("/me/work-orders/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkOrderResponse> updateWorkOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.updateStatus(id, principal.getId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Service Providers')")
    public ResponseEntity<ServiceProviderResponse> getProvider(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProfile(id));
    }
}
```

- [ ] **Step 2: Create ServiceOfferingController**

```java
// serviceplatform/controller/ServiceOfferingController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateOfferingRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform/providers/me/offerings")
@RequiredArgsConstructor
public class ServiceOfferingController {

    private final ServiceProviderService providerService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceOfferingResponse>> listMyOfferings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.listMyOfferings(principal.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceOfferingResponse> createOffering(
            @Valid @RequestBody CreateOfferingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createOffering(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceOfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody CreateOfferingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.updateOffering(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteOffering(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        providerService.deleteOffering(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/controller/ServiceProviderController.java \
        src/main/java/com/manacommunity/api/serviceplatform/controller/ServiceOfferingController.java
git commit -m "feat(csp): add provider controllers for registration, offerings, and work order management"
```

---

### Task 11: Resident Controllers

**Files:**
- Create: `serviceplatform/controller/ServiceRequestController.java`
- Create: `serviceplatform/controller/WorkOrderController.java`

**Interfaces:**
- Consumes: `ServiceRequestService`, `WorkOrderService`, `ServiceSearchService`, `LoggedInUserService`
- Produces: Resident REST endpoints for browsing, searching, requesting services, tracking requests/work orders

- [ ] **Step 1: Create ServiceRequestController**

```java
// serviceplatform/controller/ServiceRequestController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceSearchResult;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.serviceplatform.service.ServiceSearchService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-platform")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService requestService;
    private final ServiceSearchService searchService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<Page<ServiceSearchResult>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long domainId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(searchService.search(communityId, q, domainId, categoryId, page, size));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ServiceRequestResponse> createRequest(
            @Valid @RequestBody CreateServiceRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.createRequest(request, user));
    }

    @GetMapping("/requests/my")
    @PreAuthorize("hasAuthority('View Service Requests')")
    public ResponseEntity<Page<ServiceRequestResponse>> myRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.listMyRequests(principal.getId(), page, size));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('View Service Requests')")
    public ResponseEntity<ServiceRequestResponse> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequest(id));
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ServiceRequestResponse> cancelRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.cancelRequest(id, principal.getId(), reason));
    }

    @PatchMapping("/requests/{id}/submit")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ServiceRequestResponse> submitRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.submitRequest(id, principal.getId()));
    }
}
```

- [ ] **Step 2: Create WorkOrderController**

```java
// serviceplatform/controller/WorkOrderController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-platform/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Work Orders')")
    public ResponseEntity<WorkOrderResponse> getWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getWorkOrder(id));
    }

    @PatchMapping("/{id}/signoff")
    @PreAuthorize("hasAuthority('View Work Orders')")
    public ResponseEntity<WorkOrderResponse> residentSignoff(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.signoffResident(id, principal.getId()));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/manacommunity/api/serviceplatform/controller/ServiceRequestController.java \
        src/main/java/com/manacommunity/api/serviceplatform/controller/WorkOrderController.java
git commit -m "feat(csp): add resident controllers for search, requests, and work order signoff"
```

---

### Task 12: Seed Data

**Files:**
- Create: `src/main/resources/db/seed/V999__seed_service_domains_categories.sql`

**Interfaces:**
- Consumes: `service_domain` and `service_category` tables from Task 1
- Produces: 13 domains and ~155 categories pre-loaded

- [ ] **Step 1: Check if a db/seed or migration directory exists**

Run: `find src/main/resources -type d | head -20`

Determine if the project uses Flyway migrations (`db/migration/`), a seed directory, or if `ddl-auto:create` means tables are auto-created and data must be loaded via `data.sql` or `import.sql`.

- [ ] **Step 2: Create the seed SQL file**

Create the file in the appropriate location found in Step 1. If using `ddl-auto:create`, put it at `src/main/resources/data.sql` (Spring Boot auto-loads this). If Flyway, use `db/migration/V999__...sql`.

```sql
-- Service Domains and Categories Seed Data
-- This file loads the default service catalog for the Community Services Platform.

-- Domains
INSERT INTO service_domain (name, slug, icon, description, display_order, active, created_at, updated_at)
VALUES
('Home Services', 'home-services', '🏠', 'Residential maintenance and repair services', 1, true, NOW(), NOW()),
('Fitness & Wellness', 'fitness-wellness', '🏋', 'Health, fitness, and wellness services', 2, true, NOW(), NOW()),
('Healthcare', 'healthcare', '🩺', 'Medical and healthcare services', 3, true, NOW(), NOW()),
('Education', 'education', '📚', 'Tutoring and educational services', 4, true, NOW(), NOW()),
('Sports Coaching', 'sports-coaching', '🏸', 'Sports training and coaching services', 5, true, NOW(), NOW()),
('Pet Care', 'pet-care', '🐶', 'Pet care and veterinary services', 6, true, NOW(), NOW()),
('Events & Lifestyle', 'events-lifestyle', '🎉', 'Event planning and lifestyle services', 7, true, NOW(), NOW()),
('Automobile Services', 'automobile-services', '🚗', 'Vehicle maintenance and repair services', 8, true, NOW(), NOW()),
('Maintenance & AMC', 'maintenance-amc', '🛠', 'Annual maintenance and facility upkeep', 9, true, NOW(), NOW()),
('Child Care', 'child-care', '👶', 'Childcare and activity services', 10, true, NOW(), NOW()),
('Elder Care', 'elder-care', '👵', 'Senior citizen care services', 11, true, NOW(), NOW()),
('Community Facility Maintenance', 'community-facility', '🧹', 'Common area and facility maintenance', 12, true, NOW(), NOW()),
('Business & Professional Services', 'business-professional', '🏢', 'Professional and consulting services', 13, true, NOW(), NOW());

-- Categories: Home Services
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Electrician', 'electrician', '⚡', 1),
    ('Plumber', 'plumber', '🔧', 2),
    ('Carpenter', 'carpenter', '🪚', 3),
    ('Painter', 'painter', '🎨', 4),
    ('Mason', 'mason', '🧱', 5),
    ('Welder', 'welder', '🔥', 6),
    ('AC Technician', 'ac-technician', '❄️', 7),
    ('Appliance Repair', 'appliance-repair', '🔌', 8),
    ('Pest Control', 'pest-control', '🐛', 9),
    ('Deep Cleaning', 'deep-cleaning', '🧽', 10),
    ('House Cleaning', 'house-cleaning', '🏠', 11),
    ('Sofa Cleaning', 'sofa-cleaning', '🛋', 12),
    ('Water Tank Cleaning', 'water-tank-cleaning', '💧', 13),
    ('Kitchen Cleaning', 'kitchen-cleaning', '🍳', 14),
    ('Bathroom Cleaning', 'bathroom-cleaning', '🚿', 15),
    ('Interior Works', 'interior-works', '🏗', 16),
    ('Renovation', 'renovation', '🏘', 17),
    ('Waterproofing', 'waterproofing', '🌧', 18),
    ('Civil Works', 'civil-works', '🏛', 19)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'home-services';

-- Categories: Fitness & Wellness
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Personal Trainer', 'personal-trainer', '💪', 1),
    ('Yoga Trainer', 'yoga-trainer', '🧘', 2),
    ('Meditation Coach', 'meditation-coach', '🕉', 3),
    ('Zumba Trainer', 'zumba-trainer', '💃', 4),
    ('Dance Instructor', 'dance-instructor', '🩰', 5),
    ('Gym Trainer', 'gym-trainer', '🏋', 6),
    ('Pilates Coach', 'pilates-coach', '🤸', 7),
    ('Nutritionist', 'nutritionist', '🥗', 8),
    ('Dietician', 'dietician', '🍎', 9),
    ('Spa Therapist', 'spa-therapist', '💆', 10),
    ('Massage Therapist', 'massage-therapist', '🙌', 11),
    ('Beauty Services', 'beauty-services', '💅', 12),
    ('Salon at Home', 'salon-at-home', '💇', 13),
    ('Hair Stylist', 'hair-stylist', '✂️', 14)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'fitness-wellness';

-- Categories: Healthcare
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('General Physician', 'general-physician', '👨‍⚕️', 1),
    ('Pediatrician', 'pediatrician', '👶', 2),
    ('Physiotherapist', 'physiotherapist', '🦴', 3),
    ('Nurse', 'nurse', '👩‍⚕️', 4),
    ('Caretaker', 'caretaker', '🤝', 5),
    ('Ambulance', 'ambulance', '🚑', 6),
    ('Lab Test', 'lab-test', '🔬', 7),
    ('Blood Collection', 'blood-collection', '🩸', 8),
    ('Pharmacy Delivery', 'pharmacy-delivery', '💊', 9),
    ('Vaccination', 'vaccination', '💉', 10),
    ('Home Health Checkup', 'home-health-checkup', '🏥', 11),
    ('Medical Equipment Rental', 'medical-equipment-rental', '🩺', 12),
    ('Mental Health Counselling', 'mental-health-counselling', '🧠', 13)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'healthcare';

-- Categories: Education
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Home Tutor', 'home-tutor', '📖', 1),
    ('Coding Instructor', 'coding-instructor', '💻', 2),
    ('Music Teacher', 'music-teacher', '🎵', 3),
    ('Dance Teacher', 'dance-teacher', '💃', 4),
    ('Art Teacher', 'art-teacher', '🎨', 5),
    ('Language Trainer', 'language-trainer', '🗣', 6),
    ('Spoken English', 'spoken-english', '🇬🇧', 7),
    ('Robotics Trainer', 'robotics-trainer', '🤖', 8),
    ('Chess Coach', 'chess-coach', '♟', 9),
    ('Career Counselling', 'career-counselling', '🎯', 10),
    ('Tuition Classes', 'tuition-classes', '📝', 11)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'education';

-- Categories: Sports Coaching
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Cricket Coach', 'cricket-coach', '🏏', 1),
    ('Tennis Coach', 'tennis-coach', '🎾', 2),
    ('Swimming Coach', 'swimming-coach', '🏊', 3),
    ('Football Coach', 'football-coach', '⚽', 4),
    ('Basketball Coach', 'basketball-coach', '🏀', 5),
    ('Badminton Coach', 'badminton-coach', '🏸', 6),
    ('Skating Coach', 'skating-coach', '⛸', 7),
    ('Athletics Coach', 'athletics-coach', '🏃', 8),
    ('Table Tennis Coach', 'table-tennis-coach', '🏓', 9),
    ('Martial Arts Coach', 'martial-arts-coach', '🥋', 10),
    ('Gymnastics Coach', 'gymnastics-coach', '🤸', 11),
    ('Tournament Officials', 'tournament-officials', '📋', 12),
    ('Referees', 'referees', '🟨', 13),
    ('Umpires', 'umpires', '⚖️', 14)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'sports-coaching';

-- Categories: Pet Care
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Pet Grooming', 'pet-grooming', '🐩', 1),
    ('Veterinary Doctor', 'veterinary-doctor', '🩺', 2),
    ('Pet Walking', 'pet-walking', '🐕', 3),
    ('Pet Boarding', 'pet-boarding', '🏠', 4),
    ('Pet Sitting', 'pet-sitting', '🐱', 5),
    ('Dog Training', 'dog-training', '🦮', 6),
    ('Pet Vaccination', 'pet-vaccination', '💉', 7),
    ('Pet Taxi', 'pet-taxi', '🚕', 8),
    ('Pet Food Delivery', 'pet-food-delivery', '🦴', 9)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'pet-care';

-- Categories: Events & Lifestyle
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Catering', 'catering', '🍽', 1),
    ('Event Decoration', 'event-decoration', '🎊', 2),
    ('Photography', 'photography', '📸', 3),
    ('Videography', 'videography', '🎥', 4),
    ('DJ', 'dj', '🎧', 5),
    ('Event Planner', 'event-planner', '📋', 6),
    ('Birthday Organizer', 'birthday-organizer', '🎂', 7),
    ('Wedding Planner', 'wedding-planner', '💍', 8),
    ('Balloon Decoration', 'balloon-decoration', '🎈', 9),
    ('Sound System Rental', 'sound-system-rental', '🔊', 10),
    ('Stage Setup', 'stage-setup', '🎭', 11),
    ('Live Music', 'live-music', '🎸', 12),
    ('Community Event Management', 'community-event-management', '🏘', 13)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'events-lifestyle';

-- Categories: Automobile Services
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Car Wash', 'car-wash', '🚗', 1),
    ('Bike Wash', 'bike-wash', '🏍', 2),
    ('Car Detailing', 'car-detailing', '✨', 3),
    ('Vehicle Repair', 'vehicle-repair', '🔧', 4),
    ('Tyre Repair', 'tyre-repair', '🛞', 5),
    ('Battery Replacement', 'battery-replacement', '🔋', 6),
    ('EV Charging Assistance', 'ev-charging', '⚡', 7),
    ('Towing', 'towing', '🚛', 8),
    ('Driver On Demand', 'driver-on-demand', '🚘', 9),
    ('Vehicle Inspection', 'vehicle-inspection', '🔍', 10)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'automobile-services';

-- Categories: Maintenance & AMC
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Lift AMC', 'lift-amc', '🛗', 1),
    ('Generator AMC', 'generator-amc', '⚡', 2),
    ('Solar AMC', 'solar-amc', '☀️', 3),
    ('CCTV AMC', 'cctv-amc', '📹', 4),
    ('Fire Safety AMC', 'fire-safety-amc', '🧯', 5),
    ('Water Purifier AMC', 'water-purifier-amc', '💧', 6),
    ('STP Maintenance', 'stp-maintenance', '🏭', 7),
    ('DG Maintenance', 'dg-maintenance', '🔌', 8),
    ('Swimming Pool Maintenance', 'swimming-pool-maintenance', '🏊', 9),
    ('Garden Maintenance', 'garden-maintenance', '🌿', 10),
    ('Electrical Maintenance', 'electrical-maintenance', '💡', 11),
    ('Plumbing Maintenance', 'plumbing-maintenance', '🚰', 12),
    ('Building Maintenance', 'building-maintenance', '🏢', 13)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'maintenance-amc';

-- Categories: Child Care
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Babysitting', 'babysitting', '👶', 1),
    ('Day Care', 'day-care', '🏫', 2),
    ('Child Pickup & Drop', 'child-pickup-drop', '🚐', 3),
    ('Activity Classes', 'activity-classes', '🎨', 4),
    ('Summer Camps', 'summer-camps', '⛺', 5),
    ('Homework Assistance', 'homework-assistance', '📝', 6),
    ('Child Counselling', 'child-counselling', '🧠', 7)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'child-care';

-- Categories: Elder Care
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Elder Care Assistant', 'elder-care-assistant', '👵', 1),
    ('Nursing Care', 'nursing-care', '👩‍⚕️', 2),
    ('Medical Checkups', 'medical-checkups', '🩺', 3),
    ('Physiotherapy', 'physiotherapy', '🦴', 4),
    ('Home Visit Doctor', 'home-visit-doctor', '🏥', 5),
    ('Medicine Delivery', 'medicine-delivery', '💊', 6),
    ('Companion Services', 'companion-services', '🤝', 7),
    ('Emergency Assistance', 'emergency-assistance', '🆘', 8)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'elder-care';

-- Categories: Community Facility Maintenance
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Housekeeping', 'housekeeping', '🧹', 1),
    ('Security Guards', 'security-guards', '💂', 2),
    ('Gardening', 'gardening', '🌱', 3),
    ('Waste Collection', 'waste-collection', '🗑', 4),
    ('STP Operations', 'stp-operations', '🏭', 5),
    ('Water Tank Cleaning', 'comm-water-tank-cleaning', '💧', 6),
    ('Swimming Pool Maintenance', 'comm-swimming-pool', '🏊', 7),
    ('Lift Maintenance', 'comm-lift-maintenance', '🛗', 8),
    ('Fire Safety Inspection', 'fire-safety-inspection', '🧯', 9),
    ('CCTV Monitoring', 'cctv-monitoring', '📹', 10),
    ('Generator Operations', 'generator-operations', '⚡', 11),
    ('Electrical Maintenance', 'comm-electrical', '💡', 12),
    ('Plumbing Maintenance', 'comm-plumbing', '🚰', 13),
    ('Common Area Cleaning', 'common-area-cleaning', '🧼', 14)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'community-facility';

-- Categories: Business & Professional Services
INSERT INTO service_category (domain_id, name, slug, icon, display_order, active, created_at, updated_at)
SELECT d.id, c.name, c.slug, c.icon, c.ord, true, NOW(), NOW()
FROM service_domain d
CROSS JOIN (VALUES
    ('Chartered Accountant', 'chartered-accountant', '📊', 1),
    ('Lawyer', 'lawyer', '⚖️', 2),
    ('Tax Consultant', 'tax-consultant', '🧾', 3),
    ('Insurance Advisor', 'insurance-advisor', '🛡', 4),
    ('Financial Planner', 'financial-planner', '💰', 5),
    ('Real Estate Consultant', 'real-estate-consultant', '🏘', 6),
    ('Architect', 'architect', '📐', 7),
    ('Interior Designer', 'interior-designer', '🎨', 8),
    ('Loan Consultant', 'loan-consultant', '🏦', 9),
    ('Immigration Consultant', 'immigration-consultant', '✈️', 10)
) AS c(name, slug, icon, ord)
WHERE d.slug = 'business-professional';
```

- [ ] **Step 2: Verify the SQL is syntactically valid by reviewing it**

Confirm the SQL uses the correct column names matching the entity table definitions. The `community_id` column is nullable (platform-wide domains have no community). The `CROSS JOIN (VALUES ...)` syntax works on PostgreSQL 10+.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/seed/
git commit -m "feat(csp): add seed data for 13 service domains and 155 categories"
```

Note: Adjust the file path in Step 1 based on what directory structure the project actually uses for SQL files.

---

## Plan Self-Review

**Spec coverage check:**
- Service Catalog (domains + categories): Task 1-2 entities, Task 5 service, Task 9 controller ✅
- Provider Registry: Task 1 entity, Task 2 repo, Task 6 service, Task 10 controller ✅
- Provider Offerings: Task 1 entity, Task 2 repo, Task 6 service, Task 10 controller ✅
- Service Requests: Task 1 entity, Task 2 repo, Task 7 service, Task 11 controller ✅
- Work Orders: Task 1 entity, Task 2 repo, Task 7 service, Task 9+11 controllers ✅
- Search/Discovery: Task 8 service, Task 11 controller ✅
- Audit integration: Task 4 ✅
- Permission constants: Task 4 ✅
- Seed data: Task 12 ✅
- Status transitions: Validated in WorkOrderService and tests ✅
- Zero-code category addition: Domain/category CRUD API handles this ✅
- Community scoping: All entities have communityId, controllers resolve from user ✅
- Custom fields (JSONB): Entities have customFields/customFieldValues as JSONB String ✅
- Integration with Vendor: ServiceProvider.vendorId optional FK ✅
- Integration with Invoice: WorkOrder.invoiceId FK ✅

**Placeholder scan:** No TBD/TODO/placeholder content found.

**Type consistency check:**
- `ServiceCatalogService` method signatures match controller calls ✅
- `ServiceProviderService` method signatures match controller calls ✅
- `ServiceRequestService` method signatures match controller calls ✅
- `WorkOrderService` method signatures match controller calls ✅
- `ServiceSearchService.search()` signature matches controller call ✅
- All DTO class names consistent across tasks ✅
- All enum class names consistent ✅
