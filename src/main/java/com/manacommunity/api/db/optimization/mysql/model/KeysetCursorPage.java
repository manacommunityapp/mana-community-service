package com.manacommunity.api.db.optimization.mysql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic container for Keyset (Cursor-based) paginated results.
 * Replaces traditional Offset-based Page<T> to eliminate deep-offset B-Tree scan penalties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeysetCursorPage<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasNext;
    private int pageSize;
}
