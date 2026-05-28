package com.expertrise.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * DatabaseUtil — JDBC helper for backend database validation in automation tests.
 *
 * Used to verify that UI/API actions correctly persisted data in the database.
 * Example: after creating a user via API, query DB to confirm the record exists.
 *
 * Supported databases (set db.url in config.properties accordingly):
 *   MySQL      — jdbc:mysql://localhost:3306/testdb
 *   PostgreSQL — jdbc:postgresql://localhost:5432/testdb
 *   H2 Memory  — jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1  (CI/local testing)
 *
 * ThreadLocal connection — each parallel test thread gets its own connection.
 *
 * Usage:
 *   DatabaseUtil.connect();
 *   List&lt;Map&lt;String,Object&gt;&gt; rows = DatabaseUtil.executeQuery(
 *       "SELECT * FROM users WHERE email = ?", "jane@test.com");
 *   String role = DatabaseUtil.getSingleValue(
 *       "SELECT role FROM users WHERE email = ?", "jane@test.com");
 *   DatabaseUtil.executeUpdate("DELETE FROM users WHERE email = ?", "jane@test.com");
 *   DatabaseUtil.disconnect();
 */
public class DatabaseUtil {

    private static final Logger log = LogManager.getLogger(DatabaseUtil.class);

    // ThreadLocal — parallel threads each get their own JDBC connection
    private static final ThreadLocal<Connection> connectionThread = new ThreadLocal<>();

    // ── CONNECTION ─────────────────────────────────────────────────────────────

    /**
     * Connect using db.url / db.username / db.password from config.properties.
     */
    public static void connect() {
        connect(
            ConfigManager.get("db.url",      "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"),
            ConfigManager.get("db.username", "sa"),
            ConfigManager.get("db.password", "")
        );
    }

    /**
     * Connect with explicit JDBC credentials.
     *
     * @param jdbcUrl  full JDBC URL
     * @param username DB username
     * @param password DB password
     */
    public static void connect(String jdbcUrl, String username, String password) {
        try {
            Connection existing = connectionThread.get();
            if (existing != null && !existing.isClosed()) {
                log.debug("Reusing existing DB connection — thread {}", Thread.currentThread().getId());
                return;
            }
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            connectionThread.set(conn);
            log.info("DB connected — url={} thread={}", jdbcUrl, Thread.currentThread().getId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + jdbcUrl, e);
        }
    }

    /**
     * Close and remove the JDBC connection for the current thread.
     * Always call this in @AfterMethod / teardown.
     */
    public static void disconnect() {
        Connection conn = connectionThread.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    log.info("DB connection closed — thread {}", Thread.currentThread().getId());
                }
            } catch (SQLException e) {
                log.warn("Error closing DB connection: {}", e.getMessage());
            } finally {
                connectionThread.remove();
            }
        }
    }

    /** Returns true if the current thread has an open DB connection. */
    public static boolean isConnected() {
        try {
            Connection c = connectionThread.get();
            return c != null && !c.isClosed();
        } catch (SQLException e) { return false; }
    }

    // ── QUERY ──────────────────────────────────────────────────────────────────

    /**
     * Execute a SELECT query and return all result rows.
     *
     * @param sql    query with ? placeholders
     * @param params values in placeholder order
     * @return List of rows, each row is a Map{columnName(lowercase) → value}
     */
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) {
        ensureConnected();
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement stmt = prepare(sql, params);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++)
                    row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                results.add(row);
            }
            log.info("Query returned {} rows — SQL: {}", results.size(), sql.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return results;
    }

    /**
     * Execute a SELECT and return the first column of the first row as String.
     * Ideal for: SELECT COUNT(*), SELECT field FROM table WHERE id=?
     *
     * @return value as String, or null if no rows returned
     */
    public static String getSingleValue(String sql, Object... params) {
        ensureConnected();
        try (PreparedStatement stmt = prepare(sql, params);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String val = rs.getString(1);
                log.info("Single value: '{}' — SQL: {}", val, sql.trim());
                return val;
            }
            log.warn("No results — SQL: {}", sql);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }

    /**
     * Execute a COUNT query and return the integer result.
     */
    public static int getRowCount(String sql, Object... params) {
        String val = getSingleValue(sql, params);
        return val == null ? 0 : Integer.parseInt(val);
    }

    /**
     * Returns true if the query returns at least one matching record.
     * Use to assert a record exists in the DB after a UI/API action.
     */
    public static boolean recordExists(String sql, Object... params) {
        boolean exists = !executeQuery(sql, params).isEmpty();
        log.info("Record exists: {} — SQL: {}", exists, sql.trim());
        return exists;
    }

    // ── DML ────────────────────────────────────────────────────────────────────

    /**
     * Execute INSERT / UPDATE / DELETE.
     *
     * @param sql    DML with ? placeholders
     * @param params parameter values
     * @return rows affected
     */
    public static int executeUpdate(String sql, Object... params) {
        ensureConnected();
        try (PreparedStatement stmt = prepare(sql, params)) {
            int affected = stmt.executeUpdate();
            log.info("DML: {} rows affected — SQL: {}", affected, sql.trim());
            return affected;
        } catch (SQLException e) {
            throw new RuntimeException("DML failed: " + sql, e);
        }
    }

    /**
     * Insert a row from a column-name → value Map.
     * Generates the INSERT SQL dynamically — no hardcoded column lists.
     *
     * @param tableName  target table
     * @param columnData {columnName → value}
     * @return rows inserted (should be 1)
     */
    public static int insertRow(String tableName, Map<String, Object> columnData) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : columnData.entrySet()) {
            if (cols.length() > 0) { cols.append(", "); vals.append(", "); }
            cols.append(e.getKey());
            vals.append("?");
            params.add(e.getValue());
        }
        return executeUpdate(
            "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + vals + ")",
            params.toArray()
        );
    }

    /**
     * Delete rows matching a WHERE condition.
     *
     * @param tableName   target table
     * @param whereClause e.g. "email = ?"
     * @param params      values for the WHERE clause
     */
    public static int deleteRows(String tableName, String whereClause, Object... params) {
        return executeUpdate("DELETE FROM " + tableName + " WHERE " + whereClause, params);
    }

    // ── TRANSACTIONS ───────────────────────────────────────────────────────────

    /** Begin a manual transaction (disables auto-commit). */
    public static void beginTransaction() {
        try {
            ensureConnected();
            connectionThread.get().setAutoCommit(false);
            log.info("DB transaction started");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start transaction", e);
        }
    }

    /** Commit the current transaction and re-enable auto-commit. */
    public static void commitTransaction() {
        try {
            connectionThread.get().commit();
            connectionThread.get().setAutoCommit(true);
            log.info("DB transaction committed");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    /** Roll back all changes made since beginTransaction(). */
    public static void rollbackTransaction() {
        try {
            if (isConnected()) {
                connectionThread.get().rollback();
                connectionThread.get().setAutoCommit(true);
                log.warn("DB transaction rolled back");
            }
        } catch (SQLException e) {
            log.error("Rollback failed: {}", e.getMessage());
        }
    }

    // ── INTERNAL ───────────────────────────────────────────────────────────────

    private static PreparedStatement prepare(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = connectionThread.get().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
        return stmt;
    }

    private static void ensureConnected() {
        if (!isConnected())
            throw new IllegalStateException(
                "No active DB connection. Call DatabaseUtil.connect() before queries. " +
                "Typically called in @BeforeMethod.");
    }

    private DatabaseUtil() {}
}
