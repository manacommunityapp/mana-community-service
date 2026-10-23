package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsExternalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportsExternalResponseRepository extends JpaRepository<SportsExternalResponse, Long> {

    Optional<SportsExternalResponse> findByFormIdAndExternalResponseId(Long formId, String externalResponseId);

    boolean existsByFormIdAndExternalResponseId(Long formId, String externalResponseId);

    Page<SportsExternalResponse> findByFormId(Long formId, Pageable pageable);

    Page<SportsExternalResponse> findByFormIdAndStatus(Long formId, SportsExternalResponse.ResponseStatus status, Pageable pageable);

    List<SportsExternalResponse> findByFormIdOrderBySubmittedAtDesc(Long formId);

    List<SportsExternalResponse> findByFormIdAndStatusOrderBySubmittedAtDesc(Long formId, SportsExternalResponse.ResponseStatus status);

    @Query("SELECT r FROM SportsExternalResponse r WHERE r.form.event.id = :eventId AND r.status IN :statuses ORDER BY r.submittedAt DESC")
    List<SportsExternalResponse> findByEventIdAndStatusIn(
            @Param("eventId") Long eventId,
            @Param("statuses") List<SportsExternalResponse.ResponseStatus> statuses
    );

    long countByFormId(Long formId);

    long countByFormIdAndStatus(Long formId, SportsExternalResponse.ResponseStatus status);

    @Query("SELECT r FROM SportsExternalResponse r LEFT JOIN FETCH r.linkedRegistration LEFT JOIN FETCH r.resolvedUser WHERE r.id = :id")
    Optional<SportsExternalResponse> findByIdWithDetails(@Param("id") Long id);
}
