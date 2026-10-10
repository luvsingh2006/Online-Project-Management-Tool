package com.pmtool.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized, thread-safe configuration manager for the Online Project Management Tool.
 */
public final class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    private static final Properties properties = new Properties();
    private static volatile boolean initialized = false;

    private AppConfig() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (in != null) {
                properties.load(in);
                LOGGER.info("Loaded configuration properties from classpath resource: " + DEFAULT_PROPERTIES_FILE);
            } else {
                LOGGER.warning("Default " + DEFAULT_PROPERTIES_FILE + " not found in classpath. Using fallback values.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load classpath properties: " + e.getMessage(), e);
        }

        String externalPath = System.getProperty("pmtool.config.path");
        if (externalPath != null && !externalPath.isBlank()) {
            File externalFile = new File(externalPath);
            if (externalFile.exists() && externalFile.canRead()) {
                try (InputStream extIn = new FileInputStream(externalFile)) {
                    properties.load(extIn);
                    LOGGER.info("Overlaid external configuration from: " + externalPath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Unable to read external config file: " + externalPath, e);
                }
            }
        }

        initialized = true;
    }

    public static String getString(String key, String defaultValue) {
        ensureInitialized();
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid integer for property " + key + ": '" + val + "'. Using fallback: " + defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    public static long getLong(String key, long defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid long for property " + key + ": '" + val + "'. Using fallback: " + defaultValue);
            return defaultValue;
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }
}
