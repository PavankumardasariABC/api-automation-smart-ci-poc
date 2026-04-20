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
 * 🧩 Test Suite: Create Company API
 * Validates POST /api/abcpg/company with complete HTTP status handling.
 */
@Epic("Company Management")
@Feature("Create Company API")
@Severity(SeverityLevel.CRITICAL)
public class CreateCompanyTests {

    @Story("As an admin, I can create a company via API for payment processing configuration.")
    @Test(dataProvider = "createCompanyData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Verifies that a company can be created successfully, including validation for all HTTP responses.")

    public void createCompany(Map<String, Object> companyData) {

        Allure.step("1️⃣ Generating Secure Client Token and setting up endpoint.");
        String token = AuthUtils.getSecureClientToken();
        String url = ConfigManager.get("secure.base.url") + "/api/abcpg/company";

        // Step 2️⃣: Build JSON request
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(companyData);
        Allure.addAttachment("📦 Company Creation Request", "application/json", body);

        // Step 3️⃣: Prepare headers
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8"
        );

        Allure.step("🚀 Sending POST request to create company...");
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateCompanyResponse", res.asString());

        // Step 4️⃣: Retry if unauthorized
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401) — Retrying with fresh token...");
            token = AuthUtils.getSecureClientToken();
            res = ApiClient.post(url, headers, body);
        }

        // Step 5️⃣: Log and attach response
        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Company API Response", "application/json", responseBody);
        Allure.step("📥 Response Code: " + status);

        // Step 6️⃣: Handle all possible responses
        switch (status) {

            // ✅ Success
            case 200, 201 -> {
                Allure.step("✅ Company created successfully with HTTP " + status);
                JsonPath json = res.jsonPath();

                String companyId = json.getString("id");
                Assert.assertNotNull(companyId, "❌ Company ID is missing from response!");

                // Store for later use
                ResponseStore.put("companyId", companyId);
                Allure.step("📡 Company Created → ID: " + companyId);
            }

            // ⚠️ Duplicate
            case 409 -> {
                Allure.step("⚠️ 409 - Conflict: Company already exists.");
                Assert.fail("409 - Conflict: Duplicate company detected.");
            }

            // 🔸 Client Errors
            case 400 -> {
                Allure.step("❌ 400 - Bad Request: Invalid or missing fields.");
                Assert.fail("400 - Bad Request: Check required fields or request schema.");
            }
            case 401 -> {
                Allure.step("❌ 401 - Unauthorized: Token expired or invalid.");
                Assert.fail("401 - Unauthorized: Invalid or expired token.");
            }
            case 403 -> {
                Allure.step("🚫 403 - Forbidden: Access denied.");
                Assert.fail("403 - Forbidden: Permission issue for this user.");
            }
            case 404 -> {
                Allure.step("❌ 404 - Not Found: Endpoint missing or invalid URL.");
                Assert.fail("404 - Not Found: Check endpoint configuration.");
            }
            case 422 -> {
                Allure.step("❌ 422 - Validation Error: Field-level business rule failed.");
                Assert.fail("422 - Validation Error: Verify input validation constraints.");
            }

            // 🔹 Server Errors
            case 500 -> {
                Allure.step("💥 500 - Internal Server Error: Unexpected backend issue.");
                Assert.fail("500 - Internal Server Error: API processing failure.");
            }
            case 502 -> {
                Allure.step("💥 502 - Bad Gateway: Dependent service failure.");
                Assert.fail("502 - Bad Gateway: Integration failure downstream.");
            }
            case 503 -> {
                Allure.step("🕒 503 - Service Unavailable: Server temporarily overloaded or down.");
                Assert.fail("503 - Service Unavailable: Retry later.");
            }
            case 504 -> {
                Allure.step("⌛ 504 - Gateway Timeout: Request timed out.");
                Assert.fail("504 - Gateway Timeout: Server did not respond in time.");
            }

            // ⚠️ Unexpected
            default -> {
                Allure.step("⚠️ Unexpected HTTP Status Code: " + status);
                Assert.fail("Unexpected Status Code: " + status + " — Verify backend logs or contract.");
            }
        }

        Allure.step("📊 Test completed for Create Company API with status " + status);
    }
}
