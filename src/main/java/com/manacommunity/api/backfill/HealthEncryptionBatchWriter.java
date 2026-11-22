package com.manacommunity.api.backfill;

import com.manacommunity.api.healthguard.entity.EmergencyContact;
import com.manacommunity.api.healthguard.entity.HealthProfile;
import com.manacommunity.api.healthguard.repository.EmergencyContactRepository;
import com.manacommunity.api.healthguard.repository.HealthProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes one batch of the health-encryption backfill in its own transaction.
 *
 * <h3>How the encryption actually happens</h3>
 * Nothing here calls the cipher. Each row is loaded as a JPA entity — so
 * {@code EncryptedStringConverter} decrypts the sensitive columns on read, returning legacy
 * plaintext unchanged — and then the entity is made dirty by bumping {@code updatedAt}.
 * The entities are not annotated {@code @DynamicUpdate}, so Hibernate emits an UPDATE
 * covering <em>every</em> updatable column, and each sensitive column is passed back through
 * {@code EncryptedStringConverter.convertToDatabaseColumn} on the way out. The round trip is
 * read-through-converter / write-through-converter; the job never touches
 * {@code FieldEncryptionService} and never writes a value with SQL.
 *
 * <h3>Why a separate bean</h3>
 * The paging loop in {@link HealthEncryptionBackfillJob} is deliberately non-transactional so
 * that a failure half-way leaves earlier batches committed and the job re-runnable. Calling a
 * {@code @Transactional} method on the same bean would bypass the proxy, so the per-batch
 * transaction lives here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthEncryptionBatchWriter {

    private final HealthProfileRepository healthProfileRepository;
    private final EmergencyContactRepository emergencyContactRepository;

    /** @return the number of rows actually re-written */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int rewriteHealthProfiles(List<Long> ids) {
        List<HealthProfile> rows = healthProfileRepository.findAllById(ids);
        for (HealthProfile row : rows) {
            row.setUpdatedAt(bump(row.getUpdatedAt()));
        }
        healthProfileRepository.saveAll(rows);
        healthProfileRepository.flush();
        return rows.size();
    }

    /** @return the number of rows actually re-written */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int rewriteEmergencyContacts(List<Long> ids) {
        List<EmergencyContact> rows = emergencyContactRepository.findAllById(ids);
        for (EmergencyContact row : rows) {
            row.setUpdatedAt(bump(row.getUpdatedAt()));
        }
        emergencyContactRepository.saveAll(rows);
        emergencyContactRepository.flush();
        return rows.size();
    }

    /**
     * Returns a timestamp guaranteed to differ from {@code current}, so Hibernate's dirty
     * check always sees a change and always emits the UPDATE that re-encrypts the row.
     * (The entities' own {@code @PreUpdate} overwrites this with {@code now()} anyway; the
     * point is purely to make the entity dirty.)
     */
    private static LocalDateTime bump(LocalDateTime current) {
        LocalDateTime now = LocalDateTime.now();
        return now.equals(current) ? now.plusNanos(1_000L) : now;
    }
}
