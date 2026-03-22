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
 * 🧩 Test Suite: Authorization Token API
 * Validates OAuth token retrieval using client credentials (client_credentials grant).
 */
@Epic("Authentication & Authorization")
@Feature("OAuth Client Credentials Flow")
@Severity(SeverityLevel.BLOCKER)
public class Authorization_TokenTests {

    @Test(priority = 1, groups = {"Sanity", "Smoke"})
    @Story("As a system, I can authenticate using client credentials and retrieve a valid OAuth token.")
    @Description("Ensures valid OAuth access token retrieval from the authorization server using Base64-encoded client credentials.")

    public void getAuthToken() {

        Allure.step("1️⃣ Checking if existing token is already available in ResponseStore");
        String existingToken = ResponseStore.get("ClientToken");
        if (existingToken != null && !existingToken.isEmpty()) {
            Allure.step("✅ Reusing cached token: " + maskToken(existingToken));
            return;
        }

        // Step 2️⃣ Get Environment & URL
        String url = ConfigManager.getAuthUrl(false);
        Allure.step("🌍 Environment: " + ConfigManager.getEnv());
        Allure.step("🔗 Auth URL: " + url);

        // Step 3️⃣ Prepare credentials
        String credentials = System.getProperty("auth.creds", ConfigManager.get("credentials"));
        if (credentials == null || credentials.isBlank()) {
            credentials = "AUTOMATED_TESTING:t3st@llth3th1ngs"; // fallback for local
            Allure.step("⚠️ Using fallback credentials for local execution");
        }

        String encodedCreds = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // Step 4️⃣ Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + encodedCreds);
        headers.put("User-Agent", "RestAssured-Automation/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // Step 5️⃣ Request body
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        Allure.step("🔐 Requesting OAuth token with masked credentials: " + maskCredentials(credentials));

        // Step 6️⃣ Execute API Call
        Response response = ApiClient.post(url, headers, body);

        // Retry on transient failures
        if (response.getStatusCode() != 200) {
            Allure.step("⚠️ Initial token fetch failed (HTTP " + response.getStatusCode() + "). Retrying...");
            response = ApiClient.post(url, headers, body);
        }

        int statusCode = response.getStatusCode();
        String responseBody = response.asPrettyString();

        Allure.addAttachment("📨 Auth Token Response", "application/json", responseBody);
        Allure.step("📥 Response Code: " + statusCode);

        Assert.assertEquals(statusCode, 200, "❌ Expected 200 OK but got " + statusCode);

        // Step 7️⃣ Extract access token
        String accessToken = extractTokenSafely(response);
        Allure.step("🔑 Extracted Access Token: " + maskToken(accessToken));

        Assert.assertNotNull(accessToken, "❌ Access token was null or missing in response!");

        ResponseStore.put("ClientToken", accessToken);
        Allure.step("💾 Stored ClientToken in ResponseStore for reuse in subsequent tests.");

        // Step 8️⃣ Log expiry info
        if (response.jsonPath().get("expires_in") != null) {
            String expiry = response.jsonPath().getString("expires_in");
            Allure.step("⏳ Token Expiry: " + expiry + " seconds");
        }

        Allure.step("✅ OAuth token retrieval test completed successfully.");
    }

    /** Extracts token from response safely, handling nested formats */
    private String extractTokenSafely(Response response) {
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

    /** Masks sensitive credentials for secure logging */
    private String maskCredentials(String creds) {
        if (creds.contains(":")) {
            String[] parts = creds.split(":", 2);
            return parts[0] + ":********";
        }
        return "********";
    }

    /** Masks token partially for logs */
    private String maskToken(String token) {
        if (token == null) return "null";
        return token.length() > 10 ? token.substring(0, 10) + "..." : "********";
    }
}
