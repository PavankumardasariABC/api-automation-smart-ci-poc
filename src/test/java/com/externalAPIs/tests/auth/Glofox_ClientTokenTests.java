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
 * 🧩 Test Suite: Glofox Client Token API
 * Endpoint: POST https://<env>.30preprod.com/api/token
 * Purpose: Obtain and cache OAuth access token using client credentials.
 */
@Epic("Authentication & Authorization")
@Feature("Glofox OAuth Token Flow")
@Severity(SeverityLevel.BLOCKER)
public class Glofox_ClientTokenTests {

    @Story("As a Glofox client, I can authenticate and retrieve a valid OAuth token for downstream API access.")
    @Test(priority = 1, groups = {"Sanity"})
    @Description("Ensures Glofox OAuth token retrieval using Base64-encoded client credentials and validates token caching.")

    public void getGlofoxClientToken() {

        // Step 1️⃣: Check cache
        String cachedToken = ResponseStore.get("GlofoxClientToken");
        if (cachedToken != null && !cachedToken.isEmpty()) {
            Allure.step("✅ Reusing cached Glofox token: " + maskToken(cachedToken));
            return;
        }

        // Step 2️⃣: Construct endpoint
        String url = ConfigManager.get("base.url").replace("/api", "") + "/api/token";
        Allure.step("🌍 Environment: " + ConfigManager.getEnv());
        Allure.step("🔗 Auth URL: " + url);

        // Step 3️⃣: Prepare and encode credentials
        String clientId = "GLOFOX_AUTH";
        String clientSecret = "GLOFOX_AUTH";
        String credentials = clientId + ":" + clientSecret;
        String encodedCreds = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Allure.step("🧾 Using credentials: " + maskCredentials(clientId));

        // Step 4️⃣: Build headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + encodedCreds);
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "RestAssured-Automation/1.0");

        Allure.step("🧩 Headers prepared successfully.");

        // Step 5️⃣: Build body
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        Allure.addAttachment("📦 Request Body", "application/json", body.toString());

        // Step 6️⃣: Execute request
        Allure.step("🚀 Sending POST request to retrieve Glofox token...");
        Response response = ApiClient.post(url, headers, body);

        // Retry once if transient failure
        if (response.getStatusCode() >= 500 || response.getStatusCode() == 401) {
            Allure.step("⚠️ First attempt failed (" + response.getStatusCode() + "). Retrying...");
            response = ApiClient.post(url, headers, body);
        }

        int statusCode = response.statusCode();
        String responseBody = response.asPrettyString();

        Allure.addAttachment("📨 Glofox Token Response", "application/json", responseBody);
        Allure.step("📥 HTTP Status Code: " + statusCode);

        // Step 7️⃣: Handle all response codes
        switch (statusCode) {
            case 200:
            case 201:
                String token = extractToken(response);
                Assert.assertNotNull(token, "Access token should not be null!");
                ResponseStore.put("GlofoxClientToken", token);

                Allure.step("✅ Glofox Token retrieved successfully: " + maskToken(token));

                if (response.jsonPath().get("expires_in") != null) {
                    Allure.step("⏳ Token Expiry: " + response.jsonPath().getString("expires_in") + " seconds");
                }
                break;

            case 400:
                Allure.step("❌ Bad Request (400): Invalid grant_type or malformed body.");
                Assert.fail("400 - Bad Request");
                break;

            case 401:
                Allure.step("❌ Unauthorized (401): Invalid credentials or Basic Auth header.");
                Assert.fail("401 - Unauthorized");
                break;

            case 403:
                Allure.step("🚫 Forbidden (403): Client lacks permission for token endpoint.");
                Assert.fail("403 - Forbidden");
                break;

            case 404:
                Allure.step("❌ Not Found (404): Check token endpoint URL.");
                Assert.fail("404 - Not Found");
                break;

            case 500:
                Allure.step("💥 Internal Server Error (500): OAuth service issue.");
                Assert.fail("500 - Internal Server Error");
                break;

            default:
                Allure.step("⚠️ Unexpected Status: " + statusCode + ". Review response payload.");
                Assert.fail("Unexpected Status: " + statusCode);
        }

        Allure.step("✅ Glofox Token retrieval test completed.");
    }

    /** Extracts access token safely */
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
            Allure.step("⚠️ Token extraction error: " + e.getMessage());
            return null;
        }
    }

    /** Masks credentials for secure logging */
    private String maskCredentials(String clientId) {
        return clientId + ":********";
    }

    /** Masks token for display */
    private String maskToken(String token) {
        if (token == null) return "null";
        return token.length() > 10 ? token.substring(0, 10) + "..." : "********";
    }
}
