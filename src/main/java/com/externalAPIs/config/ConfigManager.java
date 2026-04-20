package com.externalAPIs.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final Properties props = new Properties();
    private static final String ENV;

    static {
       // ENV = System.getProperty("env", "dev"); // default to dev
        ENV = System.getProperty("env", "dev");
        loadProperties();
    }

    private static void loadProperties() {
        try (FileInputStream fis = new FileInputStream("src/test/resources/env/" + ENV + ".properties")) {
            props.load(fis);
            System.out.println("✅ Loaded environment: " + ENV);
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to load env properties for environment [" + ENV + "]: " + e.getMessage());
        }
    }

    public static String get(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing property: " + key + " in " + ENV + ".properties");
        }
        return value;
    }

    public static String getEnv() {
        return ENV;
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static String getSecureBaseUrl() {
        return get("secure.base.url");
    }

    public static String getAuthUrl(boolean secure) {
        return secure ? get("secure.auth.url") : get("auth.url");
    }

    public static String getCredentials(boolean secure) {
        return secure ? get("client.credentials") : get("credentials");
    }
}
