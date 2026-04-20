package com.externalAPIs.tests.common;
import io.qameta.allure.Description;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.externalAPIs.utils.DataProviderUtils;
import com.google.gson.GsonBuilder;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * 🧩 Test Suite: Create Client API
 * Validates POST /api/client endpoint with full HTTP status handling and Allure reporting.
 */
@Epic("Client Management")
@Feature("Create Client API")
@Severity(SeverityLevel.CRITICAL)
public class CreateClientTests {

    @Story("As an admin, I can create a new client record via API.")
    @Test(dataProvider = "createClientData", dataProviderClass = DataProviderUtils.class, groups = {"Regression", "Sanity"})
    @Description("Verifies that a new client can be created successfully and all possible HTTP status codes are handled gracefully.")

    public void createClient(Map<String, Object> data) {

        Allure.step("1️⃣ Fetching Secure Token and preparing endpoint details.");
        String token = AuthUtils.getSecureClientToken();
        String url = ConfigManager.get("secure.base.url") + "/api/client";

        // Generate unique client ID
        String uniqueClientId = data.get("clientId") + "-" + System.currentTimeMillis();
        data.put("clientId", uniqueClientId);

        // Prepare headers
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8"
        );

        // Convert to JSON
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(data);
        Allure.addAttachment("📦 Client Creation Request", "application/json", body);

        Allure.step("🚀 Sending POST request to create client: " + uniqueClientId);
        Response res = ApiClient.post(url, headers, body);

        // Retry if token expired
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401) — Retrying with refreshed token...");
            token = AuthUtils.getSecureClientToken();
            res = ApiClient.post(url, headers, body);
        }

        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Client API Response", "application/json", responseBody);
        Allure.step("📥 Response Code: " + status);

        // ✅ Handle all status codes
        switch (status) {

            // ✅ Success
            case 200, 201, 204 -> {
                Allure.step("✅ Client created successfully with HTTP " + status);
                String clientId = res.jsonPath().getString("id");
                Assert.assertNotNull(clientId, "❌ clientId should be present in response.");
                ResponseStore.put("glofoxClientId", clientId);
                Allure.step("📡 Stored clientId in ResponseStore → " + clientId);
            }

            // ⚠️ Duplicate or validation conflicts
            case 409 -> {
                Allure.step("⚠️ 409 - Conflict: Client already exists with same ID or data.");
                Assert.fail("409 - Conflict: Duplicate client creation attempt.");
            }

            // 🔸 Client Errors
            case 400 -> {
                Allure.step("❌ 400 - Bad Request: Invalid or missing fields in request.");
                Assert.fail("400 - Bad Request: Invalid input or malformed request body.");
            }
            case 401 -> {
                Allure.step("❌ 401 - Unauthorized: Token expired or invalid.");
                Assert.fail("401 - Unauthorized: Invalid or expired token.");
            }
            case 403 -> {
                Allure.step("🚫 403 - Forbidden: Access denied for this user.");
                Assert.fail("403 - Forbidden: User lacks required permissions.");
            }
            case 404 -> {
                Allure.step("❌ 404 - Not Found: Endpoint or related entity missing.");
                Assert.fail("404 - Not Found: Verify API path or linked resource.");
            }
            case 422 -> {
                Allure.step("❌ 422 - Validation Error: Field-level validation failed.");
                Assert.fail("422 - Validation Error: Business rule or schema issue.");
            }

            // 🔹 Server Errors
            case 500 -> {
                Allure.step("💥 500 - Internal Server Error: Unexpected backend failure.");
                Assert.fail("500 - Internal Server Error: API crashed during processing.");
            }
            case 502 -> {
                Allure.step("💥 502 - Bad Gateway: Dependent service or gateway failure.");
                Assert.fail("502 - Bad Gateway: Downstream dependency issue.");
            }
            case 503 -> {
                Allure.step("🕒 503 - Service Unavailable: API temporarily down.");
                Assert.fail("503 - Service Unavailable: Retry after some time.");
            }
            case 504 -> {
                Allure.step("⌛ 504 - Gateway Timeout: Backend took too long to respond.");
                Assert.fail("504 - Gateway Timeout: Backend timeout occurred.");
            }

            // ⚠️ Unexpected
            default -> {
                Allure.step("⚠️ Unexpected Status: " + status);
                Assert.fail("Unexpected Status Code: " + status + " — Check backend logs or documentation.");
            }
        }

        Allure.step("📊 Test completed for Create Client API with final status: " + status);
    }
}
