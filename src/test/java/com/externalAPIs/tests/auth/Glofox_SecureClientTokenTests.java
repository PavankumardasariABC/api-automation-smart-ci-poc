package com.externalAPIs.tests.auth;
import io.qameta.allure.Description;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 🧩 Test Suite: Glofox Secure Client Token API
 * Endpoint: POST /api/token
 * Purpose: Fetches a Secure Client Token for Glofox Payment APIs.
 */
@Epic("Authentication & Authorization")
@Feature("Glofox Secure OAuth Token Flow")
@Severity(SeverityLevel.BLOCKER)
public class Glofox_SecureClientTokenTests {

    @Story("As a Glofox secure service, I can authenticate with secure credentials and obtain a valid OAuth token.")
    @Test(priority = 1, groups = {"Sanity", "Smoke"})
    @Description("Ensures Glofox Secure Client Token can be retrieved using Base64-encoded credentials for secure payment APIs.")

    public void getGlofoxSecureClientToken() {

        // Step 1️⃣: Check if token is already cached
        String cachedToken = ResponseStore.get("glofoxSecureClientToken");
        if (cachedToken != null && !cachedToken.isEmpty()) {
            Allure.step("✅ Reusing cached Glofox Secure Client Token: " + maskToken(cachedToken));
            return;
        }

        // Step 2️⃣: Prepare endpoint
        String url = ConfigManager.get("secure.base.url") + "/api/token";
        Allure.step("🔗 Endpoint: " + url);

        // Step 3️⃣: Prepare credentials
        String secureCreds = System.getProperty(
                "secure.auth.creds",
                ConfigManager.get("secure.username") + ":" + ConfigManager.get("secure.password")
        );
        Assert.assertNotNull(secureCreds, "❌ Missing secure credentials. Set -Dsecure.auth.creds or secure.username/secure.password.");
        Assert.assertFalse(secureCreds.isBlank(), "❌ Secure credentials are blank. Set -Dsecure.auth.creds or secure.username/secure.password.");
        Assert.assertFalse(isPlaceholderCredentials(secureCreds), "❌ Placeholder secure credentials detected. Provide real credentials via secrets/env.");

        String encodedCreds = Base64.getEncoder()
                .encodeToString(secureCreds.getBytes(StandardCharsets.UTF_8));

        Allure.step("🔐 Using credentials: " + maskCredentials(secureCreds));
        Allure.step("🧾 Encoded Credentials: " + encodedCreds.substring(0, 10) + "... (masked)");

        // Step 4️⃣: Prepare headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + encodedCreds);
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "RestAssured-Automation");

        Allure.step("🧩 Headers prepared successfully.");

        // Step 5️⃣: Prepare body
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        Allure.addAttachment("📦 Request Body", "application/json", body.toString());

        // Step 6️⃣: Execute POST request
        Allure.step("🚀 Sending POST request to fetch Glofox Secure Token...");
        Response response = ApiClient.post(url, headers, body);

        // Retry once if unauthorized
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401) — Retrying once with same credentials...");
            response = ApiClient.post(url, headers, body);
        }

        // Step 7️⃣: Log and attach response
        int statusCode = response.statusCode();
        String responseBody = response.asPrettyString();

        Allure.addAttachment("📨 Glofox Secure Token Response", "application/json", responseBody);
        Allure.step("📥 HTTP Status Code: " + statusCode);

        // Step 8️⃣: Handle responses
        switch (statusCode) {
            case 200:
            case 201:
                String token = extractToken(response);
                Assert.assertNotNull(token, "❌ Token should not be null.");

                ResponseStore.put("glofoxSecureClientToken", token);
                Allure.step("✅ Glofox Secure Client Token fetched successfully.");
                Allure.step("🔑 Access Token (masked): " + maskToken(token));

                if (response.jsonPath().get("expires_in") != null) {
                    Allure.step("⏳ Token expires in: " + response.jsonPath().getString("expires_in") + " seconds");
                }
                break;

            case 400:
                Allure.step("❌ Bad Request (400): Invalid parameters or grant_type.");
                Assert.fail("400 - Bad Request");
                break;

            case 401:
                Allure.step("❌ Unauthorized (401): Invalid username/password.");
                Assert.fail("401 - Unauthorized");
                break;

            case 403:
                Allure.step("🚫 Forbidden (403): Client not permitted to access token endpoint.");
                Assert.fail("403 - Forbidden");
                break;

            case 404:
                Allure.step("❌ Not Found (404): Verify endpoint or environment URL.");
                Assert.fail("404 - Not Found");
                break;

            case 500:
                Allure.step("💥 Internal Server Error (500): OAuth service issue.");
                Assert.fail("500 - Internal Server Error");
                break;

            default:
                Allure.step("⚠️ Unexpected Status: " + statusCode + " — " + responseBody);
                Assert.fail("Unexpected Status Code: " + statusCode);
        }

        Allure.step("✅ Test Execution Completed for Glofox Secure Token API.");
    }

    /** Extracts access_token safely from the response */
    private String extractToken(Response response) {
        try {
            String token = response.jsonPath().getString("access_token");
            if (token == null || token.isEmpty()) {
                Map<String, Object> raw = response.as(Map.class);
                if (raw.containsKey("access_token")) {
                    token = raw.get("access_token").toString();
                }
            }
            return token;
        } catch (Exception e) {
            Allure.step("⚠️ Token extraction failed: " + e.getMessage());
            return null;
        }
    }

    /** Masks credentials for secure logging */
    private String maskCredentials(String credentials) {
        if (credentials == null) {
            return "********";
        }
        if (credentials.contains(":")) {
            String[] parts = credentials.split(":", 2);
            return parts[0] + ":********";
        }
        return "********";
    }

    private boolean isPlaceholderCredentials(String credentials) {
        String trimmed = credentials == null ? "" : credentials.trim();
        return trimmed.isEmpty()
                || trimmed.startsWith("REPLACE_")
                || trimmed.contains("YOUR_")
                || "PASTE_CREDENTIALS_HERE".equalsIgnoreCase(trimmed);
    }

    /** Masks token for report logs */
    private String maskToken(String token) {
        if (token == null) return "null";
        return token.length() > 10 ? token.substring(0, 10) + "..." : "********";
    }
}
