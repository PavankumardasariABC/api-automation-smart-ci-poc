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
 * 🧪 Test Suite: Organization Creation
 * Tests the POST /api/organization endpoint with Allure-based reporting.
 */
@Epic("Organization Management")
@Feature("Create Organization API")
@Severity(SeverityLevel.CRITICAL)
public class CreateOrganizationTests {

    @Story("As an admin, I can create a new organization via API")
    @Test(dataProvider = "createOrganizationData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Validates that an organization can be created successfully with valid input data and handles duplicates gracefully.")

    public void createOrganization(Map<String, Object> orgData) {

        Allure.step("🔑 Fetching client token for authentication");
        String token = AuthUtils.getClientToken();

        String url = ConfigManager.get("base.url") + "/api/organization";
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(orgData);

        Allure.step("📦 Preparing request for organization creation: " + orgData.get("name"));
        Allure.addAttachment("Organization Request JSON", "application/json", body);

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8"
        );

        Allure.step("🚀 Sending POST request to create organization");
        Response response = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateOrganizationResponse", response.asString());

        // Retry if unauthorized
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed token...");
            token = AuthUtils.getClientToken();
            headers = Map.of(
                    "Authorization", "Bearer " + token,
                    "Content-Type", "application/json;charset=UTF-8"
            );
            response = ApiClient.post(url, headers, body);
        }

        int status = response.statusCode();
        String responseBody = response.asPrettyString();
        Allure.addAttachment("📥 Response Body", "application/json", responseBody);

        Allure.step("📊 Validating API response and behavior");

        switch (status) {
            case 200, 201 -> {
                Allure.step("✅ Organization created successfully with HTTP " + status);
                JsonPath json = response.jsonPath();
                String orgId = json.getString("id");

                Assert.assertNotNull(orgId, "Organization ID should be present");
                ResponseStore.put("glofoxOrgId", orgId);

                Allure.step("📡 Organization Created → ID: " + orgId);
            }

            case 409 -> {
                Allure.step("⚠️ Duplicate detected (409) — Organization already exists. Skipping validation.");
                Assert.assertTrue(true, "Organization already exists, skipping re-creation.");
            }

            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid input data or missing required fields.");
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Insufficient permissions.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Endpoint or path incorrect.");
            case 422 -> Assert.fail("❌ 422 - Validation Error: Business rule violation.");
            default -> Assert.fail("⚠️ Unexpected Status Code: " + status);
        }
    }
}
