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
 * 🧩 Test Suite: Secure Token API
 * Validates OAuth token retrieval using secure credentials.
 * Grant Type: client_credentials
 * Environment: Secure Auth Server (configured via secure.auth.url)
 */
@Epic("Authentication & Authorization")
@Feature("Secure OAuth Client Credentials Flow")
@Severity(SeverityLevel.BLOCKER)
public class Secure_TokenTests {

    @Story("As a secure service client, I can authenticate using secure credentials and obtain an OAuth token.")
    @Test(priority = 1, groups = {"Sanity"})
    @Description("Ensures secure OAuth token can be fetched successfully with valid Base64-encoded credentials for downstream API authentication.")

    public void getSecureAuthToken() {

        // Step 1️⃣: Fetch Auth URL
        String url = ConfigManager.get("secure.auth.url");
        Allure.step("🔐 Fetching Secure Auth Token from URL: " + url);

        // Step 2️⃣: Prepare credentials
        String username = ConfigManager.get("secure.username");
        String password = ConfigManager.get("secure.password");
        String credentials = username + ":" + password;

        Allure.step("🧾 Using username: " + username);
        String encodedCreds = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Allure.step("✅ Encoded Credentials: " + encodedCreds.substring(0, 10) + "... (masked)");

        // Optional: Verify encoded value for consistency (can disable in prod)
        String expectedEncoded = "U0VDVVJFX0FVVE9NQVRFRF9URVNUSU5HOnQzc3RAbGx0aDN0aDFuZ3M=";
        Assert.assertEquals(encodedCreds, expectedEncoded,
                "⚠️ Encoded credentials mismatch! Verify secure.username and secure.password in config.");

        // Step 3️⃣: Prepare headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + encodedCreds);
        headers.put("User-Agent", "RestAssured-Automation/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        Allure.step("🧩 Headers prepared successfully.");

        // Step 4️⃣: Prepare body
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        Allure.addAttachment("📦 Request Body", "application/json", body.toString());

        // Step 5️⃣: Execute POST call
        Allure.step("🚀 Sending POST request to Secure Auth Server...");
        Response response = ApiClient.post(url, headers, body);

        // Step 6️⃣: Retry once if transient failure
        if (response.getStatusCode() != 200) {
            Allure.step("⚠️ First attempt failed (" + response.getStatusCode() + "). Retrying...");
            response = ApiClient.post(url, headers, body);
        }

        int statusCode = response.statusCode();
        String responseBody = response.asPrettyString();

        Allure.addAttachment("📨 Secure Token Response", "application/json", responseBody);
        Allure.step("📥 HTTP Status Code: " + statusCode);
        Assert.assertEquals(statusCode, 200, "❌ Expected HTTP 200 for Secure Token API");

        // Step 7️⃣: Extract access token
        String token = extractTokenSafely(response);
        Allure.step("🔑 Extracted Token: " + maskToken(token));

        Assert.assertNotNull(token, "❌ Secure Access Token should not be null or empty!");

        // Step 8️⃣: Store for downstream APIs
        ResponseStore.put("SecureClientToken", token);
        Allure.step("💾 Stored SecureClientToken for downstream test execution.");

        // Step 9️⃣: Log expiry info if available
        if (response.jsonPath().get("expires_in") != null) {
            String expiry = response.jsonPath().getString("expires_in");
            Allure.step("⏳ Token expiry: " + expiry + " seconds");
        }

        Allure.step("✅ Secure Token fetched successfully and stored in ResponseStore.");
    }

    /** Safely extract token from different response structures */
    private String extractTokenSafely(Response res) {
        try {
            String token = res.jsonPath().getString("access_token");
            if (token == null || token.isEmpty()) {
                Map<String, Object> raw = res.as(Map.class);
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

    /** Mask token for secure logging */
    private String maskToken(String token) {
        if (token == null) return "null";
        return token.length() > 10 ? token.substring(0, 10) + "..." : "********";
    }
}
