package com.pmtool.db;

import com.pmtool.exception.DatabaseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseMigrationRunner {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationRunner.class.getName());
    private static final String SCHEMA_FILE = "schema.sql";

    public static void runMigrations(DatabaseManager databaseManager) {
        LOGGER.info("Starting database schema migration from resource: " + SCHEMA_FILE);
        List<String> statements = loadSqlStatements(SCHEMA_FILE);

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            LOGGER.info("Database schema migration completed successfully (" + statements.size() + " statements executed).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database migration failed: " + e.getMessage(), e);
            throw new DatabaseException("Failed to run schema migrations: " + e.getMessage(), e);
        }
    }

    private static List<String> loadSqlStatements(String resourceName) {
        List<String> statements = new ArrayList<>();
        InputStream is = DatabaseMigrationRunner.class.getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new DatabaseException("Schema resource file not found on classpath: " + resourceName);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                current.append(line).append(" ");
                if (trimmed.endsWith(";")) {
                    String stmt = current.toString().trim();
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current.setLength(0);
                }
            }
            if (!current.toString().trim().isEmpty()) {
                statements.add(current.toString().trim());
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to read schema file: " + e.getMessage(), e);
        }
        return statements;
    }
}
