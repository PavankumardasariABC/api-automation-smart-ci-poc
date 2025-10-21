package com.externalAPIs.tests.common;
import io.qameta.allure.Description;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.externalAPIs.utils.DataProviderUtils;
import com.google.gson.GsonBuilder;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * 🧩 Test Suite: Create Client Profile API
 * Validates POST /api/abcblg/client-profile with complete status coverage and Allure-based reporting.
 */
@Epic("Client Management")
@Feature("Create Client Profile API")
@Severity(SeverityLevel.CRITICAL)
public class CreateClientProfileTests {

    @Story("As an admin, I can create a client profile linked to merchant and location under an organization.")
    @Test(dataProvider = "createClientProfileData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Ensures that a client profile can be created successfully, including dynamic merchant, location, and organization references.")

    public void createClientProfile(Map<String, Object> clientData) {

        Allure.step("1️⃣ Fetching required dependent values (merchantId, locationId, orgId)");
        String token = AuthUtils.getClientToken();
        String merchantId = ResponseStore.get("merchantId");
        String locationId = ResponseStore.get("locationId");
        String orgId = ResponseStore.get("glofoxOrgId");

        Assert.assertNotNull(merchantId, "❌ merchantId is null — run CreateMerchantTests first!");
        Assert.assertNotNull(locationId, "❌ locationId is null — run CreateLocationTests first!");
        Assert.assertNotNull(orgId, "❌ glofoxOrgId is null — run CreateOrganizationTests first!");

        // Step 2️⃣: Add dependent fields dynamically
        clientData.put("merchantId", merchantId);
        clientData.put("locationId", locationId);

        // Step 3️⃣: Convert to JSON body
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(clientData);
        Allure.addAttachment("📦 Client Profile Request", "application/json", body);

        // Step 4️⃣: Prepare headers
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8",
                "ABCFS-TENANT-ID", orgId,
                "User-Agent", "Automation-Test"
        );

        String url = ConfigManager.get("base.url") + "/api/abcblg/client-profile";
        Allure.step("🚀 Sending POST request to create Client Profile at: " + url);

        // Step 5️⃣: Execute POST request
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateClientProfileResponse", res.asString());

        // Step 6️⃣: Retry on unauthorized
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed token...");
            token = AuthUtils.getClientToken();
            res = ApiClient.post(url, headers, body);
        }

        // Step 7️⃣: Capture response
        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Client Profile API Response", "application/json", responseBody);
        Allure.step("📥 Response Code: " + status);

        // Step 8️⃣: Handle all status codes
        switch (status) {

            // ✅ Success responses
            case 200, 201, 204 -> {
                Allure.step("✅ Client Profile created successfully (HTTP " + status + ")");
                JsonPath json = res.jsonPath();

                String clientProfileId = json.getString("clientProfileId");
                String returnedMerchantId = json.getString("merchantId");
                String returnedLocationId = json.getString("locationId");

                Assert.assertNotNull(clientProfileId, "❌ clientProfileId is missing in response");
                Assert.assertNotNull(returnedMerchantId, "❌ merchantId is missing in response");
                Assert.assertNotNull(returnedLocationId, "❌ locationId is missing in response");

                // Save for downstream tests
                ResponseStore.put("clientProfileId", clientProfileId);
                ResponseStore.put("merchantId", returnedMerchantId);
                ResponseStore.put("locationId", returnedLocationId);

                Allure.step("📡 Stored IDs → clientProfileId: " + clientProfileId + ", merchantId: "
                        + returnedMerchantId + ", locationId: " + returnedLocationId);
            }

            // ⚠️ Business or validation errors
            case 400 -> {
                Allure.step("❌ 400 - Bad Request: Invalid or missing fields in request body.");
                Assert.fail("400 - Bad Request: Check mandatory fields or data types.");
            }
            case 401 -> {
                Allure.step("❌ 401 - Unauthorized: Token expired or invalid.");
                Assert.fail("401 - Unauthorized: Authentication issue.");
            }
            case 403 -> {
                Allure.step("🚫 403 - Forbidden: Access denied for tenant or role.");
                Assert.fail("403 - Forbidden: User not authorized for this operation.");
            }
            case 404 -> {
                Allure.step("❌ 404 - Not Found: Merchant or Location ID not found.");
                Assert.fail("404 - Not Found: Invalid or stale references.");
            }
            case 409 -> {
                Allure.step("⚠️ 409 - Conflict: Client Profile already exists.");
                Assert.fail("409 - Conflict: Duplicate client profile detected.");
            }
            case 422 -> {
                Allure.step("❌ 422 - Validation Error: Field constraints or rules violated.");
                Assert.fail("422 - Validation Error: Check business logic validation.");
            }

            // 🔹 Server-side failures
            case 500 -> {
                Allure.step("💥 500 - Internal Server Error: Unexpected backend failure.");
                Assert.fail("500 - Internal Server Error: API crashed during execution.");
            }
            case 502 -> {
                Allure.step("💥 502 - Bad Gateway: Dependent service failure.");
                Assert.fail("502 - Bad Gateway: Upstream dependency issue.");
            }
            case 503 -> {
                Allure.step("🕒 503 - Service Unavailable: API temporarily unavailable.");
                Assert.fail("503 - Service Unavailable: Try again later.");
            }
            case 504 -> {
                Allure.step("⌛ 504 - Gateway Timeout: Backend timeout or delay.");
                Assert.fail("504 - Gateway Timeout: Slow or unresponsive backend.");
            }

            // ⚠️ Unexpected status
            default -> {
                Allure.step("⚠️ Unexpected HTTP Status: " + status);
                Assert.fail("Unexpected Status Code: " + status + " — verify backend logs.");
            }
        }

        Allure.step("📊 Final Validation Completed for Create Client Profile API with status: " + status);
    }
}
