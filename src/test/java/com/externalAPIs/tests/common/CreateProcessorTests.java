package com.externalAPIs.tests.common;

import io.qameta.allure.*;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.externalAPIs.utils.DataProviderUtils;
import com.google.gson.GsonBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧩 Test Suite: Create Processor API
 * Validates POST /api/abcpg/processor with Allure-based reporting and response chaining.
 */
@Epic("Processor Management")
@Feature("Create Processor API")
@Severity(SeverityLevel.CRITICAL)
@Link(name = "Processor API Docs", url = "https://your-api-docs-link.com")
@Issue("PROCESSOR-CREATE-001")
@TmsLink("TMS-PROCESSOR-001")
public class CreateProcessorTests {

    @Story("As an admin, I can create a new processor via API")
    @Test(
            dataProvider = "createProcessorData",
            dataProviderClass = DataProviderUtils.class,
            groups = {"Regression"},
            priority = 1
    )
    @Description("Validates that a new payment processor can be created and response data is stored for dependent APIs.")
    public void createProcessor(Map<String, Object> processorData) {

        Allure.step("1️⃣ Initialize authentication and dependencies");
        String token = AuthUtils.getSecureClientToken();

        // Get Tenant ID (company or organization)
        String tenantId = ResponseStore.get("companyId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "41424320-436f-6d70-616e-792020202020"; // fallback
            Allure.step("⚠️ Tenant ID not found in ResponseStore — using default dev ID");
        }

        // Step 2️⃣: Build request
        String url = ConfigManager.get("secure.base.url") + "/api/abcpg/processor";
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(processorData);
        Allure.addAttachment("📦 Processor Creation Request", "application/json", body);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020");
        headers.put("User-Agent", "Automation-Test");

        Allure.step("🚀 Sending POST request to create processor");
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateProcessorRawResponse", res.asString());

        // Step 3️⃣: Retry if token expired
        if (res.statusCode() == 401) {
            Allure.step("🔑 Token expired — retrying with refreshed token");
            token = AuthUtils.getSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            res = ApiClient.post(url, headers, body);
        }

        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Processor API Response", "application/json", responseBody);

        // Step 4️⃣: Status-based validation
        switch (status) {

            // ✅ Success Cases
            case 200, 201 -> {
                Allure.step("✅ Processor created successfully (HTTP " + status + ")");
                JsonPath json = res.jsonPath();

                String processorId = json.getString("id");
                String processorName = json.getString("name");
                String paymentType = json.getString("paymentType");

                Assert.assertNotNull(processorId, "❌ Processor ID should not be null");
                Assert.assertNotNull(processorName, "❌ Processor Name should not be null");
                Assert.assertNotNull(paymentType, "❌ Processor PaymentType should not be null");

                // 🧾 Store for future API chaining
                ResponseStore.put("ProcessorId", processorId);
                ResponseStore.put("ProcessorName", processorName);
                ResponseStore.put("ProcessorPaymentType", paymentType);
                ResponseStore.put("ProcessorFullResponse", res.asString());

                Allure.step("📡 Processor Created → ID: " + processorId +
                        " | Name: " + processorName +
                        " | Type: " + paymentType);
            }

            // 🔹 Conflict (Already Exists)
            case 409 -> {
                Allure.step("⚠️ Processor already exists (409 Conflict) — skipping duplicate creation.");
                // Soft skip (no failure)
            }

            // 🔸 Client/Server Errors
            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid request payload. \n" + responseBody);
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Invalid or expired token.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Tenant or permission issue.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Invalid endpoint or tenant header.");
            case 422 -> Assert.fail("❌ 422 - Validation Error: Missing or invalid fields.");
            case 500 -> Assert.fail("💥 500 - Internal Server Error: Check logs.");
            default -> Assert.fail("⚠️ Unexpected Status Code: " + status + " — please verify API behavior.");
        }

        // Step 5️⃣: Attach full response for Allure Traceability
        Allure.addAttachment("🧾 Stored Processor Data for Reuse", "application/json",
                ResponseStore.get("ProcessorFullResponse"));
    }
}
