package com.billing.auth;

import com.billing.config.ApiClient;
import com.billing.config.ConfigManager;
import com.billing.store.ResponseStore;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Commerce Authorization API: {@code POST /api/authorization/token?grant_type=client_credentials} with Basic auth.
 */
public final class BillingTokenProvider {

    private static final Object LOCK = new Object();
    private static final int TOKEN_ATTEMPTS = 3;

    private BillingTokenProvider() {
    }

    public static String fetchAccessToken() {
        synchronized (LOCK) {
            String url = ConfigManager.get("billing.auth.url");
            String user = ConfigManager.get("billing.oauth.username");
            String pass = resolvePassword();
            if (pass == null || pass.isBlank()) {
                throw new IllegalStateException(
                        "billing.oauth.password is empty — set in env/*.local.properties or BILLING_OAUTH_PASSWORD");
            }
            String basic = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Basic " + basic);
            headers.put("Accept", "application/json;charset=UTF-8");
            headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            headers.put("User-Agent", "Billing-Automation/1.0");

            Map<String, String> form = new HashMap<>();
            form.put("grant_type", "client_credentials");

            Allure.step("OAuth client_credentials (Commerce) @ " + url);

            Response last = null;
            for (int attempt = 1; attempt <= TOKEN_ATTEMPTS; attempt++) {
                last = ApiClient.post(url, headers, form);
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
                throw new IllegalStateException("Token HTTP " + last.statusCode() + ": " + last.asPrettyString());
            }
            String access = last.jsonPath().getString("access_token");
            if (access == null || access.isBlank()) {
                throw new IllegalStateException("access_token missing in token response");
            }
            ResponseStore.put("BillingTokenSource", "COMMERCE_CLIENT_CREDENTIALS");
            return access;
        }
    }

    private static String resolvePassword() {
        String env = System.getenv("BILLING_OAUTH_PASSWORD");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return ConfigManager.getOptional("billing.oauth.password");
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
