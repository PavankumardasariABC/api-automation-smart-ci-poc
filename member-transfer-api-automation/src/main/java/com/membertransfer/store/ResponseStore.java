package com.membertransfer.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ResponseStore {

    private static final Map<String, Object> STORE = new ConcurrentHashMap<>();
    private static final String STORE_FILE = "build/response_store.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded;

    public static synchronized void put(String key, Object value) {
        STORE.put(key, value);
        if (key.toLowerCase().contains("request")) {
            STORE.put("RequestBody", value);
        } else if (key.toLowerCase().contains("response")) {
            STORE.put("ResponseBody", value);
        }
        persist();
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> T get(String key) {
        if (!loaded) {
            load();
        }
        return (T) STORE.get(key);
    }

    public static synchronized void clear() {
        STORE.clear();
        persist();
    }

    private static void persist() {
        try {
            File file = new File(STORE_FILE);
            file.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(file)) {
                GSON.toJson(STORE, w);
            }
        } catch (IOException e) {
            System.err.println("ResponseStore persist failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        File file = new File(STORE_FILE);
        if (!file.exists()) {
            loaded = true;
            return;
        }
        try (FileReader r = new FileReader(file)) {
            Map<String, Object> map = GSON.fromJson(r, Map.class);
            if (map != null) {
                STORE.putAll(map);
            }
        } catch (IOException e) {
            System.err.println("ResponseStore load failed: " + e.getMessage());
        } finally {
            loaded = true;
        }
    }
}
