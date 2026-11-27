package com.manacommunity.api.db.optimization.mysql.service;

import com.manacommunity.api.db.optimization.mysql.model.KeysetCursorPage;
import com.manacommunity.api.db.optimization.mysql.model.VisitorCursorRecord;

import java.util.List;

public interface KeysetPaginationEngine {

    /**
     * Ingests mock visitor records for testing keyset pagination behavior.
     */
    void seedRecords(List<VisitorCursorRecord> records);

    /**
     * Executes a keyset-based cursor query against the indexed collection.
     * SQL equivalent:
     * WHERE community_id = :communityId 
     *   AND (created_at < :lastCreatedAt OR (created_at = :lastCreatedAt AND id < :lastId))
     * ORDER BY created_at DESC, id DESC 
     * LIMIT :pageSize
     *
     * @param communityId society identifier
     * @param cursor      opaque base64 encoded (createdAtEpochMs_id) or null for first page
     * @param pageSize    number of records to fetch
     * @return KeysetCursorPage with items and nextCursor
     */
    KeysetCursorPage<VisitorCursorRecord> queryVisitorPasses(Long communityId, String cursor, int pageSize);

    /**
     * Encodes a record's createdAt and id into an opaque URL-safe cursor string.
     */
    String encodeCursor(VisitorCursorRecord record);

    /**
     * Decodes an opaque cursor string into [createdAtEpochMilli, id].
     */
    long[] decodeCursor(String cursor);
}
