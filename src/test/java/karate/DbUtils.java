package karate;

import java.sql.*;
import java.util.*;

/**
 * JDBC helper called from Karate feature files via:
 *   * def db = Java.type('karate.DbUtils')
 *   * def conn = new db(dbUrl, dbUser, dbPassword)
 *   * def rows = conn.query('SELECT * FROM event_community WHERE id = 1')
 *   * def val  = conn.scalar('SELECT COUNT(*) FROM app_users')
 */
public class DbUtils {

    private final String url;
    private final String user;
    private final String password;

    public DbUtils(String url, String user, String password) {
        this.url      = url;
        this.user     = user;
        this.password = password;
    }

    /** Run a SELECT and return every row as a Map<columnName, value>. */
    public List<Map<String, Object>> query(String sql) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                }
                results.add(row);
            }
        }
        return results;
    }

    /** Run a scalar SELECT and return the first column of the first row. */
    public Object scalar(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getObject(1) : null;
        }
    }

    /** Execute an INSERT/UPDATE/DELETE — returns affected-row count. */
    public int execute(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement  stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
}
