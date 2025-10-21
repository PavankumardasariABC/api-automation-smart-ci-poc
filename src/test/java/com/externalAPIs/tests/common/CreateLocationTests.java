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
 * 🧩 Test Suite: Create Location API
 * Validates POST /api/location functionality with complete HTTP status coverage.
 */
@Epic("Organization Management")
@Feature("Create Location API")
@Severity(SeverityLevel.CRITICAL)
public class CreateLocationTests {

    @Story("As an admin, I can create a new location under an existing organization via API")
    @Test(dataProvider = "createLocationData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Verifies that a new location can be created successfully, with full error handling for all HTTP status codes.")

    public void createLocation(Map<String, Object> locationData) {

        Allure.step("1️⃣ Fetching required dependencies (Client Token & Organization ID)");
        String token = AuthUtils.getClientToken();
        String organizationId = ResponseStore.get("glofoxOrgId");

        Assert.assertNotNull(organizationId, "❌ Missing Organization ID. Run CreateOrganizationTests first.");

        // Step 2️⃣: Construct URL
        String url = ConfigManager.get("base.url") + "/api/location";

        // Step 3️⃣: Add unique name and email
        String uniqueName = locationData.get("name") + "-" + System.currentTimeMillis();
        locationData.put("name", uniqueName);
        locationData.put("email", uniqueName + "@qa4life.com");

        String body = new GsonBuilder().setPrettyPrinting().create().toJson(locationData);
        Allure.addAttachment("📦 Location Creation Request", "application/json", body);

        // Step 4️⃣: Prepare headers
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8",
                "ABCFS-TENANT-ID", organizationId
        );

        Allure.step("🚀 Sending POST request to create Location");
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateLocationResponse", res.asString());

        // Step 5️⃣: Retry on unauthorized
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed token...");
            token = AuthUtils.getClientToken();
            res = ApiClient.post(url, headers, body);
        }

        // Step 6️⃣: Attach response details
        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Location API Response", "application/json", responseBody);
        Allure.step("📥 Response Code: " + status);

        // Step 7️⃣: Handle all HTTP codes
        switch (status) {

            // ✅ Success responses
            case 200, 201, 204 -> {
                Allure.step("✅ Location created successfully with HTTP " + status);
                JsonPath json = res.jsonPath();
                String locationId = json.getString("id");

                Assert.assertNotNull(locationId, "❌ Location ID should not be null");
                ResponseStore.put("locationId", locationId);
                Allure.step("📡 Location Created → ID: " + locationId);
            }

            // 🔸 Client-side errors
            case 400 -> {
                Allure.step("❌ 400 - Bad Request: Invalid or missing fields in request body.");
                Assert.fail("400 - Bad Request: Verify required fields or data format.");
            }

            case 401 -> {
                Allure.step("❌ 401 - Unauthorized: Invalid or expired token.");
                Assert.fail("401 - Unauthorized: Token expired or invalid.");
            }

            case 403 -> {
                Allure.step("🚫 403 - Forbidden: Access denied for this user or tenant.");
                Assert.fail("403 - Forbidden: Insufficient permissions.");
            }

            case 404 -> {
                Allure.step("❌ 404 - Not Found: Endpoint or linked resource missing.");
                Assert.fail("404 - Not Found: Invalid path or organization reference.");
            }

            case 409 -> {
                Allure.step("⚠️ 409 - Conflict: Location already exists with same name/email.");
                Assert.fail("409 - Conflict: Duplicate location creation attempt.");
            }

            case 422 -> {
                Allure.step("❌ 422 - Validation Error: One or more fields failed validation.");
                Assert.fail("422 - Validation Error: Business rule violation.");
            }

            // 🔹 Server-side errors
            case 500 -> {
                Allure.step("💥 500 - Internal Server Error: Unexpected backend issue.");
                Assert.fail("500 - Internal Server Error: API failed during processing.");
            }

            case 502 -> {
                Allure.step("💥 502 - Bad Gateway: Dependent service failed.");
                Assert.fail("502 - Bad Gateway: Downstream dependency unavailable.");
            }

            case 503 -> {
                Allure.step("🕒 503 - Service Unavailable: API temporarily unavailable.");
                Assert.fail("503 - Service Unavailable: Retry after some time.");
            }

            case 504 -> {
                Allure.step("⌛ 504 - Gateway Timeout: Request timed out waiting for backend.");
                Assert.fail("504 - Gateway Timeout: Backend did not respond in time.");
            }

            // ⚠️ Unexpected or undocumented status
            default -> {
                Allure.step("⚠️ Unexpected HTTP Status: " + status);
                Assert.fail("Unexpected Status Code: " + status + " — Check API logs or documentation.");
            }
        }

        Allure.step("📊 Final Validation Completed for Create Location API");
    }
}
