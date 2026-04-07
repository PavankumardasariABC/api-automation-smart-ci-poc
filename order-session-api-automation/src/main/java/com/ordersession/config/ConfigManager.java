package com.ordersession.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration layers (first non-blank wins):
 * <ol>
 *   <li>{@code src/test/resources/env/{env}.local.properties} — optional; gitignored; org IDs, test data, secrets</li>
 *   <li>{@code src/test/resources/env/{env}.properties} — committed defaults per environment (qa, dev, stage, beta)</li>
 *   <li>External merge file — {@code external.api.env.path} (OAuth: {@code base.url}, {@code auth.url}, {@code credentials}, …)</li>
 * </ol>
 * {@code -Denv} defaults to {@code qa}. Override path: {@code -Dexternal.api.env.path=...}
 */
public final class ConfigManager {

    private static final Properties LOCAL = new Properties();
    private static final Properties MODULE = new Properties();
    private static final Properties EXTERNAL = new Properties();
    private static final String ENV;

    static {
        ENV = System.getProperty("env", "qa");
        loadModule();
        loadExternalMerged();
        loadLocalOverride();
    }

    private static void loadModule() {
        try (FileInputStream fis = new FileInputStream("src/test/resources/env/" + ENV + ".properties")) {
            MODULE.load(fis);
            System.out.println("✅ Loaded Order Session env: " + ENV);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load env [" + ENV + "]: " + e.getMessage(), e);
        }
    }

    private static void loadExternalMerged() {
        String path = firstNonBlank(System.getProperty("external.api.env.path"));
        if (path == null) {
            path = firstNonBlank(MODULE.getProperty("external.api.env.path"));
        }
        if (path == null) {
            return;
        }
        File f = new File(path);
        if (!f.isFile()) {
            System.err.println("⚠️ external.api.env.path not found (auth merge skipped): " + f.getAbsolutePath());
            return;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            EXTERNAL.load(fis);
            System.out.println("✅ Merged OAuth env file: " + f.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load external env [" + path + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Optional per-developer / CI secrets file — not committed (see {@code .gitignore}).
     */
    private static void loadLocalOverride() {
        File f = new File("src/test/resources/env/" + ENV + ".local.properties");
        if (!f.isFile()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            LOCAL.load(fis);
            System.out.println("✅ Local overrides loaded: " + f.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load local overrides [" + f.getAbsolutePath() + "]: " + e.getMessage(), e);
        }
    }

    private static String firstNonBlank(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static String get(String key) {
        String value = firstNonBlank(LOCAL.getProperty(key));
        if (value == null) {
            value = firstNonBlank(MODULE.getProperty(key));
        }
        if (value == null) {
            value = firstNonBlank(EXTERNAL.getProperty(key));
        }
        if (value == null) {
            throw new RuntimeException("Missing property: " + key + " (add to " + ENV + ".local.properties, "
                    + ENV + ".properties, or external merge)");
        }
        return value;
    }

    public static String getOptional(String key) {
        String value = firstNonBlank(LOCAL.getProperty(key));
        if (value == null) {
            value = firstNonBlank(MODULE.getProperty(key));
        }
        if (value == null) {
            value = firstNonBlank(EXTERNAL.getProperty(key));
        }
        return value;
    }

    public static boolean hasExternalMerge() {
        return !EXTERNAL.isEmpty();
    }

    public static boolean hasLocalOverride() {
        return !LOCAL.isEmpty();
    }

    public static String getEnv() {
        return ENV;
    }
}
