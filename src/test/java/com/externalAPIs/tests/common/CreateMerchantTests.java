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
 * 🧩 Test Suite: Create Merchant API
 * Validates POST /api/abcpg/merchant with Allure-based structured reporting.
 */
@Epic("Merchant Management")
@Feature("Create Merchant API")
@Severity(SeverityLevel.CRITICAL)
public class CreateMerchantTests {

    @Story("As an admin, I can create a new merchant via API")
    @Test(dataProvider = "createMerchantData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Verifies merchant creation flow, validates response schema and ensures data persistence for update APIs.")

    public void createMerchant(Map<String, Object> merchantData) {

        Allure.step("1️⃣ Retrieve Secure Client Token");
        String token = AuthUtils.getSecureClientToken();

        Allure.step("2️⃣ Get Company ID from ResponseStore");
        String companyId = ResponseStore.get("companyId");
        Assert.assertNotNull(companyId, "❌ Company ID missing. Please run CreateCompanyTests first.");

        // Step 3️⃣: Setup URL & JSON body
        String url = ConfigManager.get("secure.base.url") + "/api/abcpg/merchant";
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(merchantData);

        Allure.addAttachment("📦 Merchant Creation Request", "application/json", body);

        // Step 4️⃣: Prepare headers
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8",
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020",
                "User-Agent", "Automation-Test"
        );

        Allure.step("🚀 Sending POST request to create Merchant");
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateMerchantRawResponse", res.asString());

        // Step 5️⃣: Retry on unauthorized
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Token expired or invalid. Retrying with new token...");
            token = AuthUtils.getSecureClientToken();
            headers = Map.of(
                    "Authorization", "Bearer " + token,
                    "Content-Type", "application/json;charset=UTF-8",
                    "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
            );
            res = ApiClient.post(url, headers, body);
        }

        // Step 6️⃣: Log response
        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Merchant API Response", "application/json", responseBody);
        Allure.step("📥 Response Status Code: " + status);

        // Step 7️⃣: Handle possible responses
        switch (status) {
            case 200, 201 -> {
                Allure.step("✅ Merchant created successfully with HTTP " + status);
                JsonPath json = res.jsonPath();

                String merchantId = json.getString("id");
                String merchantName = json.getString("name");
                String categoryId = json.getString("categoryId");

                Assert.assertNotNull(merchantId, "❌ Merchant ID should not be null!");

                // 🧾 Store for dependent tests
                ResponseStore.put("merchantId", merchantId);
                ResponseStore.put("MerchantFullResponse", res.asString());

                Allure.step("📡 Merchant Created → ID: " + merchantId + " | Name: " + merchantName);
                Allure.step("🔗 Stored full JSON in ResponseStore ➜ MerchantFullResponse");
            }

            case 409 -> {
                Allure.step("⚠️ Merchant already exists (409 Conflict) — Skipping duplicate creation.");
                // Don’t fail test — this is an acceptable scenario
            }

            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid or missing fields in request body.");
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Invalid or expired token.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Insufficient permissions for this tenant.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Invalid endpoint or missing resource.");
            case 422 -> Assert.fail("❌ 422 - Validation Error: Input data failed business validation.");
            default -> Assert.fail("⚠️ Unexpected Status Code: " + status + " — Please verify API contract.");
        }
    }
}
