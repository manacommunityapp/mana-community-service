package com.manacommunity.api.config;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

/**
 * DatabaseConnectionSecurityGuard
 *
 * Enforces strict environment isolation to protect cloud databases from local/unauthorized connections.
 * Guarantees that no local developer machine, IDE run configuration, or test runner can EVER
 * establish a database connection to the AWS Lightsail database (3.109.145.130).
 */
@Slf4j
public class DatabaseConnectionSecurityGuard {

    public static final Set<String> BLOCKED_CLOUD_IPS = Set.of(
            "3.109.145.130"
    );

    public static final Set<String> BLOCKED_CLOUD_HOSTS = Set.of(
            "3.109.145.130",
            "manacommunityhub.com",
            "dev.manacommunityhub.com"
    );

    /**
     * Validates that the active JDBC connection does not violate environment security rules.
     * Throws SecurityException immediately if an illegal connection is detected.
     */
    public static void validateConnection(Connection conn) throws SQLException {
        if (conn == null) return;

        String jdbcUrl = "";
        try {
            jdbcUrl = conn.getMetaData().getURL();
        } catch (Exception e) {
            log.warn("Could not read JDBC URL from connection metadata: {}", e.getMessage());
        }

        String lowerUrl = jdbcUrl.toLowerCase();
        // In-memory or H2 test databases are always safe
        if (lowerUrl.contains("h2") || lowerUrl.contains(":mem:")) {
            return;
        }

        // 1. Check if JDBC URL explicitly targets a blocked cloud host
        boolean urlTargetsCloud = false;
        for (String blocked : BLOCKED_CLOUD_HOSTS) {
            if (lowerUrl.contains(blocked.toLowerCase())) {
                urlTargetsCloud = true;
                break;
            }
        }

        // 2. Query PostgreSQL runtime socket addresses
        String serverAddrStr = null;
        String clientAddrStr = null;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT inet_server_addr(), inet_client_addr()")) {
            if (rs.next()) {
                serverAddrStr = rs.getString(1);
                clientAddrStr = rs.getString(2);
            }
        } catch (Exception e) {
            // Not PostgreSQL or permissions restricted
            log.debug("inet socket check: {}", e.getMessage());
        }

        boolean serverIsLightsail = false;
        if (serverAddrStr != null) {
            serverIsLightsail = BLOCKED_CLOUD_IPS.contains(serverAddrStr) || "172.26.2.251".equals(serverAddrStr);
        }

        // 3. Detect if current environment is a local developer machine or external client
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isLocalDevOs = osName.contains("win") || osName.contains("mac");
        boolean isNotServer = !new File("/etc/mana-community").exists() && !new File("/opt/mana-community").exists();

        boolean clientIsExternal = false;
        if (clientAddrStr != null) {
            try {
                InetAddress clientAddr = InetAddress.getByName(clientAddrStr);
                // If client address is NOT loopback (127.0.0.1 / ::1) and NOT internal VPC private address
                clientIsExternal = !clientAddr.isLoopbackAddress() && !clientAddr.isSiteLocalAddress();
            } catch (Exception ignored) {
            }
        }

        // If the connection targets AWS Lightsail, verify that caller is running ON the Lightsail instance
        if (urlTargetsCloud || serverIsLightsail) {
            if (isLocalDevOs || isNotServer || clientIsExternal) {
                String violation = String.format(
                        "\n========================================================================================\n" +
                        "  CRITICAL ENVIRONMENT SAFETY VIOLATION: APPLICATION STARTUP BLOCKED!\n" +
                        "========================================================================================\n" +
                        "  A local or external environment (OS: %s, Client IP: %s)\n" +
                        "  attempted to connect to the AWS Lightsail database (%s, Server IP: %s)!\n\n" +
                        "  Direct database connections from local developer workstations, IDEs, or test suites\n" +
                        "  to remote AWS databases are strictly prohibited to prevent data corruption and accidental resets.\n\n" +
                        "  HOW TO FIX:\n" +
                        "  1. Run with the 'local' profile: -Dspring.profiles.active=local\n" +
                        "  2. Or configure a local PostgreSQL database on localhost:5432\n" +
                        "  3. Ensure DB_HOST is set to 'localhost' rather than remote AWS IP.\n" +
                        "========================================================================================",
                        osName, (clientAddrStr != null ? clientAddrStr : "unknown"),
                        jdbcUrl, (serverAddrStr != null ? serverAddrStr : "unknown")
                );
                log.error(violation);
                throw new SecurityException(violation);
            }
        }

        log.info("✓ Database connection security verified (Server: {}, Client: {})",
                serverAddrStr != null ? serverAddrStr : "local",
                clientAddrStr != null ? clientAddrStr : "local");
    }
}
