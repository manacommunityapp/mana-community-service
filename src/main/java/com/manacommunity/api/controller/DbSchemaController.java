package com.manacommunity.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the live database schema (tables + columns) for the Architecture Docs
 * ER Diagram. Reads PostgreSQL's information_schema so the diagram always
 * reflects the actually-connected database.
 *
 * SUPER_ADMIN only — this is an internal/architectural view.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DbSchemaController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * The schema the app's tables actually live in. This project sets Hibernate's
     * default_schema to "manacommunity" (per environment yaml), NOT the Postgres
     * default "public" — so the schema dump must target it, otherwise the query
     * matches nothing and the ER diagram renders empty.
     */
    @Value("${spring.jpa.properties.hibernate.default_schema:manacommunity}")
    private String dbSchema;

    /** One table and the ordered list of its column names. */
    public record TableSchema(String name, List<String> columns) {}

    /**
     * GET /api/admin/db-schema
     * Returns every table in the application schema with its columns (in ordinal order).
     */
    @GetMapping("/db-schema")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<TableSchema>> getDbSchema() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = ?
              AND table_name NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
            ORDER BY table_name, ordinal_position
            """,
            dbSchema
        );

        Map<String, List<String>> byTable = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String table = (String) row.get("table_name");
            String column = (String) row.get("column_name");
            byTable.computeIfAbsent(table, k -> new ArrayList<>()).add(column);
        }

        List<TableSchema> result = new ArrayList<>();
        byTable.forEach((name, columns) -> result.add(new TableSchema(name, columns)));
        return ResponseEntity.ok(result);
    }
}
