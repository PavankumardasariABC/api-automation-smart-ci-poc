package com.ordersession.auth;

import com.ordersession.config.ApiClient;
import com.ordersession.config.ConfigManager;
import com.ordersession.store.ResponseStore;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches OAuth access tokens using the same auth contracts as the root
 * {@code api-automation-smart-ci-poc} project (client credentials on {@code auth.url},
 * {@code secure.auth.url}, or Glofox {@code base.url}/api/token).
 */
public final class ExternalApiTokenProvider {

    private static final Object LOCK = new Object();
    private static final int TOKEN_ATTEMPTS = 3;

    private ExternalApiTokenProvider() {
    }

    public static String fetchFromConfiguredSource() {
        String mode = ConfigManager.getOptional("order.session.auth.source");
        if (mode == null || mode.isBlank()) {
            mode = "GLOFOX";
        }
        return switch (mode.trim().toUpperCase()) {
            case "CLIENT" -> fetchClientToken();
            case "SECURE" -> fetchSecureToken();
            default -> fetchGlofoxToken();
        };
    }

    public static String fetchClientToken() {
        synchronized (LOCK) {
            String url = ConfigManager.get("auth.url");
            String credentials = System.getProperty("auth.creds");
            if (credentials == null || credentials.isBlank()) {
                credentials = ConfigManager.get("credentials");
            }
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return postClientCredentials(url, encoded, "CLIENT");
        }
    }

    public static String fetchSecureToken() {
        synchronized (LOCK) {
            String url = ConfigManager.get("secure.auth.url");
            String user = ConfigManager.get("secure.username");
            String pass = ConfigManager.get("secure.password");
            String credentials = user + ":" + pass;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return postClientCredentials(url, encoded, "SECURE");
        }
    }

    public static String fetchGlofoxToken() {
        synchronized (LOCK) {
            // Glofox-style client-credentials URL from merged base.url
            String url = ConfigManager.get("base.url").replace("/api", "") + "/api/token";
            String clientId = ConfigManager.getOptional("glofox.auth.client.id");
            String clientSecret = ConfigManager.getOptional("glofox.auth.client.secret");
            if (clientId == null || clientSecret == null) {
                clientId = "GLOFOX_AUTH";
                clientSecret = "GLOFOX_AUTH";
            }
            String credentials = clientId + ":" + clientSecret;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return postClientCredentials(url, encoded, "GLOFOX");
        }
    }

    private static String postClientCredentials(String url, String basicPayload, String label) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + basicPayload);
        headers.put("User-Agent", "OrderSession-Automation/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        Allure.step("OAuth client_credentials → " + label + " @ " + url);

        Response last = null;
        for (int attempt = 1; attempt <= TOKEN_ATTEMPTS; attempt++) {
            last = ApiClient.post(url, headers, body);
            int code = last.statusCode();
            if (code == 200 || code == 201) {
                break;
            }
            if (code >= 500 || code == 401 || code == 403) {
                Allure.step("Token attempt " + attempt + " HTTP " + code + " — retrying");
                sleepBackoff(attempt);
                continue;
            }
            break;
        }
        if (last == null) {
            throw new IllegalStateException("No response from token endpoint");
        }
        if (last.statusCode() != 200 && last.statusCode() != 201) {
            throw new IllegalStateException(label + " token HTTP " + last.statusCode() + ": " + last.asPrettyString());
        }
        String access = last.jsonPath().getString("access_token");
        if (access == null || access.isBlank()) {
            throw new IllegalStateException(label + " token: access_token missing");
        }
        ResponseStore.put("ExternalApiTokenSource", label);
        return access;
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
