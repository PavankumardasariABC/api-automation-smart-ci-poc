package com.externalAPIs.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ Thread-safe global ResponseStore with optional disk persistence.
 *
 * - Shared between test classes
 * - Automatically cleared at suite start (via AllureTestListener)
 * - Persists to disk after each put()
 * - Reloads automatically on first get()
 * - Automatically tracks latest API Request/Response for Allure attachments
 */
public class ResponseStore {

    private static final Map<String, Object> store = new ConcurrentHashMap<>();
    private static final String STORE_FILE = "build/response_store.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean isLoaded = false;

    /** Stores a key-value pair and saves it to disk */
    public static synchronized void put(String key, Object value) {
        store.put(key, value);

        // 🟦 Automatically track last request/response for Allure attachment
        if (key.toLowerCase().contains("request"))
            store.put("RequestBody", value);
        else if (key.toLowerCase().contains("response"))
            store.put("ResponseBody", value);

        persist();
    }

    /** Retrieves a value by key, auto-loading persisted data if needed */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T get(String key) {
        if (!isLoaded) load(); // lazy-load on first use
        return (T) store.get(key);
    }

    /** Clears all data from memory and persistence */
    public static synchronized void clear() {
        store.clear();
        persist();
        System.out.println("🧹 ResponseStore memory and disk cleared.");
    }

    /** Writes current data to disk */
    private static synchronized void persist() {
        try {
            File file = new File(STORE_FILE);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(store, writer);
            }
        } catch (IOException e) {
            System.err.println("⚠️ Failed to persist ResponseStore: " + e.getMessage());
        }
    }

    /** Loads data from disk (if available) */
    private static synchronized void load() {
        File file = new File(STORE_FILE);
        if (!file.exists()) {
            isLoaded = true;
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            Map<String, Object> loaded = gson.fromJson(reader, Map.class);
            if (loaded != null) store.putAll(loaded);
            System.out.println("📂 ResponseStore loaded with " + store.size() + " entries.");
        } catch (IOException e) {
            System.err.println("⚠️ Failed to load ResponseStore: " + e.getMessage());
        } finally {
            isLoaded = true;
        }
    }
}
