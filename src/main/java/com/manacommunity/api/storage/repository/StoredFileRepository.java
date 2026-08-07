package com.manacommunity.api.storage.repository;

import com.manacommunity.api.storage.StoredFileMetaOnly;
import com.manacommunity.api.storage.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    /** Lightweight existence + metadata check without loading the binary payload. */
    @Query("SELECT new com.manacommunity.api.storage.StoredFileMetaOnly(f.id, f.originalName, f.contentType, f.sizeBytes) FROM StoredFile f WHERE f.id = :id")
    Optional<StoredFileMetaOnly> findMetaById(Long id);
}

